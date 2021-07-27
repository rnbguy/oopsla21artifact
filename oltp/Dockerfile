FROM archlinux:base-20210718.0.29333
LABEL maintainer="Ranadeep B <mail [at] rnbguy [dot] at>"

RUN pacman -Syu git rustup ant mariadb openssl clang base-devel --needed --noconfirm
RUN rustup install stable
WORKDIR /root
RUN git clone --depth 1 https://github.com/rnbguy/oopsla21artifact
WORKDIR /root/oopsla21artifact/oltp
RUN bash build.sh
WORKDIR /root/oopsla21artifact/oltp
