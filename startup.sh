#!/bin/bash

# 启动服务
nohup java -Xlog:gc*:gc.log -Xms2g -Xmx2g -Dloader.path=$(pwd) -Denv=prod -jar $(find /root/northstar-dist -name 'northstar-[0-9]*.*.jar') >/root/northstar-dist/ns.log &
