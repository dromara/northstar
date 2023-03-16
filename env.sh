#!/bin/bash

set -e

echo "准备环境依赖..."
yum install git wget python27 gcc gcc-c++ -y

mkdir -p ~/northstar-env ~/northstar-dist

# 检查JDK环境
if [[ $(which java >/dev/null && echo $?) != 0 ]]; 
then
	echo "安装JDK17"
	cd ~/northstar-env && wget --no-check-certificate https://download.oracle.com/java/17/archive/jdk-17.0.5_linux-x64_bin.tar.gz
	tar -xvf jdk-17.0.5_linux-x64_bin.tar.gz
	rm -f jdk-17.0.5_linux-x64_bin.tar.gz
	ln -sf ~/northstar-env/$(find jdk* -maxdepth 0 -type d)/bin/* /usr/local/bin/
else
	echo "JDK17已安装"
	java -version
fi

# 安装Redis
if [[ $(which redis-server >/dev/null && echo $?) != 0 ]];
then
	echo "安装Redis"
	cd ~/northstar-env && wget --no-check-certificate http://download.redis.io/releases/redis-7.0.0.tar.gz
	tar -xzf redis-7.0.0.tar.gz
	rm -f ~/northstar-env/redis-7.0.0.tar.gz
	cd redis-7.0.0
	make
	make install
	redis-server --daemonize yes --maxmemory 512m --maxmemory-policy volatile-lfu
else
	echo "Redis已安装"
fi

echo "环境安装完成"