// ------------------------------------------------------------
// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
//

#include "utils.h"
#include "../libs/coyote-scheduler/include/coyote/dummy_scheduler.h"

void init_logging(std::string filename) {
    logging::register_simple_formatter_factory<logging::trivial::severity_level, char>("Severity");
    logging::add_file_log(keywords::file_name = filename,
                          keywords::format = "[%TimeStamp%] [%ThreadID%] [%Severity%] %Message%",
                          keywords::auto_flush = true);

    logging::core::get()->set_filter(logging::trivial::severity >= logging::trivial::info);
    logging::add_common_attributes();
}

app_config *parse_command_line(int argc, char **argv) {
    app_config *config      = new app_config();
    config->log_file_name   = argv[1];
    config->iterations      = atoi(argv[2]);
    char *consistency_arg   = argv[3];
    char *random_arg        = argv[4];
    config->num_random_test = atoi(argv[5]);
    config->delay           = atoi(argv[6]);
    char *scheduler_type    = nullptr;

    if (strcmp(random_arg, "random") == 0) {
//        std::cout << "Random test cases\n";
        config->random_test = true;
    }
    else if (strcmp(random_arg, "fixed") == 0) {
//        std::cout << "Fixed test case\n";
        config->random_test = false;
    }

    if (argc > 7)
        scheduler_type = argv[7];

    if (strcmp(consistency_arg, "linear") == 0) {
//        std::cout << "Serializability consistency\n";
        config->consistency_level = consistency::linear;
    }
    else if (strcmp(consistency_arg, "causal") == 0) {
//        std::cout << "Causal consistency\n";
        config->consistency_level = consistency::causal;
    }
    else if (strcmp(consistency_arg, "k-causal") == 0) {
//        std::cout << "k-causal consistency\n";
        config->consistency_level = consistency::k_causal;
    }

    if (scheduler_type != nullptr && std::strcmp(scheduler_type, "coyote") == 0) {
        config->scheduler = new coyote::Scheduler();
//        std::cout << "Coyote scheduler\n";
        config->coyote_enabled = true;
    }
    else if (scheduler_type != nullptr && std::strcmp(scheduler_type, "coyote_dfs") == 0) {
        config->scheduler = new coyote::Scheduler("DFSStrategy");
//        std::cout << "Coyote DFS scheduler\n";
        config->coyote_enabled = true;
        config->coyote_dfs = true;
    }
    else {
        config->scheduler = new dummy_scheduler();
        config->coyote_enabled = false;
    }
    assert(config->scheduler != NULL && "coyote::Scheduler() returned NULL!");

    return config;
}
