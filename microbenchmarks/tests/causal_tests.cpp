// ------------------------------------------------------------
// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
//

#include "gtest/gtest.h"
#include "kv_store.h"
#include "read_response_selector.h"

class causal_tests : public ::testing::Test {

protected:
    // Called once before each test
    virtual void SetUp()
    {
        read_selector = new nekara::causal_read_response_selector<std::string, int>();
        store = new nekara::kv_store<std::string, int>(read_selector);
        read_selector->init_consistency_checker(store);
    }

    virtual void TearDown() {
        delete read_selector;
        delete store;
    }

    // Called only once for all tests
    static void SetUpTestSuite() {
    }

    static void TearDownTestSuite() {
    }

    nekara::kv_store<std::string, int> *store;
    nekara::read_response_selector<std::string, int> *read_selector;
};

TEST_F(causal_tests, simple_read_write) {
    int session_id = 123;
    store->put("a", 50, session_id);
    ASSERT_EQ(store->get("a", session_id), 50);

    store->put("b", 100, session_id);
    store->put("c", 150, session_id);
    ASSERT_EQ(store->get("b", session_id), 100);
    ASSERT_EQ(store->get("c", session_id), 150);
}

/*
 * C1: W(x)a
 * C2:         R(x)a   W(x)b
 * C3:                         R(x)b   R(x)a   NOT OK
 * C4:                         R(x)a   R(x)b   OK
 */
TEST_F(causal_tests, causal_read_write) {
    int c1 = 1, c2 = 2, c3 = 3, c4 = 4;
    int a = 5, b = 10;

    store->put("x", a, c1);
    int c2_rx1 = store->get("x", c2);
    store->put("x", b, c2);
    int c3_rx1 = store->get("x", c3);
    int c4_rx1 = store->get("x", c4);
    int c3_rx2 = store->get("x", c3);
    int c4_rx2 = store->get("x", c4);

    ASSERT_EQ(c2_rx1, a);

    std::cout << c3_rx1 << " " << c3_rx2 << std::endl;
    std::cout << c4_rx1 << " " << c4_rx2 << std::endl;

    if (c3_rx1 == b)
        ASSERT_TRUE(c3_rx2 != a);
    if (c4_rx1 == b)
        ASSERT_TRUE(c4_rx2 != a);
}
