# MonkeyDB Microbenchmarks

## Dependencies

This requires docker.

## Build

For building, simply run the following:
```
docker build .
```
The last statement of the output of above command would contain the image name, e.g.:
```
 ---> e579223d56b3
Successfully built e579223d56b3
```
Use the same image name to run a docker container:
```
docker run -it <image_name> /bin/bash
```
All of the above commands should finish in less than 5 minutes. (On a fresh OS with missing dependencies, it might take up to 15 min for the docker build to finish.)

# Kick the tires command

For sanity checking, run the following script after executing the above commands, which will run a very small version of our experiments. This should complete without errors with a message that says `Generating plots ...`.
```
./BuildAndRunMicrobenchmarks.sh
```

If running the script `BuildAndRunMicrobenchmarks` returns an error `$'\r': command not found` then run this command first to get rid of the carriage return characters `sed 's/\r$//' BuildAndRunMicrobenchmarks.sh > BuildAndRunMicrobenchmarksFixed.sh`. Then do `chmod u+x BuildAndRunMicrobenchmarksFixed.sh` and finally use `BuildAndRunMicrobenchmarksFixed` in place of the original script `BuildAndRunMicrobenchmarks`.

# Full results

To reproduce results of Fig. 13 of the paper: 
```
./BuildAndRunMicrobenchmarks.sh --numIterations=5000 --testCases=50
```
or
```
./BuildAndRunMicrobenchmarks.sh -i 5000 -t 50
```

This will generate the final plots as PDF. Please see the [output](#output) section.

**Notes**
- The parameters used above can take a long time (~ one day), so it is recommended to run with smaller iterations/test-cases, for instance:
- ```
  ./BuildAndRunMicrobenchmarks.sh --numIterations=2000 --testCases=30
  ```
  This is expected to run within 2-3 hours. Trying with --numIterations=1000 and --testCases=20 should also suffice to see the general trend in the plot.
- We use DFS (Depth First Search) to compute maximum number of possible states, but it will likely not finish within 5000 iterations, in which case it will indicate that the max states could not be computed correctly. In order to limit the overall time for running experiments, it is necessary to restrict the number of iterations. As a result, the plot containing `causal_max` and `serializability_max` might not indicate true value of the total number of states. In our paper, we ran the experiment for much longer (over multiple days) to let DFS finish without bounding iterations. 
  

In order to reproduce the assertions failure experiment (Table 1), run each application individually with a small number of iterations and time the command. For instance:

```
cd build-files/applications/
time ./twitter_app twitter.log 20 causal fixed 1 1
time ./shopping_cart_app cart.log 20 causal fixed 1 1
```

This will runs the applications with small number of iterations (20) with causal consistency. The time it took for each iteration is also present in logs (logged with the iteration end statement - in microseconds). We used the time printed in the logs for Table 1 in paper.


## Microbenchmarks

The applications include the following:
- Shopping Cart
- Courseware
- Stack
- Twitter

There are two kinds of experiments for each application that differ in how the test driver invokes the application.
- _Random_ : The test driver randomly chooses from one of the application operations, subject to pre-defined
           maximum number of operations and threads (min: 2 and max: 3). This mode is used for state coverge (Fig. 13).
- _Fixed_  : A fixed test driver to find specific violation in the application. This mode is used for assertion violations (Table 1).

Each application's violations are summarized in the below table:

| Application   | Assertion                     |
|---------------|-------------------------------|
| Stack         | Element popped more than once |
| Courseware    | Course registration overflow  |
| Courseware    | Removed course registration   |
| Shopping Cart | Item reappears after deletion |
| Twitter       | Missing tweets in feed        |

#### Output

 - Number of violations found with the given number of iterations.
 - A plot with the number of unique states observed across different iterations averaged
  across all random test cases for each application. (Location: /MonkeyDB-KV/build-files/applications/plot.pdf)
   
   _Note_: PDF can not be viewed directly from docker container so it needs to be copied to host OS using the following command (run from outside the docker container):
  ```
docker cp <container_name>:/MonkeyDB-KV/build-files/applications/plot.pdf plot.pdf
```
where container name can be found out using command:
```
docker ps -a
```
  
  
### Code Structure
#### MonkeyDB-KV
MonkeyDB key-value interface is written as a C++ library that can be included in any application. It also supports an HTTP API to allow applications developed in other languages to use MonkeyDB-KV. Application can make use of simple key-value API which involves:
- get(_key_: x, _session-id_)
- put(_key_: x, _value_: y, _session-id_)

_Session-id_ is an optional parameter that can be used to uniquely identify different clients interacting with the store. We internally use a consistency checker to return a randomly chosen value which is consistent with the specified isolation level. MonkeyDB KV-interface currently supports causal and serializability.

The kv-interface is defined in `kv_store/include/kv_store.h` and the HTTP interface is defined in `kv_store/include/http_server.h`. The history of all the operations is stored within the kv-store, which is used by the consistency-checker (defined in `kv_store/include/consistency_checker.h`) to choose a response for read operation. The writes do not exhibit any randomness and are simply added to the history, creating a new version of the key. We have made the library extensible as more isolation levels can be easily added by extending the `consistency_checker` interface.

#### Applications
For each application, the source code contains:

* Files describing internal APIs used by the application

* Application API which is used by the client and its implementation using the key-value store interface

* Client code (test driver) which can run in two modes:
  * randomly select operations
  * specific operations with assertions
  
* Client's mode can be selected using command line parameter: random/fixed

Code structure of each application is as follows:
* Twitter:

  * Internal APIs: tweet.h, user.h 
  * Application API and implementation: twitter.h
  * Client: run_twitter.cpp
    * Assertion: Missing tweets across session

* Shopping Cart:

  * Internal APIs: item.h, user.h 
  * Application API and implementation: shopping_cart.h
  * Client: run_shopping_cart.cpp
    * Assertion: Item reappearing after deletion

* Courseware:

  * Internal APIs: course.h, student.h
  * Application API and implementation: courseware.h
  * Client: run_courseware.cpp
    * Assertion: Course registration overflow, removed course registration

* Treiber Stack:

  * Application API and implementation: treiber_stack.h
  * Client: run_stack.cpp
    * Assertion: Element popped more than once

The reader can easily play around with the source code of these benchmarks. For instance, you can modify the test driver or the assertions or even the applications, and then rebuild and re-run the application as outlined above. As a fun exercise problem, you can even try to fix the assertion violations in each application! 
