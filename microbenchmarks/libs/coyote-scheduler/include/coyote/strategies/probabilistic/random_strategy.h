// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

#ifndef COYOTE_RANDOM_STRATEGY_H
#define COYOTE_RANDOM_STRATEGY_H

#include "../random.h"
#include "../strategy.h"
#include <string>

namespace coyote
{
	class RandomStrategy : public Strategy
	{
	private:
		// The pseudo-random generator.
		Random generator;

		// The seed used by the current iteration.
		size_t iteration_seed;

	public:
		RandomStrategy(size_t seed) noexcept;

		RandomStrategy(RandomStrategy&& strategy) = delete;
		RandomStrategy(RandomStrategy const&) = delete;

		RandomStrategy& operator=(RandomStrategy&& strategy) = delete;
		RandomStrategy& operator=(RandomStrategy const&) = delete;

		// Returns the next operation.
		size_t next_operation(Operations& operations);

		// Returns the next boolean choice.
		bool next_boolean();

		// Returns the next integer choice.
		int next_integer(int max_value);

		// Returns the seed used in the current iteration.
		size_t seed();

		// Prepares the next iteration.
		void prepare_next_iteration();

		// Description about the strategy
		std::string get_description();

		// Fair strategy or not
		bool is_fair();
	};
}

#endif // COYOTE_RANDOM_STRATEGY_H
