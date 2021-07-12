Jepsen
======

This directory contains the code for TPCC which is executed via Jepsen on MariaDB replicas.

Logs will be generated in `logs` directory.

To reproduce the results from the plots in Figure 16 in the paper, execute `run.sh` script.

`bash run.sh <number of runs> <number of nodes> <timelimit>`

To check the assertions for `tpcc` on `3` replicas with a runtime limit of `10` secs for `20` times, you may run following command

`bash run.sh 20 3 10`

You can pass comma separated multiple values for number of nodes to run for multiple parameter values together. Example,

`bash run.sh 20 3,5 10`

will run both on `3` and `5` replica setup for `10` seconds timelimit.
