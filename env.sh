#!/bin/bash

echo "准备环境依赖..."
yum install git java-11-openjdk-devel.x86_64 nodejs wget -y
cd ~
echo "安装Maven"
wget --no-check-certificate https://mirrors.bfsu.edu.cn/apache/maven/maven-3/3.6.3/binaries/apache-maven-3.6.3-bin.tar.gz
tar -xvf apache-maven-3.6.3-bin.tar.gz
ln -s ~/apache-maven-3.6.3/bin/mvn /usr/local/bin/
wget https://gitee.com/dromara/northstar/raw/master/settings.xml
\cp -f settings.xml ~/apache-maven-3.6.3/conf/settings.xml

echo “环境安装完成”