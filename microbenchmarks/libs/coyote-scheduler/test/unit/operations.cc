// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

#include "test.h"
#include "coyote/operations/operations.h"

using namespace coyote;

int main()
{
	std::cout << "[test] started." << std::endl;
	auto start_time = std::chrono::steady_clock::now();

	Operations ops;
	assert(ops.size() == 0, "unexpected enabled size [0]");
	assert(ops.size(false) == 0, "unexpected disabled size [0]");

	ops.insert(7);
	assert(ops.size() == 1, "unexpected enabled size [1]");
	assert(ops.size(false) == 0, "unexpected disabled size [1]");
	assert(ops[0] == 7, "unexpected element in index 0 [1]");

	ops.disable(7);
	assert(ops.size() == 0, "unexpected enabled size [2]");
	assert(ops.size(false) == 1, "unexpected disabled size [2]");

	ops.enable(7);
	assert(ops.size() == 1, "unexpected enabled size [3]");
	assert(ops.size(false) == 0, "unexpected disabled size [3]");
	assert(ops[0] == 7, "unexpected element in index 0 [3]");

	ops.insert(5);
	assert(ops.size() == 2, "unexpected enabled size [4]");
	assert(ops.size(false) == 0, "unexpected disabled size [4]");
	assert(ops[0] == 7, "unexpected element in index 0 [4]");
	assert(ops[1] == 5, "unexpected element in index 1 [4]");

	ops.disable(7);
	assert(ops.size() == 1, "unexpected enabled size [5]");
	assert(ops.size(false) == 1, "unexpected disabled size [5]");
	assert(ops[0] == 5, "unexpected element in index 0 [5]");

	ops.insert(3);
	assert(ops.size() == 2, "unexpected enabled size [6]");
	assert(ops.size(false) == 1, "unexpected disabled size [6]");
	assert(ops[0] == 5, "unexpected element in index 0 [6]");
	assert(ops[1] == 3, "unexpected element in index 1 [6]");

	ops.enable(7);
	assert(ops.size() == 3, "unexpected enabled size [7]");
	assert(ops.size(false) == 0, "unexpected disabled size [7]");
	assert(ops[0] == 5, "unexpected element in index 0 [7]");
	assert(ops[1] == 3, "unexpected element in index 1 [7]");
	assert(ops[2] == 7, "unexpected element in index 2 [7]");

	ops.remove(3);
	assert(ops.size() == 2, "unexpected enabled size [8]");
	assert(ops.size(false) == 0, "unexpected disabled size [8]");
	assert(ops[0] == 5, "unexpected element in index 0 [8]");
	assert(ops[1] == 7, "unexpected element in index 1 [8]");

	ops.insert(3);
	assert(ops.size() == 3, "unexpected enabled size [9]");
	assert(ops.size(false) == 0, "unexpected disabled size [9]");
	assert(ops[0] == 5, "unexpected element in index 0 [9]");
	assert(ops[1] == 7, "unexpected element in index 1 [9]");
	assert(ops[2] == 3, "unexpected element in index 2 [9]");

	ops.disable(3);
	assert(ops.size() == 2, "unexpected enabled size [10]");
	assert(ops.size(false) == 1, "unexpected disabled size [10]");
	assert(ops[0] == 5, "unexpected element in index 0 [10]");
	assert(ops[1] == 7, "unexpected element in index 1 [10]");

	ops.remove(5);
	assert(ops.size() == 1, "unexpected enabled size [11]");
	assert(ops.size(false) == 1, "unexpected disabled size [11]");
	assert(ops[0] == 7, "unexpected element in index 0 [11]");

	ops.insert(4);
	assert(ops.size() == 2, "unexpected enabled size [12]");
	assert(ops.size(false) == 1, "unexpected disabled size [12]");
	assert(ops[0] == 7, "unexpected element in index 0 [12]");
	assert(ops[1] == 4, "unexpected element in index 1 [12]");

	ops.remove(7);
	assert(ops.size() == 1, "unexpected enabled size [13]");
	assert(ops.size(false) == 1, "unexpected disabled size [13]");
	assert(ops[0] == 4, "unexpected element in index 0 [13]");

	ops.remove(3);
	assert(ops.size() == 1, "unexpected enabled size [14]");
	assert(ops.size(false) == 0, "unexpected disabled size [14]");
	assert(ops[0] == 4, "unexpected element in index 0 [14]");

	ops.clear();
	assert(ops.size() == 0, "unexpected enabled size [15]");
	assert(ops.size(false) == 0, "unexpected disabled size [15]");

	for (size_t i = 0; i < 10000; i++)
	{
		ops.insert(i + 7);
		ops.disable(i + 7);
		ops.enable(i + 7);
		ops.insert(i + 5);
		ops.disable(i + 7);
		ops.insert(i + 3);
		ops.enable(i + 7);

		if (i % 1000 == 0)
		{
#ifdef COYOTE_DEBUG_LOG
			std::cout << "[test] size: " << ops.size() << std::endl;
#endif // COYOTE_DEBUG_LOG
		}
	}

	ops.clear();

	for (int i = 0; i < 10000; i++)
	{
		ops.insert(i);
	}

	for (size_t i = 0; i < 10000; i++)
	{
		ops.disable(i);
		ops.enable(i);

		if (i % 1000 == 0)
		{
#ifdef COYOTE_DEBUG_LOG
			std::cout << "[test] size: " << ops.size() << std::endl;
#endif // COYOTE_DEBUG_LOG
		}
	}

	std::cout << "[test] done in " << total_time(start_time) << "ms." << std::endl;
	return 0;
}
