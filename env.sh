#!/bin/bash

set -e

echo "准备环境依赖..."

mkdir -p ~/northstar-env ~/northstar-dist

JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d. -f1)

# 检查JDK环境
if [[ "$JAVA_VERSION" -ne 25 ]]; 
then
	echo "安装JDK25"
	cd ~/northstar-env && curl -k -L -o jdk-25_linux-x64_bin.tar.gz https://download.oracle.com/java/25/latest/jdk-25_linux-x64_bin.tar.gz
	tar -xvf jdk-25_linux-x64_bin.tar.gz
	rm -f jdk-25_linux-x64_bin.tar.gz
	ln -sf ~/northstar-env/$(find jdk* -maxdepth 0 -type d)/bin/* /usr/local/bin/
else
	echo "JDK25已安装"
	java -version
fi

echo "环境安装完成"