Kick-the-tires Response
======================

|Regarding kick-the-tire issues|
|-|
|When we submitted, we mentioned Linux for this artifact. Nonetheless, it should have worked on macOS. We have modified the code as per results on multiple Linux laptops, docker containers. The errors were mainly because of unexpected behaviors from OLTPBenchmark (which is an external tool). `git pull` for the latest changes. Although instructions stay the same but read them to avoid any confusion.|

---

We thank the reviewers for their comments. We have made some fixes to our artifact, based on your feedback, and added more details. We have updated the Github repository. Please `git pull` for the latest commits.

### Concern 1 [Reviewer A]
> Neither `build.sh` nor `run.sh` found.

Please note that our artifacts is split into three parts, each in its own directory - `microbenchmarks`, `oltp` and `jepsen`. Each directory corresponds to its own section in the instructions. Perhaps we failed to make it clear that one has to switch directories when switching sections. It seems that you were in the `microbenchmarks` directory but following the directions for `oltp` experiments. Commands and scripts for each directory should be executed in its own directory.

Here is a start-to-finish example.

```
git clone git@github.com:rnbguy/oopsla21artifact

# Go inside artifact directory
cd oopsla21artifact

# Go inside microbenchmarks directory
cd microbenchmarks

# follow the README file (which is section `microbenchmarks` from the instructions)
less README.md

# Build and run a docker image, etc.
# Experiment more with microbenchmarks 
...

# Exit docker (if inside it)
# Go inside the oopsla21artifact/oltp directory
cd ../oltp

# read the README file (which is section `oltp` from the instructions)
less README.md

# Build and run a docker image, etc.
# Experiment more with oltp 
...

# Exit docker (if inside it)

# Go inside the oopsla21artifact/jepsen directory
cd ../jepsen

# follow the README file (which is section `jepsen` from the instructions)
less README.md

# Run the jepsen experiments
...
```

### Concern 2 [Reviewer A]
> Sample output

We added an example output for `oltp` artifact at [`oltp/output_example.txt`](oltp/output_example.txt). We used the following command to generate it:

```
[oltp] $ bash run.sh 20 tpcc,smallbank,voter,wikipedia causal,readcommitted 2,3 10 > output_example.txt
```

This is essentially the _one-shot_ command but with 20 runs per unique configuration. The choices for configuration parameters,
* `tpcc`, `smallbank`, `voter` and `wikipedia` benchmarks
* `causal` and `readcommitted` weak isolation level
* `2` and `3` nodes
* `10` second timeout

### Concern 3 [Reviewer B, C]
> Error running script

Apologies for the `mkdir` error. There was a small mistake in the script `oltp/run.sh` that we have now fixed and updated in our Github Repository. We have revised instructions for the oltp benchmarks to build and run a docker image. That should ensure that these issues are fixed and it is much easier to run the artifact.
