cmake_minimum_required(VERSION 3.10)
project(nekara_kv_store)

set(CMAKE_CXX_STANDARD 14)
set(cpprestsdk_DIR /usr/lib/${CMAKE_LIBRARY_ARCHITECTURE}/cmake/)
find_package(cpprestsdk REQUIRED)

add_library(nekara_kv_store include/http_server.h include/kv_store.h include/transaction.h include/key_not_found_exception.h include/operation_response.h include/operation_param.h include/consistency_checker.h include/read_response_selector.h include/consistency_exception.h include/operation.h src/kv_demo.cpp)
target_link_libraries(nekara_kv_store PRIVATE cpprestsdk::cpprest)