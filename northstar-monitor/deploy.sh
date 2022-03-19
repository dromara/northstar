#!/bin/bash

set -e

[[ $(pgrep node | wc -l) > 0 ]] && bash ./shutdown.sh

echo "准备环境依赖..."
yum install git nodejs -y

echo "构建项目"
npm config set registry http://registry.npm.taobao.org/
npm i
npm run build
cd server
npm i
cd ..
bash ./startup.sh