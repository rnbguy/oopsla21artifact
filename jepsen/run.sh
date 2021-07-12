set -m

TOP_LOG_DIR="log"

mkdir -p "${TOP_LOG_DIR}"

function run_bench {
	timelimit=$1
	nodes=$2

    nodestr=`for e in $(seq 1 ${nodes}); do echo n$(( (e % 5) + 1 )); done | paste -sd',' -`

    CURR_DIR=`mktemp -d "${LOG_DIR}/XXXXX"`

    docker container prune -f > /dev/null 2>&1
    docker volume prune -f > /dev/null 2>&1
    docker network prune -f > /dev/null 2>&1

    > "${CURR_DIR}/up.out"
    ./docker/bin/up > "${CURR_DIR}/up.out" 2>&1 &
    sleep 2
    PID="$!"
    while true; do
        count=`grep "Debian GNU/Linux 10 n[0-9]\+ console" "${CURR_DIR}/up.out" | wc -l`
        if [ "$count" -eq "5" ]; then
            break
        fi
        sleep 1
    done

    ## docker instances are live now

    ## jepsen test
    # echo "running: . /root/.bashrc; cd /jepsen/tpcc; lein run test --time-limit ${timelimit} --nodes ${nodestr}" > "${CURR_DIR}/jepsen.out"
    docker exec -it jepsen-control bash -c ". /root/.bashrc; cd /jepsen/tpcc; lein run test --time-limit ${timelimit} --nodes ${nodestr}" > "${CURR_DIR}/jepsen.out"

    cat "${CURR_DIR}/jepsen.out" | grep "assert violations: " | dos2unix | head -n 1 | cut -d':' -f4 | tr -d ' ' | tr ',' '\n' | sort -n | uniq | paste -sd',' - >> "${CURR_VIO_LOG}"

    docker kill $(docker ps -q) > /dev/null 2>&1
    fg > /dev/null 2>&1
}

# DEFAULT PARAMETERS
total_run=5
nodes="2,3"
timelimit="10"

[ -z $1 ] || total_run=$1
[ -z $2 ] || nodes=$2
[ -z $3 ] || timelimit=$3

for node in ${nodes//,/ }; do
    LOG_DIR=`mktemp -d ${TOP_LOG_DIR}/XXXX`
    CURR_VIO_LOG="${LOG_DIR}/jepsen_vio.log"
    for i in `seq 1 "${total_run}"`; do
		start=`date +%s`
        run_bench "$timelimit" "$node"
		end=`date +%s`
		dur=$(($dur + $end - $start))
    done
	echo "=========="
	echo "TPCC on Jepsen + MariaDB cluster"
	echo "----------"
	echo "On ${nodes} nodes with time limit of ${timelimit} secs"
	echo "Average duration per run: $(( $dur / $total_run )) secs"
	echo "----------"
    cat "${CURR_VIO_LOG}" | tr ',' '\n' | sort -n | sed -e 's/^/A/' | uniq -c | column -t -N "#Violation among ${total_run} runs,Assertion" -O '2,1' -o ' | '
	echo "----------"
done
