// ------------------------------------------------------------
// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
//
// Selects one read response among multiple valid responses using consistency specification.

#ifndef NEKARA_KEY_VALUE_STORE_READ_RESPONSE_SELECTOR_H
#define NEKARA_KEY_VALUE_STORE_READ_RESPONSE_SELECTOR_H


#include "kv_store.h"
#include "consistency_checker.h"
#include "../../libs/coyote-scheduler/include/coyote/scheduler.h"

#include <list>
#include <set>
#include <unordered_map>
#include <algorithm>
#include <random>

namespace nekara {
    template<typename K, typename V>
    class read_response_selector {
    public:
        read_response_selector() {
            this->store = nullptr;
        }

        virtual ~read_response_selector() {

        }

        virtual void init_consistency_checker(const kv_store<K, V> *store,
                                              coyote::Scheduler *scheduler = nullptr) = 0;

        // By default it picks one transaction response at random
        virtual GET_response<K, V> *select_read_response(transaction<K, V> *tx,
                                                         GET_operation<K, V> *op,
                                                         std::vector<GET_response<K, V> *> candidates) {
            int idx = std::rand() % candidates.size();
            return candidates[idx];
        }

    protected:
        const kv_store<K, V> *store;
        coyote::Scheduler *scheduler;
    };

    template<typename K, typename V>
    class causal_read_response_selector : public read_response_selector<K, V> {
    public:
        causal_read_response_selector() {
            this->checker = nullptr;
        }

        ~causal_read_response_selector() {
            delete this->checker;
        }

        void init_consistency_checker(const kv_store<K, V> *store,
                                      coyote::Scheduler *scheduler = nullptr) {
            this->store = store;
            this->scheduler = scheduler;
            this->checker = new causal_consistency_checker<K, V>(this->store);
        }

        GET_response<K, V> *select_read_response(transaction<K, V> *tx,
                                                 GET_operation<K, V> *op,
                                                 std::vector<GET_response<K, V> *> candidates) {

            if (this->scheduler != nullptr && this->scheduler->get_strategy() == "DFSStrategy") {
                return dfs_response_selector(tx, op, candidates);
            }

            int limit = candidates.size();

            std::random_device rd; // obtain a random number from hardware
            std::mt19937 gen(rd()); // seed the generator

            while (limit > 0) {
                int index = -1;
                // Choose one candidate randomly
                if (this->scheduler == nullptr) {
                    std::uniform_int_distribution<> dist(0, limit - 1);
                    index = dist(gen);
                } else
                    index = this->scheduler->next_integer(limit);

                // Swap with the last element to not pick the same element again
                swap(candidates[index], candidates[limit - 1]);
                GET_response<K, V> *candidate = candidates[limit - 1];
                limit--;

                op->set_response(candidate);
                if (checker->is_consistent(tx)) {
                    return candidate;
                }
            }

            // No consistent transaction response possible
            op->set_response(nullptr);
            throw consistency_exception("GET", tx->get_tx_id());
        }

    private:
        consistency_checker<K, V> *checker;

        /*
         * Special case for Coyote DFS scheduler which firsts checks consistency
         * and then does random selection.
         */
        GET_response<K, V> *dfs_response_selector(transaction<K, V> *tx,
                                                  GET_operation<K, V> *op,
                                                  std::vector<GET_response<K, V> *> candidates) {

            std::vector<GET_response<K, V> *> valid_candidates;
            for (auto candidate : candidates) {
                op->set_response(candidate);
                if (checker->is_consistent(tx)) {
                    valid_candidates.push_back(candidate);
                }
            }

            if (valid_candidates.size() == 0) {
                // No consistent transaction response possible
                op->set_response(nullptr);
                throw consistency_exception("GET", tx->get_tx_id());
            }

            int index = this->scheduler->next_integer(valid_candidates.size());
            return valid_candidates[index];
        }
    };

    template<typename K, typename V>
    class linearizable_read_response_selector : public read_response_selector<K, V> {
    public:
        linearizable_read_response_selector() {
        }

        ~linearizable_read_response_selector() {
        }

        void init_consistency_checker(const kv_store<K, V> *store,
                                      coyote::Scheduler *scheduler = nullptr) {
            this->store = store;
            this->scheduler = scheduler;
        }

        GET_response<K, V> *select_read_response(transaction<K, V> *tx,
                                                 GET_operation<K, V> *op,
                                                 std::vector<GET_response<K, V> *> candidates) {
            if (candidates.empty()) {
                // No consistent transaction response possible
                op->set_response(nullptr);
                throw consistency_exception("GET", tx->get_tx_id());
            }
            op->set_response(candidates.back());
            return candidates.back();
        }
    };

    // k-causal : at most k times weaker than linearizable
    template<typename K, typename V>
    class k_causal_read_response_selector : public read_response_selector<K, V> {
    public:
        k_causal_read_response_selector(int k, int total_read_count) {
            this->k = k;
            this->total_read_count = total_read_count;
            this->causal_selector = new causal_read_response_selector<K, V>();
            this->linearizable_selector = new linearizable_read_response_selector<K, V>();
        }

        ~k_causal_read_response_selector() {
            delete this->causal_selector;
            delete this->linearizable_selector;
        }

        void init_consistency_checker(const kv_store<K, V> *store,
                                      coyote::Scheduler *scheduler = nullptr) {
            this->scheduler = scheduler;
            this->causal_selector->init_consistency_checker(store, scheduler);
            this->linearizable_selector->init_consistency_checker(store, scheduler);
            // Pick k causal reads
            pick_k_read_ids();
        }

        GET_response<K, V> *select_read_response(transaction<K, V> *tx,
                                                 GET_operation<K, V> *op,
                                                 std::vector<GET_response<K, V> *> candidates) {

            this->read_count++;
            if (this->k_read_ids.find(this->read_count) != this->k_read_ids.end()) {
                return this->causal_selector->select_read_response(tx, op, candidates);
            }
            return this->linearizable_selector->select_read_response(tx, op, candidates);
        }

    private:
        int k, total_read_count;
        int read_count = 0;
        std::unordered_set<int> k_read_ids;
        causal_read_response_selector<K, V> *causal_selector;
        linearizable_read_response_selector<K, V> *linearizable_selector;

        void pick_k_read_ids() {
            if (this->scheduler != nullptr) {
                pick_k_read_ids_coyote();
                return;
            }

            std::vector<int> read_ids(this->total_read_count);
            std::iota(read_ids.begin(), read_ids.end(), 1);
            std::shuffle(read_ids.begin(), read_ids.end(), std::mt19937{std::random_device{}()});
            k_read_ids.insert(read_ids.begin(), read_ids.begin() + this->k);
        }


        /*
         * NOTE: This is not ideal way to implement it, but currently coyote dfs always generates
         * same random number when called multiple times. This implementation avoids getting
         * stuck in infinite loop.
         */
        void pick_k_read_ids_coyote() {
            for (int i = 1; i <= this->total_read_count; i++) {
                bool choose = this->scheduler->next_boolean();
                if (choose) {
                    this->k_read_ids.insert(i);
                }
            }
        }
    };
}
#endif //NEKARA_KEY_VALUE_STORE_READ_RESPONSE_SELECTOR_H
