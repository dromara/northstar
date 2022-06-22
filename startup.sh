#!/bin/bash

# 启动服务
nohup java -Xlog:gc*:gc.log -Xmn1g -Xmx1g -jar ~/northstar-dist/northstar.jar >ns.log &
