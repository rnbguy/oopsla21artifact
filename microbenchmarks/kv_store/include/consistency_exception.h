// ------------------------------------------------------------
// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
//

#ifndef NEKARA_KEY_VALUE_STORE_CONSISTENCY_EXCEPTION_H
#define NEKARA_KEY_VALUE_STORE_CONSISTENCY_EXCEPTION_H

#include <exception>
#include <string>
#include <cstring>

namespace nekara {
    class consistency_exception : public std::exception {
    public:
        consistency_exception(const std::string &tx_name, long tx_id) {
            this->tx_details = tx_name + " TX_ID: " + std::to_string(tx_id);
        }

        const char* what() const throw() {
            return strcat((char *)"Inconsistent transaction: ", this->tx_details.c_str());
        }

    private:
        std::string tx_details;
    };
}
#endif //NEKARA_KEY_VALUE_STORE_CONSISTENCY_EXCEPTION_H
