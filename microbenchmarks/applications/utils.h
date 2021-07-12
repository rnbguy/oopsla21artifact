// ------------------------------------------------------------
// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
//

#ifndef NEKARA_KEY_VALUE_STORE_APP_UTILS_H
#define NEKARA_KEY_VALUE_STORE_APP_UTILS_H

#include "app_config.h"

#include <string>
#include <boost/log/trivial.hpp>
#include <boost/log/utility/setup.hpp>
#include <boost/log/utility/setup/file.hpp>
#include <boost/log/expressions.hpp>

namespace logging = boost::log;
namespace keywords = boost::log::keywords;

void init_logging(std::string filename);
app_config *parse_command_line(int argc, char **argv);

#endif //NEKARA_KEY_VALUE_STORE_APP_UTILS_H
