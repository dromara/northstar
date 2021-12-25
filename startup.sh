#!/bin/bash
cd ~
nohup java -Xlog:gc*:gc.log -Xmn1g -Xmx1g -DEMAIL0=$EMAIL0 -jar northstar.jar >ns.log &
