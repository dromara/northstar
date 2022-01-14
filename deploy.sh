#!/bin/bash

set -e

if [[ `pgrep -a java | grep northstar.jar | wc -l` > 0 ]]; then
	kill `pgrep -a java | grep northstar.jar | awk '{print $1}'`
fi

if [[ -z `ls ~ | grep northstar-monitor` ]]; then
	cd ~ && wget https://gitee.com/dromara/northstar-monitor/attach_files/945147/download/dist.tar.gz
	tar -xvf dist.tar.gz
	mv dist northstar-monitor
	cd northstar-monitor && nohup node bundle.js >ns-monitor.log &
fi

cd ~/northstar
~/apache-maven-3.6.3/bin/mvn clean install -Dmaven.test.skip=true
\mv -f northstar-main/target/northstar-main*.jar ~/northstar.jar
