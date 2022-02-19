#!/bin/bash

set -e

if [[ `pgrep -a java | grep northstar.jar | wc -l` > 0 ]]; then
	kill `pgrep -a java | grep northstar.jar | awk '{print $1}'`
fi

if [[ -z `ls ~ | grep northstar-monitor` ]]; then
	# 不同的版本对应的前端部署包可能不同
	cd ~ && wget https://gitee.com/dromara/northstar-monitor/attach_files/971552/download/dist.tar.gz
	tar -xvf dist.tar.gz
	mkdir ~/northstar-monitor
	mv dist ~/northstar-monitor/dist
	cd northstar-monitor/dist && nohup node bundle.js >ns-monitor.log &
fi

cd ~/northstar
~/apache-maven-3.6.3/bin/mvn clean install -Dmaven.test.skip=true
\mv -f northstar-main/target/northstar-main*.jar ~/northstar.jar
