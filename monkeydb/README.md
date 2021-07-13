MonekyDB
========

# Description
MonkeyDB is a mock database to produce behvaiors of weaker isolation level deliberately.
Distributed applications can use this database to expose application level bugs which may only be produced by _corner cases_.

We execute the following four benchmarks from [OLTPBench](https://github.com/oltpbenchmark/oltpbench) (source code included) on MonekyDB.
- TPCC
- smallbank
- voter
- Wikipedia

Then we check for the following assertions (subsection 8.1, 8.2) on these benchmarks and present the number of violation.
- A1-A12 (8.1) for TPCC
- A13 (8.2) for smallbank
- A14 (8.2) for voter
- A15-A17 (8.2) for wikipedia

# Results from paper
We present the statistics of our findings as 2D plots in figure 14 and 15 in the paper. This artifact reproduces the same.

# Dependencies
- A stable `rust` toolchain. (Install via [Rustup](https://www.rust-lang.org/tools/install))
- Bash and its common utilities
- Apache Ant + Java (for OLTPBench)
- MySQL console client (for OLTPBench)

# Instruction

## Build

From the current directory, execute `bash build.sh`

## Execute

_Logs will be generated in `log` directory._

To reproduce the results from the plots in Figure 14 and 15 in the paper, execute `run.sh` script.

`bash run.sh <number of runs> <benchmark> <consistency> <number of nodes> <timelimit>`

An example: to check the assertions for `tpcc` under `causal` consistency on `3` replicas with a runtime limit of `10` secs for `20` times, you may run following command

`bash run.sh 20 tpcc causal 3 10`

Possible values:
- benchmark: `tpcc`, `smallbank`, `voter`, `wikipedia`
- consistency: `causal`, `readcommitted`

You can pass comma separated multiple values to run for multiple parameter values together. Example,

`bash run.sh 20 tpcc,smallbank causal,readcommitted 3,5 10`

will run both `tpcc` and `smallbank` under `causal` and `readcommitted` consistency on `3` and `5` replica setup.

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

As the out shows, A15 and A16 were violated 60% time and A17 was not violated at all.

## Runtime
This particular artifact was run on a Intel-i7 laptop with 16GB RAM.

The table below lists the average duration per run in seconds.

| benchmarks | tpcc | smallbank | voter | wikipedia |
|-|:-:|:-:|:-:|:-:|
| 2 nodes ; causal | 60 | 15 | 15 | 15 |
| 3 nodes ; causal | 60 | 15 | 15 | 15 |
| 2 nodes ; readcommitted | 170 | 15 | 15 | 15 |
| 3 nodes ; readcommitted | 170 | 15 | 15 | 15 |

## Kick-the-tire command

```
bash build.sh
bash run.sh 10 wikipedia causal 3 10
```

It should finish in less than 5 minutes.

## One shot command

```
bash build.sh
bash run.sh 100 tpcc,smallbank,voter,wikipedia causal,readcommitted 2,3 10
```

It will take around 20 hours to finish.

## Produce results from paper

The one shot command is enough. But one can reproduce the results one by one.

Note. More runs for each set of parameters is preferable to produce more correct trend. Suggested minimum number of runs is 100.
