#!/bin/bash

# 启动服务
nohup java -Xlog:gc*:gc.log -Xmn2g -Xmx2g -Denv=prod -jar ~/northstar-dist/northstar*.jar >ns.log &
