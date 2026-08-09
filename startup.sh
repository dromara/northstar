#!/bin/bash

# 启动服务
# Spring Boot 4 起可执行 jar 默认使用 JarLauncher，loader.path 需通过显式 PropertiesLauncher 启动才能生效
JAR=$(find /root/northstar-dist -name 'northstar-[0-9]*.*.jar')
nohup java -Xlog:gc*:gc.log -Xms2g -Xmx2g -Dloader.path=$(pwd) -Denv=prod -cp $JAR org.springframework.boot.loader.launch.PropertiesLauncher >/root/northstar-dist/ns.log &
