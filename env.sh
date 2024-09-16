#!/bin/bash

set -e

echo "准备环境依赖..."
yum install git wget -y

mkdir -p ~/northstar-env ~/northstar-dist

# 检查JDK环境
if java -version 2>&1 | grep -q "java version 21"; 
then
	echo "JDK21已安装"
	java -version
else
	echo "安装JDK21"
	cd ~/northstar-env && wget --no-check-certificate https://gitee.com/dromara/northstar/attach_files/1903632/download
	tar -xvf jdk-21_linux-x64_bin.tar.gz
	rm -f jdk-21_linux-x64_bin.tar.gz
	ln -sf ~/northstar-env/$(find jdk* -maxdepth 0 -type d)/bin/* /usr/local/bin/
fi

echo "环境安装完成"