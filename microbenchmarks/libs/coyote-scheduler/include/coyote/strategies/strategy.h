// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

#ifndef COYOTE_STRATEGY_H
#define COYOTE_STRATEGY_H

#include "../operations/operations.h"
#include <string>

namespace coyote
{
	class Strategy
	{
	public:
		Strategy() {};

		// Returns the next operation.
		virtual size_t next_operation(Operations& operations) = 0;

		// Returns the next boolean choice.
		virtual bool next_boolean() = 0;

		// Returns the next integer choice.
		virtual int next_integer(int max_value) = 0;

		// Prepares the next iteration.
		virtual void prepare_next_iteration() = 0;

		// Description about the strategy
		virtual std::string get_description() = 0;

		// Fair strategy or not
		virtual bool is_fair() = 0;

		virtual bool completely_explored() {
		    return false;
		}
	};
}

#endif
