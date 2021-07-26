FROM archlinux:base-20210718.0.29333
LABEL maintainer="Ranadeep B <mail [at] rnbguy [dot] at>"

RUN pacman -Syu git rustup ant mariadb openssl clang base-devel --needed --noconfirm && \
    rustup install stable && \
    cd ~ && \
    git clone --depth 1 https://github.com/rnbguy/oopsla21artifact && \
    cd oopsla21artifact/oltp && \
    bash build.sh

WORKDIR /root/oopsla21artifact/oltp
