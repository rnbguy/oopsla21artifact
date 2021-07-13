Jepsen
======

# Description
Jepsen is a distributed system testing framework.
Developers can write tests for their applications or database using Jepsen.

We implement TPCC benchmark using Jepsen and executed it on MariaDB database.

Then we check for the A1-A12 assertions (subsection 8.1) for TPCC and present the number of violation.

# Results from paper
We present the statistics of our findings as 2D plots in figure 16 in the paper. This artifact reproduces the same.

# Dependencies
- docker-compose and docker
- Bash and its utilities

If you are using apt, `sudo apt install docker-compose; sudo gpasswd -a <username> docker` and reboot.

If you are using pacman, `sudo pacman -S docker-compose; sudo gpasswd -a <username> docker; sudo systemctl enable docker` and rebbot.
If you are using something different than `systemctl`, change the command accordingly.

# Instruction

## Execute

_Logs will be generated in `log` directory._

To reproduce the results from the plots in Figure 16 in the paper, execute `run.sh` script.

`bash run.sh <number of runs> <number of nodes> <timelimit>`

To check the assertions for `tpcc` on `3` replicas with a runtime limit of `10` secs for `20` times, you may run following command

`bash run.sh 20 3 10`

You can pass comma separated multiple values for number of nodes to run for multiple parameter values together. Example,

`bash run.sh 20 3,5 10`

will run both on `3` and `5` replica setup for `10` seconds timelimit.

## Output

A sample output would look as following,

```
$ bash run.sh 15 wikipedia causal 3 10
==========
Benchmark: wikipedia
----------
15 runs with time limit of 10 secs
On 3 nodes with "causal" consistency
Average duration per run: 9 secs
----------
Assertion | #Violation among 15 runs | Violation%
A15       | 9                        | 60.00
A16       | 9                        | 60.00
----------
```

It briefs about the parameters. Then prints a tables with the number of violations and the percentage of violations.
It also prints an average duration per run.

If an assertion is not present in the table, its count (and also its percentage) is zero.

## Runtime
This particular artifact was run on a Intel-i7 laptop with 16GB RAM.



The table below lists the average duration per run in seconds.

| benchmarks | tpcc |
|-|:-:|
| 2 nodes | 60 |
| 3 nodes | 60 |
| 5 nodes | 60 |
| 7 nodes | 60 |

## Kick-the-tire command

```
bash run.sh 5 3 10
```

It should finish in less than 5 minutes.

## One shot command

```
bash run.sh 100 2,3,5,10 10
```

It will take around 20 hours to finish.

## Produce results from paper

The one shot command is enough. But one can reproduce the results one by one.

Note. More runs for each set of parameters is preferable to produce more correct trend. Suggested minimum number of runs is 100.

# Futher usage

This codebase was made possible only to compare Jepsen's performance on TPCC with MonkeyDB.

We borrowed the java code presented in `tpcc/oltp_src` from authors of [Rahmani et al. 2019] and extended it to make it work for MariaDB cluster.

The MariaDB cluster deployment and setup code is presented in `src/tpcc/core.clj`.