MonekyDB
========

This directory contains the code for MonkeyDB.

Logs will be generated in `log` directory.

To reproduce the results from the plots in Figure 14 and 15 in the paper, execute `run.sh` script.

`bash run.sh <number of runs> <benchmark> <consistency> <number of nodes> <timelimit>`

To check the assertions for `tpcc` under `causal` consistency on `3` replicas with a runtime limit of `10` secs for `20` times, you may run following command

`bash run.sh 20 tpcc causal 3 10`


Possible values:
- benchmark: `tpcc`, `smallbank`, `voter`, `wikipedia`
- consistency: `causal`, `readcommitted`

You can pass comma separated multiple values to run for multiple parameter values together. Example,

`bash run.sh 20 tpcc,smallbank causal,readcommitted 3,5 10`

will run both `tpcc` and `smallbank` under `causal` and `readcommitted` consistency on `3` and `5` replica setup.
