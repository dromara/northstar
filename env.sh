#!/bin/bash

set -e

echo "准备环境依赖..."
yum install git wget python36 gcc gcc-c++ -y

mkdir -p /home/northstar/northstar-env


if [[ $(which node >/dev/null && echo $?) != 0 ]];
then 
	echo "安装Node16"
	cd /home/northstar/northstar-env && wget --no-check-certificate https://nodejs.org/download/release/v14.19.1/node-v14.19.1-linux-x64.tar.xz
	tar -xvf node-v14.19.1-linux-x64.tar.xz 
	rm -f node-v14.19.1-linux-x64.tar.xz 
	ln -sf /home/northstar/northstar-env/node-v14.19.1-linux-x64/bin/* /usr/local/bin/
else
	echo "Node16已安装"
	node -v
fi

# 检查JDK环境
if [[ $(which java >/dev/null && echo $?) != 0 ]]; 
then
	echo "安装JDK17"
	cd /home/northstar/northstar-env && wget --no-check-certificate https://download.oracle.com/java/17/latest/jdk-17_linux-x64_bin.tar.gz
	tar -xvf jdk-17_linux-x64_bin.tar.gz
	rm -f jdk-17_linux-x64_bin.tar.gz
	ln -sf /home/northstar/northstar-env/$(find jdk* -maxdepth 0 -type d)/bin/* /usr/local/bin/
else
	echo "JDK17已安装"
	java -version
fi

# 检查Maven环境
if [[ $(which mvn >/dev/null && echo $?) != 0 ]]; 
then
	echo "安装Maven"
	cd /home/northstar/northstar-env && wget --no-check-certificate https://mirrors.bfsu.edu.cn/apache/maven/maven-3/3.6.3/binaries/apache-maven-3.6.3-bin.tar.gz
	tar -xvf apache-maven-3.6.3-bin.tar.gz
	rm -f apache-maven-3.6.3-bin.tar.gz
	ln -sf /home/northstar/northstar-env/apache-maven-3.6.3/bin/mvn /usr/local/bin/
	curl https://gitee.com/dromara/northstar/raw/master/settings.xml >/home/northstar/northstar-env/apache-maven-3.6.3/conf/settings.xml
else
	echo "Maven已安装"
	mvn -v
fi

# 安装Redis
if [[ $(which redis-server >/dev/null && echo $?) != 0 ]];
then
	echo "安装Redis"
	cd /home/northstar/northstar-env && wget --no-check-certificate http://download.redis.io/releases/redis-7.0.0.tar.gz
	tar -xzf redis-7.0.0.tar.gz
	rm -f /home/northstar/northstar-env/redis-7.0.0.tar.gz
	cd redis-7.0.0
	make
	make install
	redis-server --daemonize yes
else
	echo "Redis已安装"
fi

echo "环境安装完成"