OLTP on MonkeyDB
================

# Description

We execute the following four benchmarks from [OLTPBench](https://github.com/oltpbenchmark/oltpbench) (source code included) on MonekyDB.
- TPCC
- smallbank
- voter
- Wikipedia

Then we check for the following assertions (subsection 8.1, 8.2) on these benchmarks and present the number of violations.
- A1-A12 (8.1) for TPCC
- A13 (8.2) for smallbank
- A14 (8.2) for voter
- A15-A17 (8.2) for wikipedia

# Implementation

## OLTPBench modifications

We use four benchmarks from OLTPBench. As our implementation does not support all the modern SQL syntax, we had to modify some parts of the original OLTPBench code to transform complicated SQL queries into a group of simplified queries. The modified codes are available in the following directories,
- `oltpbench/src/com/benchmarks/tpcc`
- `oltpbench/src/com/benchmarks/smallbank`
- `oltpbench/src/com/benchmarks/voter`
- `oltpbench/src/com/benchmarks/wikipedia`

## MonkeyDB

MonkeyDB is implemented in the Rust language. The source code is present inside the `src` directory. It consists of the following components:

- The SQL client-server connection is present in `src/server.rs`. 
- The compliation of SQL to Key-Value (KV) api (Section 5) is in `src/sql.rs`.
- The KV store (Section 6) is implemented in `src/dis_kv.rs`.

# Results from paper

We present the results of our experiments as 2D plots, as in Figure 14 and 15 in the paper. This artifact reproduces the same plots. (Note that there is randomness involved, so the results will not reproduce exactly, but only approximately the same. The longer you run the artifact, the closer the results would be.)

# Option 1: Use Docker

This is the recommended option for running this artifact. We also provide a second option without docker later but this option is preferable because it ensures that the correct dependencies are installed. 

We provide a [`Dockerfile`](Dockerfile) based on Archlinux docker image. _Docker containers are supported in any OS as long as [Docker is installed](https://docs.docker.com/get-started)._ Here are the necessary steps for building (takes 3 to 15 min):

```
# build the docker image. this includes `bash build.sh`
docker build . -t oopsla21_aec48
```

Next, start the container as follows.

```
# start the docker container
docker run -it oopsla21_aec48
```

We provide a script called `run.sh` for running the experiments. It requires the following parameters:

`bash run.sh <iterations> <benchmark> <consistency> <number of nodes> <timelimit>`

For example, in order to run `tpcc` under `causal` consistency, configured with `3` replicas for a total of `20` iterations, and a runtime limit of `10` secs per iteration, you may run the following command:

`bash run.sh 20 tpcc causal 3 10`

Possible values of parameters:
- benchmark: `tpcc`, `smallbank`, `voter`, `wikipedia`
- consistency: `causal`, `readcommitted`

You can pass comma-separated multiple values to run for multiple configurations together. For example:

`bash run.sh 20 tpcc,smallbank causal,readcommitted 3,5 10`

will run both `tpcc` and `smallbank` under `causal` and `readcommitted` consistency on `3` and `5` replica setup.

For kick-the-tires, run the following:

```
bash run.sh 15 wikipedia causal 3 10
```

It should finish in less than 5 minutes. (See below for expected output.)

_Logs will be generated in `log` directory._

For the full experiments:

```
bash run.sh 100 tpcc,smallbank,voter,wikipedia causal,readcommitted 2,3 10
```

It will take around 20 hours to finish and will most closely resemble the results presented in the paper (Fig. 14 and 15).

The output of this command with `20` iterations that we obtained on our machine is available at [`output_example.txt`](output_example.txt).

# Option 2: Self (non-docker) install

We require the following set of dependencies to be installed. 
- A stable `rust` toolchain. (Install via [Rustup](https://www.rust-lang.org/tools/install))
- Apache Ant (for OLTPBench)
- MySQL console client (for OLTPBench)
- libssl, clang
- Bash and its utilities

We have verified that the following simple commands are enough to install these dependencies.
- If you are using apt, `apt install cargo ant mariadb-client libssl-dev clang bsdmainutils`
- If you are using pacman, `pacman -S rustup ant mariadb openssl clang base-devel --needed; rustup install stable`

From the current directory, execute `bash build.sh`. This will take around 3 to 15 min, depending on how many packages need to be installed.
For quick sanity checking, run:

```
bash run.sh 15 wikipedia causal 3 10
```

It should finish in less than 5 minutes. Use this to verify your setup. (See below for expected output.)

# Output

A sample output would look as follows,

```
$ bash run.sh 15 wikipedia causal 3 10
==========
Benchmark: wikipedia
----------
15 runs with time limit of 10 secs
On 3 nodes with "causal" consistency
Average duration per run: 9 secs
-----------------------------------------------
Assertion  #Violation among 15 runs  Violation%
A15        9                         60.00
A16        9                         60.00
-----------------------------------------------
```

It briefs about the parameters. Then it prints a table with the number of violations and the percentage of violations.
It also prints the average duration per run. If an assertion is not present in the table, its count (and also its percentage) is zero. 
The output above shows that `A15` and `A16` were violated 60% times and `A17` was never violated.


# Runtime

This particular artifact was run on an Intel-i7 (gen 7) laptop with 16GB RAM.

The table below lists the average duration per run in seconds.

| benchmarks | tpcc | smallbank | voter | wikipedia |
|-|:-:|:-:|:-:|:-:|
| 2 nodes ; causal | 60 | 15 | 15 | 15 |
| 3 nodes ; causal | 60 | 15 | 15 | 15 |
| 2 nodes ; readcommitted | 170 | 15 | 15 | 15 |
| 3 nodes ; readcommitted | 170 | 15 | 15 | 15 |

# Futher usage

Our main contribution is MonkeyDB. It is just a `cargo` binary project. To run MonekeyDB, execute `cargo run --release` and MonkeyDB will serve SQL queries at default 3306 port. The binary will also be available in the `target/release` directory.

You can make MonkeyDB listen to some other port as `cargo run --release -- -a 8800` (check `cargo run --release -- --help`).

Once MonkeyDB is up, it is ready to connect to any MySQL client such as MySQL console client or even Rust MySQL client. You can connect to it concurrently from multiple terminals as well. MonkeyDB currently does not support everything that a modern MySQL server does. So complex SQL queries may not work with MonkeyDB currently.

## Special commands for MonkeyDB

There are few special commands for MonkeyDB.

| Query | Description |
|-|-|
|`reset`|Erase the whole database|
|`loading reset`|Reset the database back to loading phase and start weak consistency|
|`set consistency (readcommitted\|causal)`|Set consistency level|

1. By default, MonkeyDB starts with a Serializable level. At this point, we can load unambiguous initial data to MonkeyDB. We execute OLTPBench's loading phase at this point.
2. Then we send `loading reset` command to stop the loading phase and move to a weak consistent database. We execute OLTPBench's execution phase at this point. By default, the level is Causal Consistency.

### `commit` at the end of transactions

MonkeyDB processes each transaction in a sequence. The transaction requests possibly come in parallel. So a transaction on session A stays blocked, until a previously started transaction commits on session B. This is why the transactions must be committed before progressing on transactions in a different session.

For example, if we have two MySQL client consoles - `mysql1` and `mysql2`

If we query something on `mysql1` first.

```
mysql1> SELECT * FROM Table;
...
```

and, before _committing_ on `mysql1`, we query on `mysql2`.

```
mysql2> SELECT * FROM Table;
# stuck here 
```

Then `mysql2` will stay blocked until `commit` is called on `mysql1`.

```
mysql1> commit;
```
