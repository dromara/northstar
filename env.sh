#!/bin/bash

set -e

echo "准备环境依赖..."
yum install git nodejs wget python27 -y
cd ~
echo "安装JDK17"
wget --no-check-certificate https://download.oracle.com/java/17/latest/jdk-17_linux-x64_bin.tar.gz
tar -xvf jdk-17_linux-x64_bin.tar.gz
ln -s ~/jdk-17.0.1/bin/* /usr/local/bin/
echo "安装Maven"
wget --no-check-certificate https://mirrors.bfsu.edu.cn/apache/maven/maven-3/3.6.3/binaries/apache-maven-3.6.3-bin.tar.gz
tar -xvf apache-maven-3.6.3-bin.tar.gz
ln -s ~/apache-maven-3.6.3/bin/mvn /usr/local/bin/
wget https://gitee.com/dromara/northstar/raw/master/settings.xml
\cp -f settings.xml ~/apache-maven-3.6.3/conf/settings.xml

echo “环境安装完成”