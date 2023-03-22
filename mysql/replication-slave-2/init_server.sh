#!/bin/bash

set -e

cd -- "$( dirname -- "${BASH_SOURCE[0]}" )"

./clean_up.sh

../mysql-8.0.31-linux-glibc2.17-x86_64-minimal/bin/mysqld --defaults-file=config-replication-slave-2.cfg --initialize-insecure
