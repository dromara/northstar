#!/bin/bash

echo "安装MongoDB"
curl -sSL https://get.daocloud.io/docker | sh 
systemctl enable docker
systemctl start docker 
echo {\"registry-mirrors\": [\"https://docker.mirrors.ustc.edu.cn\"]} >/etc/docker/daemon.json
systemctl restart docker
docker pull mongo:4.0 
docker run --name Mongo --net=host -d mongo:4.0
echo "docker start Mongo" >>/etc/rc.local
echo “安装完成”