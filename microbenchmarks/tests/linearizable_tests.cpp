// ------------------------------------------------------------
// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
//

#include "gtest/gtest.h"
#include "kv_store.h"
#include "read_response_selector.h"

class linearizable_tests : public ::testing::TestWithParam<int> {

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
        std::cout << "Linearizable: " << linearizable_count << " Total: " << test_count << std::endl;
        std::cout << "Percentage: " << (linearizable_count + 0.0) / test_count * 100.0 << std::endl;
    }

    nekara::kv_store<std::string, int> *store;
    nekara::read_response_selector<std::string, int> *read_selector;
    static long test_count, linearizable_count;
};

long linearizable_tests::test_count(0);
long linearizable_tests::linearizable_count(0);

/*
 * C1: W(x)a
 * C2:         R(x)a   W(x)b
 * C3:                         R(x)b   R(x)b   OK
 * C4:                         R(x)b   R(x)b   OK
 */
TEST_P(linearizable_tests, test_linearizability) {
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

    if (c3_rx1 == b && c3_rx2 == b && c4_rx1 == b && c4_rx2 == b)
        this->linearizable_count++;

    this->test_count++;

}

INSTANTIATE_TEST_CASE_P(Instantiation, linearizable_tests, ::testing::Range(1, 50));
