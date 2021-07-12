// ------------------------------------------------------------
// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
//

#include "treiber_stack.h"
#include "../app_config.h"
#include "../utils.h"
#include "../../kv_store/include/read_response_selector.h"

#include <iostream>
#include <vector>
#include <thread>
#include <boost/log/trivial.hpp>
#include <boost/log/utility/setup/file.hpp>
#include <assert.h>
#include <chrono>

#define NUM_SESSIONS 3
#define NUM_OPS 3

std::vector<std::vector<bool>> operations(NUM_SESSIONS);
std::vector<int> results(6, 0);
std::vector<int> assert_counter(6, 0);

app_config *config;

void assert_count(bool result, int assert_index) {
    if (result)
        assert_counter[assert_index]++;
}

// Checks if any pop returned a pushed value multiple times
void check_pop_valid() {
    std::unordered_set<int> unique_vals;
    bool repeat_value = false;
    for (auto e : results) {
        if (e != 0) {
            if (unique_vals.find(e) != unique_vals.end()) {
                repeat_value = true;
            }
            unique_vals.insert(e);
        }
    }
    assert_count(repeat_value, 0);

}

// Randomly choose operations for each thread
void random_fill(long seed) {
    for (int i = 1; i <= NUM_SESSIONS; i++) {
        long thread_seed = seed * 97 + 19;
        std::mt19937 generator (thread_seed);
        operations[i - 1].resize(NUM_OPS);
        for (int j = 1; j <= NUM_OPS; j++) {
            unsigned long long random_value = generator();
            operations[i - 1][j - 1] = random_value % 2;
        }
    }
}

void do_operations(treiber_stack<int> *stack, int t_id) {
    config->scheduler->start_operation(t_id);
    std::vector<int> pop_values;
    for (int i = 1; i <= operations[t_id - 1].size(); i++) {
        if (operations[t_id - 1][i - 1]) {
            stack->push(t_id * 10 + i, config->delay, t_id);
        }
        else {
            pop_values.push_back(stack->pop(config->delay, t_id));
        }
    }
    if (!config->random_test) {
        for (int i = 0; i < pop_values.size(); i++)
            results[(t_id - 1) * 2 + i] = pop_values[i];
    }
    config->scheduler->complete_operation(t_id);
}
/*
 */
void run_iteration() {
    config->scheduler->attach();

    nekara::read_response_selector<long, std::pair<int, long>> *get_next_tx;

    if (config->consistency_level == consistency::causal)
        get_next_tx = new nekara::causal_read_response_selector<long, std::pair<int, long>>();
    else if (config->consistency_level == consistency::linear)
        get_next_tx = new nekara::linearizable_read_response_selector<long, std::pair<int, long>>();
    else if (config->consistency_level == consistency::k_causal)
        get_next_tx = new nekara::k_causal_read_response_selector<long, std::pair<int, long>>(2,
                                                                                              operations.size() * operations[0].size()/2);

    nekara::kv_store<long, std::pair<int, long>> *store = new nekara::kv_store<long, std::pair<int, long>>(get_next_tx);
    if (config->coyote_enabled)
        get_next_tx->init_consistency_checker(store, config->scheduler);
    else
        get_next_tx->init_consistency_checker(store);

    treiber_stack<int> *stack = new treiber_stack<int>(store, config->scheduler, config->coyote_enabled);
    std::vector<std::thread> threads;

    for (int i = 1; i <= NUM_SESSIONS; i++) {
        config->scheduler->create_operation(i);
        threads.push_back(std::thread(do_operations, stack, i));
    }

    for (int i = 1; i <= threads.size(); i++)
        config->scheduler->join_operation(i);

    for (auto &t : threads)
        t.join();

    delete store;
    delete get_next_tx;
    config->scheduler->detach();
    assert(config->scheduler->error_code() == coyote::ErrorCode::Success);
}

/*
 * Args:
 * log_filename
 * num of iterations
 * consistency-level: linear, causal, k-causal
 * random/fixed
 * number of random runs (use 1 for fixed)
 * coyote/coyote-dfs (optional)
 */
int main(int argc, char **argv) {
    config = parse_command_line(argc, argv);
    init_logging(config->log_file_name);
    int dfs_finished = 0;

    if (!config->random_test){
        // T1: PUSH PUSH PUSH
        // T2: POP POP POP
        operations = {
                {1, 1, 0},
                {0, 1, 0},
                {0, 0}
        };
    }

    for (int i = 1; i <= config->num_random_test; i++) {
        if (config->random_test) {
            BOOST_LOG_TRIVIAL(info) << "RANDOM " << i - 1 << " start";
            random_fill(i * 19 + 11);
        }
        if (config->coyote_dfs) {
            delete config->scheduler;
            config->scheduler = new coyote::Scheduler("DFSStrategy");
        }
        for (int j = 0; j < config->iterations; j++) {
            BOOST_LOG_TRIVIAL(info) << "Iteration " << j << " start";
            std::chrono::steady_clock::time_point begin = std::chrono::steady_clock::now();
            run_iteration();
            std::chrono::steady_clock::time_point end = std::chrono::steady_clock::now();

            check_pop_valid();

            results[0] = results[1] = results[2] = results[3] = 0;
            std::stringstream ss;
            for (auto k : assert_counter)
                ss << k << " ";
            BOOST_LOG_TRIVIAL(info) << ss.str();
            BOOST_LOG_TRIVIAL(info) << "Iteration " << j << " end "
                                    << std::chrono::duration_cast<std::chrono::microseconds>(end - begin).count();
            if (config->coyote_dfs && config->scheduler->completely_explored()) {
                dfs_finished++;
                break;
            }
        }
        if (config->random_test) {
            BOOST_LOG_TRIVIAL(info) << "RANDOM " << i - 1 << " end";
        }
    }

    if (!config->random_test) {
        int violation_count = assert_counter[0];

        std::cout << "Total violations found: " << violation_count
                  << " in " << config->iterations << " iterations\n";
    }

    if (config->coyote_dfs) {
        std::cout << "DFS finished in " << dfs_finished << " out of "
                  << config->num_random_test << " test cases with "
                  << config->iterations << " iterations\n";
    }

    delete config;

    return 0;
}
