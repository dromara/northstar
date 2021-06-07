#!/bin/bash

echo "安装MongoDB"
mv mongo.repo /etc/yum.repos.d/mongodb-org-4.0.repo
yum install -y mongodb-org
systemctl start mongod
echo “安装完成”