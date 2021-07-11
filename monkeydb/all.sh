cd /oopsla21/monkeydb
cargo build --release

cd /oopsla21/oltpbench

# benches=(tpcc smallbank voter wikipedia)
benches=(tpcc)

for bench in ${benches[@]}; do
    cargo build --example ${bench}_cr --release
    bash oltp_disql.sh ${bench} 3 10 "45,43,4,4,4" "causal" 
done