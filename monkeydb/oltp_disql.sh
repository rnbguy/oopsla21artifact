set -m

RUN_DIR=`readlink -f ../monkeydb/runs`

mkdir -p ${RUN_DIR}

OLTP_DIR=`readlink -f oltpbench`
MONKEYDB_DIR=`readlink -f ../monkeydb`

PORT=3306
NTERM=7
DUR=5
CONSISTENCY="causal"
EXEC_ST="random"
ASSERT_ST="random"

[ -z $1 ] || BENCHNAME=$1
[ -z $2 ] || NTERM=$2
[ -z $3 ] || DUR=$3
[ -z $4 ] || CONSISTENCY=$4
[ -z "$5" ] || EXEC_ST="$5"
[ -z "$6" ] || ASSERT_ST="$6"

CURR_RUN_DIR=`mktemp -d $RUN_DIR/run_${BENCHNAME}_${NTERM}_${DUR}_${CONSISTENCY}_${EXEC_ST}_${ASSERT_ST}_XXXXXX`

MONKEYDB_LOG=${CURR_RUN_DIR}/monkeydb_log
OLTP_LOAD=${CURR_RUN_DIR}/oltp_load_log
OLTP_EXEC=${CURR_RUN_DIR}/oltp_exec_log
MONKEYDB_PID_FILE=${CURR_RUN_DIR}/monkeydb_pid
OLTP_OUTD=oltp_out

function oltp_run() {
	cd ${CURR_RUN_DIR}
	> $MONKEYDB_LOG
	> $OLTP_LOAD
	> $OLTP_EXEC
	PORT=`tail -f "$MONKEYDB_LOG" | head -n1 | cut -d':' -f2` &
	${MONKEYDB_DIR}/target/release/monkeydb -a 0 >> "$MONKEYDB_LOG" 2>&1 &
	MONKEYDB_PID=$!
	echo "$MONKEYDB_PID" > "$MONKEYDB_PID_FILE"
	fg 1 > /dev/null
	PORT=`tail -f "$MONKEYDB_LOG" | head -n1 | cut -d':' -f2`
	echo curr_dir $CURR_RUN_DIR
	echo port $PORT
	mysql -h 127.0.0.1 -P $PORT -e 'reset';
	oltp_config="${CURR_RUN_DIR}/${BENCHNAME}_config_monkeydb_${PORT}.xml"
	sed "s|<DBUrl>jdbc:mysql://localhost:[0-9]\+/${BENCHNAME}</DBUrl>|<DBUrl>jdbc:mysql://localhost:${PORT}/${BENCHNAME}</DBUrl>|g" "${OLTP_DIR}/config/sample_${BENCHNAME}_config.xml" > "$oltp_config"
    sed -i "s|<isolation>.*</isolation>|<isolation>${CONSISTENCY}</isolation>|g" "${oltp_config}"
    # sed -i "s|<isolation>.*</isolation>|<isolation>TRANSACTION_READ_COMMITTED</isolation>|g" "${oltp_config}"
    sed -i "s|<username>.*</username>|<username>root</username>|g" "${oltp_config}"
    sed -i "s|<password>.*</password>|<password></password>|g" "${oltp_config}"
	sed -i "s|<terminals>[0-9]\+</terminals>|<terminals>${NTERM}</terminals>|g" "${oltp_config}"
	sed -i "s|<time>[0-9]\+</time>|<time>${DUR}</time>|g" "${oltp_config}"
	cd $OLTP_DIR
	${OLTP_DIR}/oltpbenchmark -b "${BENCHNAME}" -c "${oltp_config}" --create=true --load=true -o "$OLTP_OUTD" > "${OLTP_LOAD}" 2> "${OLTP_LOAD}_err"
	cd - > /dev/null
	if [ -s "${OLTP_LOAD}_err" ]; then
		echo "found error in LOAD - ${OLTP_LOAD}_err"
	else
		# mysql -h 127.0.0.1 -P $PORT -e "UPDATE useracct SET user_editcount = 0; COMMIT;";
		mysql -h 127.0.0.1 -P $PORT -e "loading reset";
		mysql -h 127.0.0.1 -P $PORT -e "set consistency ${CONSISTENCY}";
		mysql -h 127.0.0.1 -P $PORT -e "read ${EXEC_ST}";
		cd $OLTP_DIR
		${OLTP_DIR}/oltpbenchmark -b "${BENCHNAME}" -c "${oltp_config}" --execute=true -o "$OLTP_OUTD" >> "${OLTP_EXEC}" 2> "${OLTP_EXEC}_err"
		cd - > /dev/null
		mysql -h 127.0.0.1 -P $PORT -e 'print summary';
		if [ -s "${OLTP_EXEC}_err" ]; then
			echo "found error in EXEC - ${OLTP_EXEC}_err"
		else
			${MONKEYDB_DIR}/target/release/examples/${BENCHNAME}_cr -p "$PORT" -s "${ASSERT_ST}" -d "${BENCHNAME}"
		fi
	fi
	echo $MONKEYDB_PID
	kill -SIGINT "$MONKEYDB_PID"
	fg 2 > /dev/null
	cd - > /dev/null
}

oltp_run
