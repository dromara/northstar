#!/bin/bash

set -e

echo "准备环境依赖..."
yum install git wget -y

mkdir -p ~/northstar-env ~/northstar-dist

# 检查JDK环境
if [[ $(which java >/dev/null && echo $?) != 0 ]]; 
then
	echo "安装JDK21"
	cd ~/northstar-env && wget --no-check-certificate https://download.oracle.com/java/21/latest/jdk-21_linux-x64_bin.tar.gz
	tar -xvf jdk-21_linux-x64_bin.tar.gz
	rm -f jdk-21_linux-x64_bin.tar.gz
	ln -sf ~/northstar-env/$(find jdk* -maxdepth 0 -type d)/bin/* /usr/local/bin/
else
	echo "JDK21已安装"
	java -version
fi

echo "环境安装完成"