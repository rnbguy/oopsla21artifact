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
    if which -v > /dev/null 2>&1; then
        for e in awk cat column cut date grep head mktemp mysql printf sort uniq; do
            dep_check_one $e
        done
    else
        echo " which (for dependency check)"
    fi
}

check=`dep_check_all`
[ ! -z "$check" ] && echo -e "not found:\n$check" && exit 1

build