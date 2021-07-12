# syntax=docker/dockerfile:1
#
FROM ubuntu:18.04

#
# Update the image to the latest packages
RUN apt-get update && apt-get upgrade -y

# Install gpg and wget
RUN apt-get install -y gpg wget

# Install cmake
RUN wget https://github.com/Kitware/CMake/releases/download/v3.17.2/cmake-3.17.2-Linux-x86_64.sh \
      -q -O /tmp/cmake-install.sh \
      && chmod u+x /tmp/cmake-install.sh \
      && mkdir /usr/bin/cmake \
      && /tmp/cmake-install.sh --skip-license --prefix=/usr/bin/cmake \
      && rm /tmp/cmake-install.sh

ENV PATH="/usr/bin/cmake/bin:${PATH}"

# Install other dependencies
RUN apt-get install -y libboost-all-dev         # Boost for logging
RUN apt-get install -y libcpprest-dev           # For Rest API of MonkeyDB

# Dependencies for Python plot script
RUN apt-get -y install python3-pip
RUN pip3 install numpy
RUN pip3 install pandas
RUN pip3 install plotly==5.1.0
RUN pip3 install -U kaleido


COPY . /MonkeyDB-KV
WORKDIR /MonkeyDB-KV

# Use:
#       docker run -it image_name /bin/bash
# To start bash with this docker image.
#
# For sanity check of MonkeyDB-KV, run the following script:
#       ./BuildAndRunMicrobenchmarks.sh
#
# With no parameters the script should finish in <5 mins, printing required
# information on screen with the plot pdf generated as file :
# /MonkeyDB-KV/build-files/applications/average_states_random_test_cases.pdf
# 
# Use following command to copy plot pdf from docker container (as docker can
# not display pdfs):
#   docker cp container_name:/MonkeyDB-KV/build-files/applications/average_states_random_test_cases.pdf .
#
# To reproduce results given in paper for microbrenchmarks, use:
#       ./BuildAndRunMicrobenchmarks.sh 5000 50
#


