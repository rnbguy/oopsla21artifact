// ------------------------------------------------------------
// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
//

#ifndef NEKARA_KEY_VALUE_STORE_APP_CONFIG_H
#define NEKARA_KEY_VALUE_STORE_APP_CONFIG_H

#include "../libs/coyote-scheduler/include/coyote/scheduler.h"

enum consistency {linear, causal, k_causal};

struct app_config {
    char *log_file_name;
    int iterations;
    consistency consistency_level = consistency::linear;
    bool random_test = false;
    int num_random_test = 1;
    coyote::Scheduler *scheduler = nullptr;
    bool delay = true;
    bool coyote_enabled = false;
    bool coyote_dfs = false;

    ~app_config(){
        // Delete coyote scheduler
        delete scheduler;
        scheduler = nullptr;
    }
};
#endif //NEKARA_KEY_VALUE_STORE_APP_CONFIG_H
