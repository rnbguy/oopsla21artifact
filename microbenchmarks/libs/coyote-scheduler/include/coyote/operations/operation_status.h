// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

#ifndef COYOTE_OPERATION_STATUS_H
#define COYOTE_OPERATION_STATUS_H

namespace coyote
{
    enum class OperationStatus
    {
        None = 0,
        Enabled,
        JoinAnyOperations,
        JoinAllOperations,
        WaitAnyResource,
        WaitAllResources,
        Completed
    };
}

#endif // COYOTE_OPERATION_STATUS_H
