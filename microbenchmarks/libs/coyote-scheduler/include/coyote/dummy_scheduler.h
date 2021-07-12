// ------------------------------------------------------------
// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
//

#ifndef NEKARA_KEY_VALUE_STORE_DUMMY_SCHEDULER_H
#define NEKARA_KEY_VALUE_STORE_DUMMY_SCHEDULER_H

#include <cstddef>
#include "scheduler.h"

/*
 * Dummy coyote scheduler which does nop operations for every call.
 */
class dummy_scheduler : public coyote::Scheduler {
public:
    dummy_scheduler() {

    }

    // Attaches to the scheduler. This should be called at the beginning of a testing iteration.
    // It creates a main operation with id '0'.
    virtual coyote::ErrorCode attach() noexcept {
       return coyote::ErrorCode::Success;
    }

    // Detaches from the scheduler. This should be called at the end of a testing iteration.
    // It completes the main operation with id '0' and releases all controlled operations.
    virtual coyote::ErrorCode detach() noexcept {
        return coyote::ErrorCode::Success;
    }

    // Creates a new operation with the specified id.
    virtual coyote::ErrorCode create_operation(size_t operation_id) noexcept {
        return coyote::ErrorCode::Success;
    }

    // Starts executing the operation with the specified id.
    virtual coyote::ErrorCode start_operation(size_t operation_id) noexcept {
        return coyote::ErrorCode::Success;
    }

    // Waits until the operation with the specified id has completed.
    virtual coyote::ErrorCode join_operation(size_t operation_id) noexcept {
        return coyote::ErrorCode::Success;
    }

    // Waits until the operations with the specified ids have completed.
    virtual coyote::ErrorCode join_operations(const size_t* operation_ids, size_t size, bool wait_all) noexcept {
        return coyote::ErrorCode::Success;
    }

    // Completes executing the operation with the specified id and schedules the next operation.
    virtual coyote::ErrorCode complete_operation(size_t operation_id) noexcept {
        return coyote::ErrorCode::Success;
    }

    // Creates a new resource with the specified id.
    virtual coyote::ErrorCode create_resource(size_t resource_id) noexcept {
        return coyote::ErrorCode::Success;
    }

    // Waits the resource with the specified id to become available and schedules the next operation.
    virtual coyote::ErrorCode wait_resource(size_t resource_id) noexcept {
        return coyote::ErrorCode::Success;
    }

    // Waits the resources with the specified ids to become available and schedules the next operation.
    virtual coyote::ErrorCode wait_resources(const size_t* resource_ids, size_t size, bool wait_all) noexcept {
        return coyote::ErrorCode::Success;
    }

    // Signals all waiting operations that the resource with the specified id is available.
    virtual coyote::ErrorCode signal_resource(size_t resource_id) noexcept {
        return coyote::ErrorCode::Success;
    }

    // Signals the waiting operation that the resource with the specified id is available.
    virtual coyote::ErrorCode signal_resource(size_t resource_id, size_t operation_id) noexcept {
        return coyote::ErrorCode::Success;
    }

    // Deletes the resource with the specified id.
    virtual coyote::ErrorCode delete_resource(size_t resource_id) noexcept {
        return coyote::ErrorCode::Success;
    }

    // Schedules the next operation, which can include the currently executing operation.
    // Only operations that are not blocked nor completed can be scheduled.
    virtual coyote::ErrorCode schedule_next() noexcept {
        return coyote::ErrorCode::Success;
    }

    // Returns the last error code, if there is one assigned.
    virtual coyote::ErrorCode error_code() noexcept {
        return coyote::ErrorCode::Success;
    }

    virtual std::string get_strategy() noexcept {
        return "";
    }
    virtual bool completely_explored() noexcept {
        return false;
    }

    virtual size_t seed() noexcept {
        return 0;
    }

};

#endif //NEKARA_KEY_VALUE_STORE_DUMMY_SCHEDULER_H
