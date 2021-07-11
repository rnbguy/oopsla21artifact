cargo build --release

cd oltpbench
ant bootstrap
ant resolve
ant build

cd ..

benches=(tpcc smallbank voter wikipedia)
# benches=(tpcc)

for bench in ${benches[@]}; do
    cargo build --example ${bench}_cr --release
    bash oltp_disql.sh ${bench} 3 10 "causal" 
done