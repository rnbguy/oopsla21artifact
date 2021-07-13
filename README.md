# MonkeyDB Artifact

This repository contains the software artifact that supports our OOPSLA'21 submission on "_MonkeyDB: Effectively Testing Correctness against Weak
Isolation Levels_". MonkeyDB is a mock database that is capable of producing _weak_ behaviors possible under a given isolation levels. 
Applications can use MonkeyDB to test the correctness of their logic when using a DB configured with a weak isolation level.

Our artifact is split into three parts because they each require a different setup. Each part contains its own readme with instructions to build and run experiments.
- The directory [`microbenchmarks`](https://github.com/rnbguy/oopsla21artifact/blob/master/microbenchmarks/README.md) supports section 7 (Evaluation: Microbenchmarks) of the paper. It contains a set of microbenchmarks, inspired from real-world applications.
These benchmarks are linked against the Key-Value interface of MonkeyDB and executed multiple times to observe state coverage (Fig. 13) and assertion failures (Table 1).
- The directory [`monkeydb`](https://github.com/rnbguy/oopsla21artifact/blob/master/monkeydb/README.md) supports the main results of the paper in sections 8.1 and 8.2 (Fig. 14 and 15). It contains OLTP benchmarks (TPCC, Smallbank, Voter and Wikipedia) 
that are executed against the SQL interface of MonkeyDB. 
- The directory [`jepsen`](https://github.com/rnbguy/oopsla21artifact/blob/master/jepsen/README.md) supports Section 9 (Comparison to other testing techniques). This experiment does not use MonkeyDB, instead it runs a version of TPCC using Jepsen on a real DB. This reproduces Fig. 16 of the paper.

We note that, for historic reasons, there are two versions of the Key-Value interface for MonkeyDB. The main implementation is the one contained in the `monkeydb` directory. It supports both SQL as well as Key-Value interfaces and resembles the architecture presented in the paper (Fig. 11). We have an alternative implementation in the `microbenchmarks` directory that only support a Key-Value interface. The former is Rust and the latter is C++. Both implementations are semantically the same.
