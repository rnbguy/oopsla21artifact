Jepsen
======

# Description
Jepsen is a distributed system testing framework. Developers can write tests for their applications or database using Jepsen. We re-implemented the TPCC benchmark using Jepsen and executed it on a MariaDB database. Then we calculate the number of times the A1-A12 assertions for TPCC (subsection 8.1) were violated. 

One word of caution. We currently use Jepsen's own [docker setup](https://github.com/jepsen-io/jepsen/tree/main/docker). Unfortunately, we did not find this to be very robust --- while on some machines it worked seamlessly, on other (similar) machines, it did not. We are unable to figure out the underlying reason. Given that Jepsen is not our contribution, we did not spend more time debugging this issue with Jepsen. We hope that it works on your machine! 

# Results from the paper
We present our findings as 2D plots in Figure 16 of the paper. This artifact reproduces the same.

# Dependencies
- docker-compose and docker
- Bash and its utilities

If you are using apt, `sudo apt install docker-compose; sudo gpasswd -a <username> docker` and reboot.

If you are using pacman, `sudo pacman -S docker-compose; sudo gpasswd -a <username> docker; sudo systemctl enable docker` and rebbot.
If you are using something different than `systemctl`, change the command accordingly.

# Instructions

## First run

The first run will start building the docker image. This should not take more than 10 minutes.

## Execute

_Logs will be generated in `log` directory._

To reproduce the results from the plots in Figure 16 in the paper, execute `run.sh` script.

`bash run.sh <iterations> <number of nodes> <timelimit>`

To check the assertions for TPCC on `3` replicas with a runtime limit of `10` secs per iteration, for `20` iterations, you may run the following command:

`bash run.sh 20 3 10`

You can pass comma-separated multiple values to run for a set of multiple numbers of nodes together. Example,

`bash run.sh 20 3,5 10`

will run both on `3` and `5` replica setup for `10` seconds timelimit per iteration, for `20` iterations.

## Kick-the-tire command

```
bash run.sh 3 3 10
```

It should finish in less than 20 minutes. See below for expected output.

## One-shot command

```
bash run.sh 100 2,3,5,10 10
```

It will take around 1 day to finish.

## Output

A sample output would look as follows,

```
$ bash run.sh 3 3 10
==========
TPCC on Jepsen + MariaDB cluster
----------
3 runs on 3 nodes with time limit of 10 secs
Average duration per run: 152 secs
----------------------------------------------
Assertion  #Violation among 3 runs  Violation%
A1         3                        100.00
A4         3                        100.00
A8         3                        100.00
A9         3                        100.00
A10        3                        100.00
A12        2                        66.67
-----------------------------------------------
```

The output briefs about the parameter values used. Then it prints a table with the number of violations and the percentage of violations.
It also prints an average duration per iteration. If an assertion is not present in the table, its count (and also its percentage) is zero.

## Runtime
This particular artifact was run on an Intel-i7 (gen 7) laptop with 16GB RAM. The table below lists the expected duration per run in seconds.

| benchmarks | tpcc |
|-|:-:|
| 2 nodes | 75 |
| 3 nodes | 160 |
| 5 nodes | 250 |
| 10 nodes | 300 |

## Reproduce results from paper

The one-shot command is enough. But one can also reproduce the results one benchmark at a time. Note. More number of iterations for each set of parameters is preferable to produce a more correct trend. The suggested minimum number of iterations is 100.

# Futher usage

This code was to compare Jepsen's performance on TPCC with MonkeyDB. We borrowed the java code presented in `tpcc/oltp_src` from authors of [Rahmani et al. 2019] and extended it to make it work for a MariaDB cluster. The MariaDB cluster deployment and setup code are presented in `src/tpcc/core.clj`. Our script uses Jepsen's [docker setup](https://github.com/jepsen-io/jepsen/tree/main/docker) to deploy clusters.
