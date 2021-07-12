// ------------------------------------------------------------
// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
//

#ifndef NEKARA_KEY_VALUE_STORE_TREIBER_STACK_H
#define NEKARA_KEY_VALUE_STORE_TREIBER_STACK_H

#define EMPTY 0

#include "../../kv_store/include/kv_store.h"
#include "../../libs/coyote-scheduler/include/coyote/scheduler.h"

#include <assert.h>
#include <thread>

/*
 * Stack on top of key-value store.
 * <id, (val, next_id)>
 * <0, (val, next_id)> next_id contains the id of the head of the stack
 */
template <typename V>
class treiber_stack {
public:
    nekara::kv_store<long, std::pair<V, long>> *store;

    /*
     * Does push operation by:
     * 1. Generate unique id for new node and insert with dummy next id.
     * 2. Get the current head and replace it with this node, try until successful.
     */
    void push(V val, bool delay, long session_id = 1) {
        if (!coyote_enabled && delay)
            std::this_thread::sleep_for(std::chrono::milliseconds(rand() % 4));
        coyote::ErrorCode e = scheduler->schedule_next();
        assert(e == coyote::ErrorCode::Success);
        // Insert node with dummy next pointer
        long new_head_id = store->get_size() * 10 + session_id;
        store->put(new_head_id, {val, -1});

        while (true) {
            long cur_head_id = -1;
            try {
                cur_head_id = store->get(0, session_id).second;
                // Update next of already existing key
                store->put(new_head_id, {val, cur_head_id});
            } catch (std::exception &e) {
                // Pass
            }
            if (update_head(cur_head_id, new_head_id, session_id))
                break;
        }
    }


    V pop(bool delay, long session_id = 1) {
        if (!coyote_enabled && delay)
            std::this_thread::sleep_for(std::chrono::milliseconds(rand() % 4));
        coyote::ErrorCode e = scheduler->schedule_next();
        assert(e == coyote::ErrorCode::Success);
        while (true) {
            long cur_head_id = store->get(0, session_id).second;

            if (cur_head_id == -1) {
                BOOST_LOG_TRIVIAL(info) << "POP " << EMPTY << " " << session_id;
                return EMPTY;
            }

            std::pair<V, long> head;
            try {
                head = store->get(cur_head_id, session_id);
            } catch (std::exception &e) {
                BOOST_LOG_TRIVIAL(info) << "POP " << EMPTY << " " << session_id;
                return EMPTY;
            }

            V val = head.first;

            // Optional delete the current head from store
            if (update_head(cur_head_id, head.second, session_id)) {
                BOOST_LOG_TRIVIAL(info) << "POP " << val << " " << session_id;
                return val;
            }
        }
    }

    treiber_stack(nekara::kv_store<long, std::pair<V, long>> *store,
                  coyote::Scheduler *scheduler,
                  bool coyote_enabled) {
        this->store = store;
        this->scheduler = scheduler;
        this->coyote_enabled = coyote_enabled;
        this->lock_used = false;
        // Insert the initial dummy value to point to head
        this->store->put(0, {NULL, -1});
    }

private:

    coyote::Scheduler *scheduler;
    std::mutex mtx; // Mutex for changing head of the stack
    bool coyote_enabled;
    bool lock_used;

    // Try updating head
    // NOTE: mutex not required for coyote, as coyote atomically runs all blocks between schedule next
    bool update_head(long old_head, long new_head, long session_id = 1) {
        if (!coyote_enabled)
            mtx.lock();
        long head_id = -1;
        try {
            head_id = store->get(0, session_id).second;
        } catch (std::exception &e) {
            // Pass
        }

        if (head_id == old_head) {
            store->put(0, {NULL, new_head}, session_id);
            if (!coyote_enabled)
                mtx.unlock();
            return true;
        }
        if (!coyote_enabled)
            mtx.unlock();
        return false;
    }
};

#endif //NEKARA_KEY_VALUE_STORE_TREIBER_STACK_H
