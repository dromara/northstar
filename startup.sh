#!/bin/bash

# 启动服务
nohup java -Xlog:gc*:gc.log -Xmn2g -Xmx2g -jar ~/northstar-dist/northstar*.jar >ns.log &
