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

build