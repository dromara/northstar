#!/bin/bash
cd ~
nohup java -Xlog:gc*:gc.log -Xmn1g -Xmx1g  -jar northstar.jar >ns.log &
