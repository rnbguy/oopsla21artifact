## Profiling the Coyote scheduler
We use `valgrind` for profiling the scheduler on Linux and WSL.

To install `valgrind` run:
```
sudo apt-get install -y valgrind
```

To profile an executable that is using the Coyote scheduler run:
```
valgrind --tool=callgrind <PATH_TO_EXECUTABLE>
```

This will dump a file named `callgrind.out.<PID>` (where `<PID>` was the process id of the
executable that just run).

To see the stacks, run:
```
callgrind_annotate  callgrind.out.<PID>
```

To see counts for each statement (rather than just at a function level) add the `--auto=yes` option. To see inclusive results add the `--inclusive=yes` option.