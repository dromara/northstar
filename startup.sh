#!/bin/bash

# 启动前端
cd ~/northstar-monitor/dist/ && nohup node bundle.js >~/ns-monitor.log &
# 启动服务
cd ~ && nohup java -Xlog:gc*:gc.log -Xmn1g -Xmx1g -DEMAIL0=$EMAIL0 -jar northstar.jar >~/ns.log &
