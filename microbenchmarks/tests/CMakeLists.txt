project(tests)
add_subdirectory(lib)
include_directories(${gtest_SOURCE_DIR}/include ${gtest_SOURCE_DIR})
include_directories(${nekara_kv_store_SOURCE_DIR}/include ${gtest_SOURCE_DIR})

add_executable(tests_run causal_tests.cpp linearizable_tests.cpp)

target_link_libraries(tests_run nekara_kv_store)

target_link_libraries(tests_run gtest gtest_main)