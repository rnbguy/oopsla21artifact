// ------------------------------------------------------------
// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
//

#include "../treiber_stack/treiber_stack.h"
#include "../app_config.h"
#include "../utils.h"
#include "../../kv_store/include/read_response_selector.h"

#include <iostream>
#include <vector>
#include <thread>
#include <boost/log/trivial.hpp>
#include <boost/log/utility/setup/file.hpp>
#include <assert.h>

#define NUM_SESSIONS 3
#define NUM_OPS 3

/*
 * Simple app does PUT and GET to kv-store through multiple sessions (threads).
 */
std::vector<std::vector<std::pair<std::string, int>>> operations(NUM_SESSIONS,
                                                                 std::vector<std::pair<std::string, int>>(NUM_OPS));

std::vector<std::string> keys = {"a", "b"};

app_config *config;

void do_operations(nekara::kv_store<std::string, int> *store, int t_id) {
    config->scheduler->start_operation(t_id);
    for (int i = 1; i <= NUM_OPS; i++) {
        std::string key = operations[t_id - 1][i - 1].first;
        int value = operations[t_id - 1][i - 1].second;
        if (value == -1) {
            coyote::ErrorCode e = config->scheduler->schedule_next();
            assert(e == coyote::ErrorCode::Success);
            try {
                store->get(key, t_id);
            } catch (std::exception &e) {
                // Pass
            }
        }
        else {
            coyote::ErrorCode e = config->scheduler->schedule_next();
            assert(e == coyote::ErrorCode::Success);
            store->put(key, value, t_id);
        }
    }
    config->scheduler->complete_operation(t_id);
}
/*
 */
void run_iteration() {
    config->scheduler->attach();

    nekara::read_response_selector<std::string, int> *get_next_tx;

    if (config->consistency_level == consistency::causal)
        get_next_tx = new nekara::causal_read_response_selector<std::string, int>();
    else if (config->consistency_level == consistency::linear)
        get_next_tx = new nekara::linearizable_read_response_selector<std::string, int>();
    else if (config->consistency_level == consistency::k_causal)
        get_next_tx = new nekara::k_causal_read_response_selector<std::string, int>(2,
                                                                                    operations.size() * operations[0].size()/2);

    nekara::kv_store<std::string, int> *store = new nekara::kv_store<std::string, int>(get_next_tx);
    if (config->coyote_enabled)
        get_next_tx->init_consistency_checker(store, config->scheduler);
    else
        get_next_tx->init_consistency_checker(store);

    std::vector<std::thread> threads;

    for (int i = 1; i <= NUM_SESSIONS; i++) {
        config->scheduler->create_operation(i);
        threads.push_back(std::thread(do_operations, store, i));
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
 * coyote/coyote-dfs (optional)
 */
int main(int argc, char **argv) {
    config = parse_command_line(argc, argv);
    init_logging(config->log_file_name);

    // T1: PUT(a) GET(a) PUT(b)
    // T2: PUT(a) PUT(b) GET(b)
    // T3: GET(a) GET(b) PUT(b)
    operations = {
            {{"a", 10}, {"a", -1}, {"b", 12}},
            {{"a", 20}, {"b", 21}, {"b", -1}},
            {{"a", -1}, {"b", -1}, {"b", 32}}
    };

    for(int j = 0; j < config->iterations; j++) {
        BOOST_LOG_TRIVIAL(info) << "Iteration " << j << " start";
        std::chrono::steady_clock::time_point begin = std::chrono::steady_clock::now();
        run_iteration();
        std::chrono::steady_clock::time_point end = std::chrono::steady_clock::now();
        BOOST_LOG_TRIVIAL(info) << "Iteration " << j << " end " << std::chrono::duration_cast<std::chrono::microseconds>(end - begin).count();
    }

    delete config;
    return 0;
}