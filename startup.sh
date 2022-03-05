#!/bin/bash
cd ~
# 启动前端
nohup node ~/northstar-monitor/dist/bundle.js >ns-monitor.log &
# 启动服务
nohup java -Xlog:gc*:gc.log -Xmn1g -Xmx1g -DEMAIL0=$EMAIL0 -jar northstar.jar >ns.log &
