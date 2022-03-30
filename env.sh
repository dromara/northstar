#!/bin/bash

set -e

echo "准备环境依赖..."
yum install git nodejs wget python27 gcc gcc-c++ -y

# 检查JDK环境
if [[ $(which java >/dev/null && echo $?) != 0 ]]; 
then
	echo "安装JDK17"
	cd ~ && wget --no-check-certificate https://download.oracle.com/java/17/latest/jdk-17_linux-x64_bin.tar.gz
	tar -xvf jdk-17_linux-x64_bin.tar.gz
	rm -f jdk-17_linux-x64_bin.tar.gz
	ln -s ~/$(find jdk* -maxdepth 0 -type d)/bin/* /usr/local/bin/
else
	echo "JDK17已安装"
	java -version
fi

# 检查Maven环境
if [[ $(which mvn >/dev/null && echo $?) != 0 ]]; 
then
	echo "安装Maven"
	wget --no-check-certificate https://mirrors.bfsu.edu.cn/apache/maven/maven-3/3.6.3/binaries/apache-maven-3.6.3-bin.tar.gz
	tar -xvf apache-maven-3.6.3-bin.tar.gz
	rm -f apache-maven-3.6.3-bin.tar.gz
	ln -s ~/apache-maven-3.6.3/bin/mvn /usr/local/bin/
	curl https://gitee.com/dromara/northstar/raw/master/settings.xml >~/apache-maven-3.6.3/conf/settings.xml
else
	echo "Maven已安装"
	mvn -v
fi

# 检查MongoDB环境
if [[ $(systemctl status mongod | grep active | wc -l) == 0 ]];
then
	echo "安装MongoDB"
	curl https://gitee.com/dromara/northstar/raw/master/mongo.repo >/etc/yum.repos.d/mongodb-org-4.0.repo
	yum install -y mongodb-org
	systemctl start mongod
else
	echo "MongoDB已安装"
fi

echo "环境安装完成"