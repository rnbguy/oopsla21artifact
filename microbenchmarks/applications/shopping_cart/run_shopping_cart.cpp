// ------------------------------------------------------------
// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
//

#include <thread>
#include "../utils.h"
#include "../../kv_store/include/read_response_selector.h"
#include "shopping_cart.h"

#define NUM_SESSIONS 2
#define NUM_OPS 3

app_config *config = nullptr;
nekara::kv_store<std::string, web::json::value> *store;
user u("dip", 1);

// For fixed run
std::vector<int> assert_counter(6, 0);
std::vector<int> results = {0, 0, 0, 0};

std::vector<std::vector<int>> serial_results = {
        {0, 0, 0, 0},
        {0, 0, 4, 0},
        {0, 0, 4, 4},
        {3, 3, 3, 3},
        {0, 0, 3, 3},
        {0, 3, 3, 3},
};

std::vector<std::vector<int>> reappear_results = {
        {0, 4, 4, 4},
        {4, 4, 4, 4}
};
// 0 0 3 0 - causal but not item reappear result

// For random run
std::vector<std::vector<int>> operations(NUM_SESSIONS);
std::vector<item> items = {shoes, ball};

void add_item_to_store(item i) {
    store->put("item:" + std::to_string(i.id) + ":name", web::json::value(i.name));
    store->put("item:" + std::to_string(i.id) + ":price", web::json::value(i.price));
}

void add_user_to_store(user u) {
    store->put("user:" + std::to_string(u.id) + ":name", web::json::value(u.name));
}

/*
 * Insert items and user into store
 */
void populate_shopping_cart() {
    add_item_to_store(shoes);
    add_item_to_store(book);
    add_item_to_store(umbrella);
    add_item_to_store(ball);
    add_item_to_store(bat);
    add_item_to_store(table);

    add_user_to_store(u);
}

void assert_count(bool result, int assert_index) {
    if (result)
        assert_counter[assert_index]++;
}

void serial_count(std::vector<int> &results) {
    bool serial = false;
    for (auto candidate : serial_results) {
        if (candidate == results) {
            serial = true;
        }
    }
    assert_count(serial, 0);
}

void reappear_count(std::vector<int> &results) {
    bool reappear = false;
    for (auto candidate : reappear_results) {
        if (candidate == results) {
            reappear = true;
        }
    }
    assert_count(reappear, 1);
}

/*
 * C1: ADD(X, 2) ADD(Y) | CHANGE_QTY(X, 3) DELETE(X) GET(X) GET_CART
 * C2: ADD(Y) GET(X) GET(Y) | CHANGE_QTY(X, 4) GET(X)
 *
 * Assertions:
 * A1 C1: GET(X) == 0
 * A2 C1: GET_CART() == [(Y, 1)] || [(Y, 2)]
 *
 * A3 C2: GET(X) == 0 || 2
 * A4 C2: GET(Y) == 1 || 2
 * A5 C2: if GET(X) == 2: GET(Y) == 2
 * A6 C2: GET(X) == 4 || 0
 */
void do_operations(shopping_cart *cart, int t_id) {
    config->scheduler->start_operation(t_id);
    if (t_id == 1) {
        cart->tx_start(config->delay);
        cart->add_item(shoes, 2);
        cart->add_item(ball);
        cart->tx_end();

        cart->tx_start(config->delay);
        cart->change_quantity(shoes, 3);
        cart->remove_item(shoes);
        int qt_x = cart->get_quantity(shoes);
        std::vector<std::pair<item, int>> cart_list = cart->get_cart_list();
        cart->tx_end();

        std::cout << "cart_list " << cart_list.size() << std::endl;
        for (auto i : cart_list)
            std::cout << i.first.name << " " << i.second << std::endl;

        assert_count(qt_x == 0, 0);
        assert_count(cart_list.size() == 1 && cart_list[0].first == ball &&
                     cart_list[0].second == 1 || cart_list[0].second == 2, 1);
    }
    else if (t_id == 2) {
        cart->tx_start(config->delay);
        cart->add_item(ball);
        int qt_x = cart->get_quantity(shoes);
        int qt_y = cart->get_quantity(ball);
        cart->tx_end();

        cart->tx_start(config->delay);
        cart->change_quantity(shoes, 4);
        int new_qt_x = cart->get_quantity(shoes);
        cart->tx_end();

        std::cout << "qt x y" << qt_x << " " << qt_y << std::endl;

        assert_count(qt_x == 0 || qt_x == 2, 2);
        assert_count(qt_y == 1 || qt_y == 2, 3);
        if (qt_x == 2)
            assert_count(qt_y == 2, 4);

        assert_count(new_qt_x == 0 || new_qt_x == 4, 5);
    }
    config->scheduler->complete_operation(t_id);
}

/*
 * C1: ADD_ITEM(X, 2) | GET_CART() | GET_CART()
 * C2: ADD_ITEM(X, 3) | GET_CART() | GET_CART()
 */
void do_op1(shopping_cart *cart, int t_id) {
    config->scheduler->start_operation(t_id);
    if (t_id == 1) {
        cart->tx_start(config->delay);
        cart->add_item(shoes, 2, t_id);
        cart->tx_end();

        cart->tx_start(config->delay);
        auto cart_a = cart->get_cart_list(t_id);
        cart->tx_end();

        cart->tx_start(config->delay);
        auto cart_b = cart->get_cart_list(t_id);
        cart->tx_end();

        int qt_x = 0;
        if (cart_a.size() != 0)
            qt_x = cart_a[0].second;

        int qt_x2 = 0;
        if (cart_b.size() != 0)
            qt_x2 = cart_b[0].second;

        results[0] = qt_x;
        results[1] = qt_x2;
    }
    else if (t_id == 2) {
        cart->tx_start(config->delay);
        cart->add_item(shoes, 3, t_id);
        cart->tx_end();

        cart->tx_start(config->delay);
        auto cart_a = cart->get_cart_list(t_id);
        cart->tx_end();

        cart->tx_start(config->delay);
        auto cart_b = cart->get_cart_list(t_id);
        cart->tx_end();

        int qt_x = 0;
        if (cart_a.size() != 0)
            qt_x = cart_a[0].second;

        int qt_x2 = 0;
        if (cart_b.size() != 0)
            qt_x2 = cart_b[0].second;

        results[2] = qt_x;
        results[3] = qt_x2;

    }
    config->scheduler->complete_operation(t_id);
}
/*
 * C1: REMOVE_ITEM(X) | GET_CART() | GET_CART()
 * C2: ADD_ITEM(X, 3) | GET_CART() | GET_CART()
 */
void do_op(shopping_cart *cart, int t_id) {
    config->scheduler->start_operation(t_id);
    if (t_id == 1) {
        cart->tx_start(config->delay);
        cart->remove_item(shoes, t_id);
        cart->tx_end();

        cart->tx_start(config->delay);
        auto cart_a = cart->get_cart_list(t_id);
        cart->tx_end();

        cart->tx_start(config->delay);
        auto cart_b = cart->get_cart_list(t_id);
        cart->tx_end();

        int qt_x = 0;
        if (cart_a.size() != 0)
            qt_x = cart_a[0].second;

        int qt_x2 = 0;
        if (cart_b.size() != 0)
            qt_x2 = cart_b[0].second;

        results[0] = qt_x;
        results[1] = qt_x2;
    }
    else if (t_id == 2) {
        cart->tx_start(config->delay);
        cart->add_item(shoes, 3, t_id);
        cart->tx_end();

        cart->tx_start(config->delay);
        auto cart_a = cart->get_cart_list(t_id);
        cart->tx_end();

        cart->tx_start(config->delay);
        auto cart_b = cart->get_cart_list(t_id);
        cart->tx_end();

        int qt_x = 0;
        if (cart_a.size() != 0)
            qt_x = cart_a[0].second;

        int qt_x2 = 0;
        if (cart_b.size() != 0)
            qt_x2 = cart_b[0].second;

        results[2] = qt_x;
        results[3] = qt_x2;

    }
    config->scheduler->complete_operation(t_id);
}

// Fills operations matrix randomly
void random_fill(long seed) {
    for (int i = 1; i <= NUM_SESSIONS; i++) {
        long thread_seed = seed * 97 + 19;
        std::mt19937 generator (thread_seed);
        operations[i - 1].resize(NUM_OPS);
        for (int j = 1; j <= NUM_OPS; j++) {
            unsigned long long random_value = generator();
            operations[i - 1][j - 1] = random_value % 3;
        }
    }
}

/*
 * 2 threads, 4 operations in each thread
 * Choices of operations:
 * 0 : get_cart()
 * 1 : add_item(i)
 * 2 : remove_item(i)
 */
void do_random_op(shopping_cart *cart, int t_id) {
    config->scheduler->start_operation(t_id);
    int idx = 0;
    for (int j = 1; j <= NUM_OPS; j++) {
        cart->tx_start(config->delay);
        if (operations[t_id - 1][j - 1] == 0) {
            cart->get_cart_list(t_id);
        }
        else if (operations[t_id - 1][j - 1] == 1) {
            cart->add_item(items[idx], t_id);
        }
        else if (operations[t_id - 1][j - 1] == 2) {
            cart->remove_item(items[idx], t_id);
        }
        cart->tx_end();
        idx = (idx + 1) % items.size();
    }
    // Last operation is fixed - a read operation
    cart->tx_start(config->delay);
    cart->get_cart_list(t_id);
    cart->tx_end();

    config->scheduler->complete_operation(t_id);
}

void run_iteration() {
    config->scheduler->attach();
    config->scheduler->create_resource(0);

    nekara::read_response_selector<std::string, web::json::value> *get_next_tx;

    if (config->consistency_level == consistency::causal)
        get_next_tx = new nekara::causal_read_response_selector<std::string, web::json::value>();
    else if (config->consistency_level == consistency::linear)
        get_next_tx = new nekara::linearizable_read_response_selector<std::string, web::json::value>();
    else if (config->consistency_level == consistency::k_causal)
        get_next_tx = new nekara::k_causal_read_response_selector<std::string, web::json::value>(2, 12);

    store = new nekara::kv_store<std::string, web::json::value>(get_next_tx);
    if (config->coyote_enabled)
        get_next_tx->init_consistency_checker(store, config->scheduler);
    else
        get_next_tx->init_consistency_checker(store);

    shopping_cart *cart = new shopping_cart(u, store, config->scheduler,
                                            config->coyote_enabled, config->consistency_level);
    populate_shopping_cart();
    // Required for do_op assertion
    cart->add_item(shoes);

    std::vector<std::thread> threads;

    for (int i = 1; i <= NUM_SESSIONS; i++) {
        config->scheduler->create_operation(i);
        if (config->random_test)
            threads.push_back(std::thread(do_random_op, cart, i));
        else
            threads.push_back(std::thread(do_op, cart, i));
    }

    std::this_thread::sleep_for(std::chrono::milliseconds(5));

    for (int i = 1; i <= threads.size(); i++)
        config->scheduler->join_operation(i);

    for (auto &t : threads)
        t.join();

    delete cart;
    delete store;
    delete get_next_tx;
    config->scheduler->delete_resource(0);
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

    for (int i = 1; i <= config->num_random_test; i++) {
        if (config->random_test) {
            BOOST_LOG_TRIVIAL(info) << "RANDOM " << i - 1 << " start";
            random_fill(i * 19 + 11);
        }
        if (config->coyote_dfs) {
            delete config->scheduler;
            config->scheduler = new coyote::Scheduler("DFSStrategy");
        }
        for(int j = 0; j < config->iterations; j++) {
            BOOST_LOG_TRIVIAL(info) << "Iteration " << j << " start";
            std::chrono::steady_clock::time_point begin = std::chrono::steady_clock::now();
            run_iteration();
            std::chrono::steady_clock::time_point end = std::chrono::steady_clock::now();

            serial_count(results);
            reappear_count(results);

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
        int violation_count = assert_counter[1];

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
