cmake_minimum_required(VERSION 3.10)
project(nekara_key_value_store)

set(CMAKE_CXX_STANDARD 14)

#set(BOOST_ADDITIONAL_VERSIONS 1.69.0 1.69)
ADD_DEFINITIONS(-DBOOST_LOG_DYN_LINK)
set(BOOST_USE_STATIC_LIBS ON)
set(BOOST_USE_MULTITHREADED ON)
set(BOOST_NO_SYSTEM_PATHS ON)
set(BOOST_NO_BOOST_CMAKE ON)
set(BOOST_ROOT /usr/include/boost/)
set(BOOST_INCLUDEDIR /usr/include/boost/)
set(BOOST_LIBRARYDIR /usr/lib/x86_64-linux-gnu) # Ubuntu
#set(BOOST_LIBRARYDIR /usr/lib64/) # Fedora

find_package(Boost COMPONENTS log REQUIRED)

add_subdirectory(libs/coyote-scheduler)

add_executable(nekara_key_value_store main.cpp)

target_link_libraries(nekara_key_value_store nekara_kv_store Boost::log coyote)

add_subdirectory(kv_store)
add_subdirectory(tests)
add_subdirectory(applications)
