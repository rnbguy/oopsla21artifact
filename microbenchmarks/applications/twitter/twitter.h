// ------------------------------------------------------------
// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
//

#ifndef NEKARA_KEY_VALUE_STORE_TWITTER_H
#define NEKARA_KEY_VALUE_STORE_TWITTER_H

#include "user.h"
#include "../../kv_store/include/kv_store.h"
#include "../../libs/coyote-scheduler/include/coyote/scheduler.h"
#include "../app_config.h"

#include <cpprest/json.h>
#include <thread>

/*
 * Format in which data is stored in the kv-store
    key    : value
    string : json

    "users" : {
                    "list":
                            [
                                xxx,
                                xxx,
                            ]
                    "count": xxx
                 }

    "user:id:name" : {
                        xxx
                     }

    "user:id:following" : {
                        "list":
                                    [
                                        xxx,
                                        xxx,
                                     ]
                        "count": xxx
                        }

    "user:id:followers" : {
                            "list":
                                    [
                                        xxx,
                                        xxx,
                                    ]
                            "count": xxx
                         }

    "user:id:tweets" :  {
                            "list":
                                    [
                                        xxx,
                                        xxx,
                                    ]
                            "count": xxx
                         }

    "tweet:id" : {
                    "content": xxx,
                    "timestamp": xxx,
                    "likes": xxx,
                    "retweets" : xxx
                 }


 */
class twitter {
public:
    twitter(nekara::kv_store<std::string, web::json::value> *store,
            coyote::Scheduler *scheduler, bool coyote_enabled,
            consistency consistency_level);
    void add_user(user u);
    void follow(user a, user b);
    void publish_tweet(user u, tweet t);
    std::vector<tweet> get_newsfeed(user u);
    std::vector<tweet> get_timeline(user u, long session_id = 1);
    std::vector<tweet> get_all_timeline(long session_id = 1);
    std::vector<long> get_following(user u, long session_id = 1);
    std::vector<long> get_followers(user u, long session_id = 1);
    long following_count(user u, long session_id = 1);
    long followers_count(user u, long session_id = 1);

    void tx_start(bool delay);
    void tx_end();
private:
    std::vector<tweet> _get_newsfeed(long user_id);
    std::vector<tweet> _get_timeline(long user_id, long session_id = 1);

    nekara::kv_store<std::string, web::json::value> *store;
    coyote::Scheduler *scheduler;
    std::mutex mtx;
    bool coyote_enabled, lock_used = false;
};

twitter::twitter(nekara::kv_store<std::string, web::json::value> *store,
                 coyote::Scheduler *scheduler, bool coyote_enabled,
                 consistency consistency_level) {
    this->store = store;
    this->scheduler = scheduler;
    this->coyote_enabled = coyote_enabled;
}

void twitter::add_user(user u) {
    // Add user to list
    web::json::value user_list;
    try {
        user_list = store->get("users");
    } catch (std::exception &e) {
        // user_list doesn't exist
        user_list["list"] = web::json::value::array();
        user_list["count"] = web::json::value(0);
    }

    int user_count = user_list["count"].as_integer();
    user_list["list"][user_count] = u.get_id();
    user_list["count"] = web::json::value(user_count + 1);
    store->put("users", user_list);

    // Add user details
    web::json::value user_json;
    user_json["list"] = web::json::value::array();
    user_json["count"] = web::json::value(0);

    store->put("user:" + std::to_string(u.get_id()) + ":name", web::json::value(u.get_username()));
    store->put("user:" + std::to_string(u.get_id()) + ":following", user_json, u.get_id());
    store->put("user:" + std::to_string(u.get_id()) + ":followers", user_json, u.get_id());
    store->put("user:" + std::to_string(u.get_id()) + ":tweets", user_json, u.get_id());
}

void twitter::tx_start(bool delay) {
    if (!coyote_enabled) {
        if (delay)
            std::this_thread::sleep_for(std::chrono::milliseconds(rand() % 4));
        mtx.lock();
        return;
    }
    coyote::ErrorCode e = scheduler->schedule_next();
    assert(e == coyote::ErrorCode::Success);
    while (lock_used) {
        scheduler->wait_resource(0);
    }
    lock_used = true;
}

void twitter::tx_end() {
    if (!coyote_enabled) {
        mtx.unlock();
        return;
    }
    assert(lock_used == true);
    lock_used = false;
    scheduler->signal_resource(0);
}

// user a follows user b
void twitter::follow(user a, user b) {
    // update a's following list
    web::json::value following;
    try {
        following = store->get("user:" + std::to_string(a.get_id()) + ":following", a.get_id());
    } catch (std::exception &e) {
        // user doesn't exist
        return;
    }

    // Check if already following
    for (auto &i : following["list"].as_array()) {
        if (i.as_integer() == b.get_id())
            return;
    }

    int following_count = following["count"].as_integer();
    following["list"][following_count] = b.get_id();
    following["count"] = web::json::value(following_count + 1);
    store->put("user:" + std::to_string(a.get_id()) + ":following", following, a.get_id());

    // update b's followers list
    web::json::value followers;
    try {
        followers = store->get("user:" + std::to_string(b.get_id()) + ":followers", a.get_id());
    } catch (std::exception &e) {
        // user doesn't exist
        return;
    }
    int follower_count = followers["count"].as_integer();
    followers["list"][follower_count] = a.get_id();
    followers["count"] = web::json::value(follower_count + 1);
    store->put("user:" + std::to_string(a.get_id()) + ":followers", followers, a.get_id());
}

void twitter::publish_tweet(user u, tweet t) {
    // add tweet to user u
    web::json::value tweets;
    try {
        tweets = store->get("user:" + std::to_string(u.get_id()) + ":tweets", u.get_id());
    } catch (std::exception &e) {
        // user doesn't exist
        return;
    }
    int tweets_count = tweets["count"].as_integer();
    tweets["list"][tweets_count] = t.get_id();
    tweets["count"] = web::json::value(tweets_count + 1);
    store->put("user:" + std::to_string(u.get_id()) + ":tweets", tweets, u.get_id());


    // add tweet details
    web::json::value tweet;
    tweet["content"] = web::json::value(t.get_tweet());
    tweet["timestamp"] = web::json::value(t.get_timestamp());
    tweet["likes"] = web::json::value(t.get_likes());
    tweet["retweets"] = web::json::value(t.get_retweets());

    store->put("tweet:" + std::to_string(t.get_id()), tweet, u.get_id());
}

std::vector<tweet> twitter::get_newsfeed(user u) {
    return _get_newsfeed(u.get_id());
}

std::vector<tweet> twitter::get_all_timeline(long session_id) {
    std::vector<tweet> res;
    web::json::value user_list;
    try {
        user_list = store->get("users");
    } catch (std::exception &e) {
        return res;
    }

    for (auto &i : user_list["list"].as_array()) {
        std::vector<tweet> tweets = _get_timeline(i.as_integer(), session_id);
        res.insert(res.end(), tweets.begin(), tweets.end());
    }
    return res;
}

std::vector<tweet> twitter::_get_newsfeed(long user_id) {
    std::vector<tweet> timeline;
    std::map<int, std::vector<int>> state_log;

    // Get following list
    web::json::value following;
    try {
        following = store->get("user:" + std::to_string(user_id) + ":following", user_id);
    } catch (std::exception &e) {
        // user doesn't exist
        return timeline;
    }

    // Fetch tweets of following users
    for (auto &i : following["list"].as_array()) {
        std::vector<tweet> tweets_by_user = _get_timeline(i.as_integer(), user_id);
        for (auto t : tweets_by_user) {
            state_log[i.as_integer()].push_back(t.get_id());
        }

        timeline.insert(timeline.end(), tweets_by_user.begin(), tweets_by_user.end());
    }

    // Sort timeline based on timestamp
    std::sort(timeline.begin(), timeline.end(),
              [](const tweet &a, const tweet &b) {
                  return (a.get_timestamp() < b.get_timestamp());
              });

    std::stringstream ss;

    for (auto e : state_log) {
        ss << "(" << e.first << ": ";
        for (int i = 0; i < e.second.size(); i++) {
            ss << e.second[i];
            if (i != e.second.size() - 1)
                ss << " ";
        }
        ss << ") ";
    }

    std::string val = ss.str();
    if (val.size() != 0)
        BOOST_LOG_TRIVIAL(info) << val;
    return timeline;
}

std::vector<tweet> twitter::get_timeline(user u, long session_id) {
    return _get_timeline(u.get_id(), session_id);
}

std::vector<tweet> twitter::_get_timeline(long user_id, long session_id) {
    std::vector<tweet> all_tweets;
    web::json::value tweets;
    try {
        tweets = store->get("user:" + std::to_string(user_id) + ":tweets", session_id);
    } catch (std::exception &e) {
        std::cout << "tweets doesn't exist\n";
        return all_tweets;
    }
    for (auto &t : tweets["list"].as_array()) {
        web::json::value tweet_json;
        try {
            tweet_json = store->get("tweet:" + std::to_string(t.as_integer()), session_id);
        } catch (std::exception &e) {
            std::cout << "tweet doesn't exist " << t << std::endl;
            continue;
        }
        tweet tweet_obj(t.as_integer(), tweet_json["content"].as_string(), tweet_json["timestamp"].as_integer());
        tweet_obj.set_likes(tweet_json["likes"].as_integer());
        tweet_obj.set_retweets(tweet_json["retweets"].as_integer());
        all_tweets.push_back(tweet_obj);
    }

    // Sort tweets based on timestamp
    std::sort(all_tweets.begin(), all_tweets.end(),
              [](const tweet &a, const tweet &b) {
                  return (a.get_timestamp() < b.get_timestamp());
              });

    // Log tweets
    std::stringstream ss;
    ss << "(" << user_id << ": ";
    for (int i = 0; i < all_tweets.size(); i++) {
        ss << all_tweets[i].get_id();
        if (i != all_tweets.size() - 1)
            ss << " ";
    }
    ss << ") ";

    std::string val = ss.str();
    if (val.size() != 0)
        BOOST_LOG_TRIVIAL(info) << val;

    return all_tweets;
}

std::vector<long> twitter::get_following(user u, long session_id) {

}
std::vector<long> twitter::get_followers(user u, long session_id) {

}

long twitter::following_count(user u, long session_id) {

}

long twitter::followers_count(user u, long session_id) {

}
#endif //NEKARA_KEY_VALUE_STORE_TWITTER_H
