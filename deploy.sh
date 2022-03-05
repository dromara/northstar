#!/bin/bash

set -e

# 停止旧进程
if [[ `pgrep -a java | grep northstar.jar | wc -l` > 0 ]]; then
	kill `pgrep -a java | grep northstar.jar | awk '{print $1}'`
fi

# 移除原有目录
if [[ -n `ls ~ | grep northstar-monitor` ]]; then
	kill `pgrep node`
	rm -rf ~/northstar-monitor
fi

# 不同的版本对应的前端部署包可能不同
cd ~ && wget https://gitee.com/dromara/northstar-monitor/attach_files/986590/download/dist.tar.gz
tar -xvf dist.tar.gz && rm -f dist.tar.gz
mkdir ~/northstar-monitor
mv dist ~/northstar-monitor/dist

cd ~/northstar
~/apache-maven-3.6.3/bin/mvn clean install -Dmaven.test.skip=true
\mv -f northstar-main/target/northstar-main*.jar ~/northstar.jar
