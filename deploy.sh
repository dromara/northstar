#!/bin/bash

set -e

# 停止旧进程
if [[ `pgrep -a java | grep northstar.jar | wc -l` > 0 ]]; then
	kill `pgrep -a java | grep northstar.jar | awk '{print $1}'`
fi

cd ~/northstar
mvn clean install -Dmaven.test.skip=true
\mv -f northstar-main/target/northstar-*.jar ~/northstar.jar
