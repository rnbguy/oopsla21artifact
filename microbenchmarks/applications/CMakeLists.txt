cmake_minimum_required(VERSION 3.10)
project(kv_store_apps)

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
#set(BOOST_LIBRARYDIR /usr/lib64/)  # Fedora


find_package(Boost COMPONENTS log REQUIRED)
set (CMAKE_CXX_FLAGS "-g")

# courseware
add_executable(courseware_app courseware/run_courseware.cpp ../libs/coyote-scheduler/include/coyote/dummy_scheduler.h utils.h utils.cpp app_config.h courseware/course.h courseware/student.h courseware/courseware.h)

# shopping_cart
add_executable(shopping_cart_app shopping_cart/run_shopping_cart.cpp ../libs/coyote-scheduler/include/coyote/dummy_scheduler.h utils.h utils.cpp app_config.h shopping_cart/shopping_cart.h shopping_cart/item.h shopping_cart/user.h)

# twitter
add_executable(twitter_app twitter/run_twitter.cpp ../libs/coyote-scheduler/include/coyote/dummy_scheduler.h utils.h utils.cpp app_config.h twitter/user.h twitter/twitter.h twitter/tweet.h)

# treiber_stack
add_executable(stack_app treiber_stack/run_stack.cpp ../libs/coyote-scheduler/include/coyote/dummy_scheduler.h utils.h utils.cpp app_config.h treiber_stack/treiber_stack.h)

target_link_libraries(courseware_app nekara_kv_store Boost::log coyote)
target_link_libraries(shopping_cart_app nekara_kv_store Boost::log coyote)
target_link_libraries(twitter_app nekara_kv_store Boost::log coyote)
target_link_libraries(stack_app nekara_kv_store Boost::log coyote)
