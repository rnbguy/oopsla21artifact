#
# Runs MonkeyDB KV microbenchmark experiments.
#
# Args (optional):
#
# Number of iterations (default = 20)
# Number of random test cases (default = 10)
#

#
# This script runs all the applications with the specified number of iterations
# and test cases. The applications include:
#
#   Shopping Cart
#   Courseware
#   Stack
#   Twitter
#
# There are two kinds of experiments for each application:
#
#   Random : Application randomly chooses the operations, subject to pre-defined
#            number of operations and threads (min: 2 and max: 3).
#   Fixed  : A fixed test case to find violation in the application.
#

#
# Output:
#
#   Number of violations found with the given number of iterations.
#   A plot with the number of unique states observed across different iterations
#   averaged across all random test cases for each application.
#

SECONDS=0
iter=20
test_cases=10

while test $# -gt 0; do
  case "$1" in
    -h|--help)
      echo "BuildAndRunMicrobenchmarks script, usage:"
      echo " "
      echo "./BuildAndRunMicrobenchmarks.sh [options]"
      echo " "
      echo "options:"
      echo "-h, --help                show brief help"
      echo "-i, --numIterations=X     specify number of iterations"
      echo "-t, --testCases=Y         specify number of test cases"
      exit 0
      ;;
    -i)
      shift
      if test $# -gt 0; then
        iter=$1
      fi
      shift
      ;;
    --numIterations*)
      iter=`echo $1 | sed -e 's/^[^=]*=//g'`
      shift
      ;;
    -t)
      shift
      if test $# -gt 0; then
        test_cases=$1
      fi
      shift
      ;;
    --testCases*)
      test_cases=`echo $1 | sed -e 's/^[^=]*=//g'`
      shift
      ;;
    *)
      break
      ;;
  esac
done

echo "Iterations = $iter Test cases = $test_cases"

#
# Build MonkeyDB with apps
#

rm -rf build-files
mkdir build-files
cd build-files/

# Build monkeyDB
echo "Building MonkeyDB Key-Value Store..."
cmake ../ > build_log.txt 2>&1
echo "MonkeyDB build finished"
echo ""

# Build applications
echo "Building MonkeyDB applications..."
cmake --build ./ --target courseware_app shopping_cart_app twitter_app stack_app > app_build_log.txt 2>&1
echo "MonkeyDB applications build finished"
echo ""

#
# Run apps
#

cd applications/

mkdir -p shopping_cart/
mkdir -p courseware/
mkdir -p stack/
mkdir -p twitter/

#
# Shopping Cart
#

echo "---------------------------------------------------------"
echo "Running shopping cart application..."
echo ""

echo "Finding violations"
echo "With causal"
./shopping_cart_app temp.log $iter causal fixed 1 1

echo "With serializability"
./shopping_cart_app temp.log $iter linear fixed 1 1

echo ""

# DFS
echo "Running random test cases..."
echo "Running DFS to compute max states"
echo "With causal"
./shopping_cart_app shopping_cart/causal_max $iter causal random $test_cases 0 coyote_dfs
echo ""
echo "With serializability"
./shopping_cart_app shopping_cart/serializability_max $iter linear random $test_cases 0 coyote_dfs
echo ""

# Causal
echo "Running random test cases with MonkeyDB (causal)"
./shopping_cart_app shopping_cart/causal_delay $iter causal random $test_cases 1
./shopping_cart_app shopping_cart/causal $iter causal random $test_cases 0

# Serializability
echo "Running random test cases with MonkeyDB (serializability)"
./shopping_cart_app shopping_cart/serializability_delay $iter linear random $test_cases 1
./shopping_cart_app shopping_cart/serializability $iter linear random $test_cases 0

echo "Shopping cart run finished"
echo ""

#
# Courseware
#
echo "---------------------------------------------------------"
echo "Running courseware application..."
echo ""

echo "Finding violations"
echo "With causal"
./courseware_app temp.log $iter causal fixed 1 1

echo "With serializability"
./courseware_app temp.log $iter linear fixed 1 1

echo ""

# DFS
echo "Running random test cases..."
echo "Running DFS to compute max states"
echo "With causal"
./courseware_app courseware/causal_max $iter causal random $test_cases 0 coyote_dfs
echo ""
echo "With serializability"
./courseware_app courseware/serializability_max $iter linear random $test_cases 0 coyote_dfs
echo ""

# Causal
echo "Running random test cases with MonkeyDB (causal)"
./courseware_app courseware/causal_delay $iter causal random $test_cases 1
./courseware_app courseware/causal $iter causal random $test_cases 0

# Serializability
echo "Running random test cases with MonkeyDB (serializability)"
./courseware_app courseware/serializability_delay $iter linear random $test_cases 1
./courseware_app courseware/serializability $iter linear random $test_cases 0

echo "Courseware run finished"
echo ""

#
# Stack
#
echo "---------------------------------------------------------"
echo "Running stack application..."
echo ""

echo "Finding violations"
echo "With causal"
./stack_app temp.log $iter causal fixed 1 1

echo "With serializability"
./stack_app temp.log $iter linear fixed 1 1

echo ""

# DFS
echo "Running random test cases..."
echo "Running DFS to compute max states"
echo "With causal"
./stack_app stack/causal_max $iter causal random $test_cases 0 coyote_dfs
echo ""
echo "With serializability"
./stack_app stack/serializability_max $iter linear random $test_cases 0 coyote_dfs
echo ""

# Causal
echo "Running random test cases with MonkeyDB (causal)"
./stack_app stack/causal_delay $iter causal random $test_cases 1
./stack_app stack/causal $iter causal random $test_cases 0

# Serializability
echo "Running random test cases with MonkeyDB (serializability)"
./stack_app stack/serializability_delay $iter linear random $test_cases 1
./stack_app stack/serializability $iter linear random $test_cases 0

echo "Stack run finished"
echo ""

#
# Twitter
#
echo "---------------------------------------------------------"
echo "Running twitter application..."
echo ""

echo "Finding violations"
echo "With causal"
./twitter_app temp.log $iter causal fixed 1 1

echo "With serializability"
./twitter_app temp.log $iter linear fixed 1 1

echo ""

# DFS
echo "Running random test cases..."
echo "Running DFS to compute max states"
echo "With causal"
./twitter_app twitter/causal_max $iter causal random $test_cases 0 coyote_dfs
echo ""
echo "With serializability"
./twitter_app twitter/serializability_max $iter linear random $test_cases 0 coyote_dfs
echo ""

# Causal
echo "Running random test cases with MonkeyDB (causal)"
./twitter_app twitter/causal_delay $iter causal random $test_cases 1
./twitter_app twitter/causal $iter causal random $test_cases 0

# Serializability
echo "Running random test cases with MonkeyDB (serializability)"
./twitter_app twitter/serializability_delay $iter linear random $test_cases 1
./twitter_app twitter/serializability $iter linear random $test_cases 0

echo "Twitter run finished"
echo ""

echo "Generating plots for random test cases from the logs..."
log_dir="$(pwd)/"
python3 ../../plot_history/plot.py $log_dir $iter > plot_log.txt 2>&1

echo "plot.pdf saved in $log_dir"
echo "Use docker cp to copy to host OS and view:"
echo "docker cp <container_name>:$(pwd)/plot.pdf plot.pdf"
echo "Use this command from host OS to get the container name : docker -ps a "

# Uncomment to display plot pdf, not supported in docker
#xdg-open plot.pdf

# Print time taken by the script
duration=$SECONDS
echo "$(($duration / 3600)) hours, $((($duration / 60) % 60)) minutes and $(($duration % 60)) seconds elapsed."
