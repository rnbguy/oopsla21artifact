// ------------------------------------------------------------
// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
//

#ifndef NEKARA_KEY_VALUE_STORE_KEY_NOT_FOUND_EXCEPTION_H
#define NEKARA_KEY_VALUE_STORE_KEY_NOT_FOUND_EXCEPTION_H

#include <exception>
#include <string>
#include <cstring>

namespace nekara {
    class key_not_found_exception : public std::exception {
    public:
        key_not_found_exception(const std::string &key_str) {
            this->key = key_str;
        }

        const char* what() const throw() {
            return strcat((char *)"Key Not Found: ", this->key.c_str());
        }
    private:
        std::string key;
    };
}
#endif //NEKARA_KEY_VALUE_STORE_KEY_NOT_FOUND_EXCEPTION_H
