Kick-the-tires Response
======================

We thank the reviewers for the kick-the-tires comments. We fixed our artifact and added more details.

We updated the Github repository. Please `git pull` for the latest commits.

### Concern 1

Our artifacts are presented in three different directories - `jepsen`, `microbenchmarks`, and `oltp`.
Commands and scripts for each of them should be executed in their corresponding directory.

Here is a start-to-finish example.

```
git clone git@github.com:rnbguy/oopsla21artifact
cd oopsla21artifact
# inside artifact directory

cd oltp
# inside sub-artifact directory

# read the README file
less README.md

# IMPORTANT: make sure the dependencies are installed

# execute the scripts mentioned in the README.md
bash build.sh
bash run.sh 15 wikipedia causal 3 10
```

### Concern 2

We added an example output for `oltp` artifact at [`oltp/output_example.txt`](oltp/output_example.txt). We used the following command

```
[oltp] $ bash run.sh 20 tpcc,smallbank,voter,wikipedia causal,readcommitted 2,3 10 > output_example.txt
```

This is essentially the _one-shot_ command but with 20 runs per unique configuration. The choices for configuration parameters,
* `tpcc`, `smallbank`, `voter` or `wikipedia` benchmarks
* `causal` or `readcommitted` weak isolation level
* `2` or `3` nodes
* `10` second timeout

### Concern 3

For the `mkdir` error, there was a small mistake in `oltp/run.sh`, that we have fixed and updated our Github Repository.
