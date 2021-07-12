// ------------------------------------------------------------
// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
//

#ifndef NEKARA_KEY_VALUE_STORE_KV_STORE_H
#define NEKARA_KEY_VALUE_STORE_KV_STORE_H

#define DEFAULT_SESSION 1

#include "transaction.h"
#include "key_not_found_exception.h"
#include "consistency_exception.h"

#include <list>
#include <vector>
#include <mutex>
#include <unordered_map>
#include <iostream>
#include <boost/log/trivial.hpp>
#include <boost/log/utility/setup.hpp>

namespace nekara {
    // Forward declaration of class
    template<typename K, typename V>
    class read_response_selector;

    template <typename K, typename V>
    class kv_store {
    public:
        kv_store(read_response_selector<K, V> *read_selector);
        V get(const K &key, long session_id = DEFAULT_SESSION);
        int put(const K &key, const V &value, long session_id = DEFAULT_SESSION);
        V remove(const K &key, long session_id = DEFAULT_SESSION);
        size_t get_size() const;
        ~kv_store();

        const read_response_selector<K, V> *get_gen_next_tx() const;
        void set_gen_next_tx(read_response_selector<K, V> *gen_next_tx);

        const std::list<transaction<K, V>*> get_session_history(long session_id) const;
        const std::list<transaction<K, V>*> get_history() const;

    private:
        void commit_tx(transaction<K, V> *tx, long session_id);

        std::unordered_map<K, std::list<std::pair<V, long>>> kv_map;
        std::list<transaction<K, V>*> history;
        std::unordered_map<long, std::list<transaction<K, V>*>> session_order;
        std::mutex mtx;
        read_response_selector<K, V> *read_selector;
        void log_stack();
    };

}

template <typename K, typename V>
nekara::kv_store<K, V>::kv_store(read_response_selector<K, V> *get_next_tx) {
    this->read_selector = get_next_tx;
}

// Destructor
template <typename K, typename V>
nekara::kv_store<K, V>::~kv_store() {
    for (auto tx : this->history) {
        delete tx;
    }
}

/*
 * GET operation.
 * May throw key_not_found_exception.
 */
template <typename K, typename V>
V nekara::kv_store<K, V>::get(const K &key, long session_id) {
    // Create GET operation and transaction
    GET_param<K, V> *params = new GET_param<K, V>(key);
    GET_operation<K, V> *op = new GET_operation<K, V>(params);
    transaction<K, V> *tx = new transaction<K, V>(op);
    tx->set_session_id(session_id);

    // Acquire the lock and enter critical section.
    this->mtx.lock();
    tx->start_transaction();

    // Throw exception if key doesn't exist
    if (this->kv_map.find(key) == this->kv_map.end()) {
//        BOOST_LOG_TRIVIAL(error) << "TXN " << tx->get_tx_id() << " GET " << key
//                                 << " NOTFOUND " << session_id;
        this->mtx.unlock();
        delete tx;
        std::stringstream ss;
        ss << key;
        throw key_not_found_exception(ss.str());
    }

    // List down candidate responses using all possible versions present in the store
    // corresponding to the given key
    std::vector<GET_response<K, V>*> candidate_responses;
    for (auto candidate_value : this->kv_map[key]) {
        GET_response<K, V> *op_response = new GET_response<K, V>(key, candidate_value.first);
        op_response->set_written_by_tx_id(candidate_value.second);
        candidate_responses.push_back(op_response);
    }

    GET_response<K, V> *op_response;
    try {
        // Choose one transaction response among the candidates by some strategy
        op_response = read_selector->select_read_response(tx, op, candidate_responses);
    } catch (consistency_exception &e) {
        // No consistent response possible
//        BOOST_LOG_TRIVIAL(error) << "TXN " << tx->get_tx_id() << " GET " << key
//                                 << " INCONSISTENT " << session_id;
        this->mtx.unlock();
        delete tx;
        for (auto c : candidate_responses)
            delete c;
        std::stringstream ss;
        ss << "GET(" << key << ")";
        throw consistency_exception(ss.str(), tx->get_tx_id());
    }

    op->set_response(op_response);
    tx->end_transaction();
    this->commit_tx(tx, session_id);

//    BOOST_LOG_TRIVIAL(info) << "TXN " << tx->get_tx_id() << " GET " << key
//                            << " " << op_response->get_value() << " " << session_id;

    // Done with critical section, release the lock.
    this->mtx.unlock();

    // Free allocated memory
    for (auto candidate : candidate_responses) {
        if (candidate != op_response)
            delete candidate;
    }
    return op_response->get_value();
}

/*
 * PUT operation.
 */
template <typename K, typename V>
int nekara::kv_store<K, V>::put(const K &key, const V &value, long session_id) {
    // Create PUT operation and transaction
    PUT_param<K, V> *params = new PUT_param<K, V>(key, value);
    PUT_operation<K, V> *op = new PUT_operation<K, V>(params);
    transaction<K, V> *tx = new transaction<K, V>(op);
    tx->set_session_id(session_id);

    // Acquire the lock and enter critical section.
    this->mtx.lock();
    tx->start_transaction();

    if (this->kv_map.find(key) == this->kv_map.end()) {
        std::list<std::pair<V, long>> l({{value, tx->get_tx_id()}});
        this->kv_map[key] = l;
    }
    else
        this->kv_map[key].push_back({value, tx->get_tx_id()});

    tx->end_transaction();

    // Record response of transaction
    PUT_response<K, V> *op_response = new PUT_response<K, V>(true);
    op->set_response(op_response);

    this->commit_tx(tx, session_id);

//    BOOST_LOG_TRIVIAL(info) << "TXN " << tx->get_tx_id() << " PUT " << key
//                            << " " << value << " " << session_id;

    // Done with critical section, release the lock.
    this->mtx.unlock();
    return 1;
}

/*
 * REMOVE operation.
 * May throw key_not_found_exception.
 */
template <typename K, typename V>
V nekara::kv_store<K, V>::remove(const K &key, long session_id) {
    // TODO
}

template <typename K, typename V>
void nekara::kv_store<K, V>::commit_tx(transaction<K, V> *tx, long session_id) {
    // Insert transaction in history
    this->history.push_back(tx);
    if (this->session_order.find(session_id) == this->session_order.end())
        this->session_order[session_id] = std::list<transaction<K, V>*>({tx});
    else
        this->session_order[session_id].push_back(tx);
}

template<typename K, typename V>
const nekara::read_response_selector<K, V> *nekara::kv_store<K, V>::get_gen_next_tx() const {
    return read_selector;
}

template<typename K, typename V>
size_t nekara::kv_store<K, V>::get_size() const {
    return this->kv_map.size();
}

template<typename K, typename V>
void nekara::kv_store<K, V>::set_gen_next_tx(nekara::read_response_selector<K, V> *get_next_tx) {
    this->read_selector = get_next_tx;
}

template<typename K, typename V>
const std::list<nekara::transaction<K, V>*> nekara::kv_store<K, V>::get_history() const {
    return this->history;
}

template<typename K, typename V>
const std::list<nekara::transaction<K, V>*> nekara::kv_store<K, V>::get_session_history(long session_id) const {
    if (this->session_order.find(session_id) == this->session_order.end())
        return std::list<transaction<K, V>*>();
    return this->session_order.at(session_id);
}

#endif //NEKARA_KEY_VALUE_STORE_KV_STORE_H
