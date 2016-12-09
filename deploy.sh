#!/bin/sh

mvn clean install -Dmaven.test.skip
scp target/im-engine-2.0.tar.gz root@172.20.9.79:/data/soft
