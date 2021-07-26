function build() {
    cargo build --release
    for bench in tpcc smallbank voter wikipedia; do
        cargo build --example ${bench}_cr --release
    done

    cd oltpbench
    ant bootstrap
    ant resolve
    ant build
    cd ..
}


function dep_check_one {
    [ -z $1 ] || which $1 > /dev/null 2>&1 || echo "  $1"
}

function dep_check_all {
    for e in ant cargo echo; do
        dep_check_one $e
    done
}

check=`dep_check_all`
[ ! -z "$check" ] && echo -e "not found:\n$check" && exit 1

build