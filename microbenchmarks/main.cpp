// ------------------------------------------------------------
// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
//

#include "kv_store/include/http_server.h"

#include <iostream>
#include <cpprest/http_listener.h>
#include <boost/log/trivial.hpp>
#include <boost/log/utility/setup.hpp>
#include <boost/log/utility/setup/file.hpp>
#include <boost/log/expressions.hpp>

namespace logging = boost::log;
namespace keywords = boost::log::keywords;

void init_logging(std::string filename) {
    logging::register_simple_formatter_factory<logging::trivial::severity_level, char>("Severity");
    logging::add_file_log(keywords::file_name = filename,
                          keywords::format = "[%TimeStamp%] [%ThreadID%] [%Severity%] %Message%",
                          keywords::auto_flush = true);

    logging::core::get()->set_filter(logging::trivial::severity >= logging::trivial::debug);
    logging::add_common_attributes();
}

/*
 * Initializes the HTTP server on the given address.
 */
std::unique_ptr<nekara::http_server<std::string, web::json::value>> init_server(const utility::string_t& address) {
    web::uri_builder uri(address);
    utility::string_t addr = uri.to_uri().to_string();
    std::unique_ptr<nekara::http_server<std::string, web::json::value>> server = std::unique_ptr<nekara::http_server<std::string, web::json::value>>(
                                                    new nekara::http_server<std::string, web::json::value>(addr));
    server->open().wait();
    BOOST_LOG_TRIVIAL(debug) << "Started HTTP server at " << addr;
    std::cout << "Started Listening at: " <<   addr << std::endl;
    return std::move(server);
}

/*
 * Shut down the server.
 */
void close_server(std::unique_ptr<nekara::http_server<std::string, web::json::value>> &server) {
    if (server != nullptr)
        server->close().wait();
}

/*
 * Starts HTTP server.
 * Arguments:
 * Port number
 * Log file path
 */
int main(int argc, char* argv[]) {
    init_logging(argv[2]);
    utility::string_t port = U(argv[1]);
    utility::string_t address = U("http://127.0.0.1:");
    address.append(port);
    std::unique_ptr<nekara::http_server<std::string, web::json::value>> server = init_server(address);

    while (true) {
        std::cout << "Type exit to stop" << std::endl;
        sleep(10);
        std::string line;
        std::getline(std::cin, line);
        if (line.compare("exit") == 0) {
            close_server(server);
            return 0;
        }
    }
}
