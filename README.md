# Northstar量化交易平台

**开源声明：**  
**本项目归入dromara开源组织运营的初心，是希望可以有更多志同道合的朋友一起参与项目的开发，并且能借其在交易市场上有所收获！**  
**借用组织的口号：一个人或许能走的更快，但一群人会走的更远。**  
**本项目仅属于技术分享，不构成任何交易建议。使用者自身在交易前，需要清楚其可能面对的交易风险与相关法律规定，并为自身行为负责！**

这是一个面向程序员的开源量化交易平台。用于期货、股票、外汇、炒币等多种投机场景，实现程序化投机。暂时只对接了国内期货交易所，理论上可以对接任意交易所。

![输入图片说明](https://images.gitee.com/uploads/images/2021/0909/075051_336e5835_1676852.png "封面.png")
![Image text](https://images.gitee.com/uploads/images/2021/0609/223845_f3942e1e_1676852.png)

通过JAVA后台来编写程序化的交易策略，并提供页面监控界面。详细请参考监控端项目https://gitee.com/dromara/northstar-monitor

![Image](https://images.gitee.com/uploads/images/2021/0606/220710_eeab5dd9_1676852.png)
![Image](https://images.gitee.com/uploads/images/2021/0606/220728_32ef6b37_1676852.png)


项目架构采用事件驱动+插件式开发
![Image](https://images.gitee.com/uploads/images/2021/1107/172130_9da2bdcd_1676852.png)


**开源不易，感谢点赞关注加收藏！**  
**详细文档请参考wiki： https://gitee.com/dromara/northstar/wikis/**

## 适用人群
全栈技术爱好者、小型私募技术团队

## 社群支持
详情可私聊

## 运行环境
建议使用Linux云服务器

## 程序架构
- B/S架构
- northstar项目为服务端
- northstar-monitor项目为web网页监控端
- 交互协议HTTP + websocket
- 数据库为MongoDB
- 前端采用nodejs + vue
- 服务端采用java + springboot

## 启动步骤
假设当前环境是全新的服务器  

初始化系统环境（只需要运行一次）
```
curl https://gitee.com/dromara/northstar/raw/master/env.sh | sh
```

初始化数据库（如果使用外部数据库的可以忽略）
```
curl -o mongo.repo https://gitee.com/dromara/northstar/raw/master/mongo.repo
curl https://gitee.com/dromara/northstar/raw/master/mongo.sh | sh
```
安装成功后验证mongoDB 是否安装成功（如果使用外部数据库的可以忽略）
```
systemctl status mongod
```
如果成功的话，屏幕输出如下，注意第三行Active的状态是active(running)：
```
● mongod.service - MongoDB Database Server
   Loaded: loaded (/usr/lib/systemd/system/mongod.service; enabled; vendor preset: disabled)
   Active: active (running) since Sun 2021-06-27 17:30:14 CST; 56s ago
     Docs: https://docs.mongodb.org/manual
  Process: 5932 ExecStart=/usr/bin/mongod $OPTIONS (code=exited, status=0/SUCCESS)
  Process: 5929 ExecStartPre=/usr/bin/chmod 0755 /var/run/mongodb (code=exited, status=0/SUCCESS)
  Process: 5926 ExecStartPre=/usr/bin/chown mongod:mongod /var/run/mongodb (code=exited, status=0>
  Process: 5923 ExecStartPre=/usr/bin/mkdir -p /var/run/mongodb (code=exited, status=0/SUCCESS)
 Main PID: 5934 (mongod)
   Memory: 53.2M
   CGroup: /system.slice/mongod.service
           └─5934 /usr/bin/mongod -f /etc/mongod.conf

Jun 27 17:30:13 ai-trader-hw systemd[1]: Starting MongoDB Database Server...
Jun 27 17:30:13 ai-trader-hw mongod[5932]: about to fork child process, waiting until server is r>
Jun 27 17:30:13 ai-trader-hw mongod[5932]: forked process: 5934
Jun 27 17:30:14 ai-trader-hw mongod[5932]: child process started successfully, parent exiting
Jun 27 17:30:14 ai-trader-hw systemd[1]: Started MongoDB Database Server.
```

下载最新版项目，然后解压。**注意：把解压后的文件夹重命名为northstar**
[https://gitee.com/dromara/northstar/releases](https://gitee.com/dromara/northstar/releases)


修改northstar/northstar-main/src/main/resources/application.yml配置文件中的邮箱代理配置  
(若不使用邮件通知功能，可以忽略此步)
```
spring:
  ...

  mail:
    host: smtp.126.com              //不同的邮箱配置不同
    username: ${email}              //为了不上传git，采用启动参数配置
    password: UQNXPDZJIYQTRFRW      //在邮箱网站获取
    default-encoding: UTF-8
    subscribed: ${sub_email}        //为了不上传git，采用启动参数配置
    port: 465
    protocol: smtp
    
    ...
```

在.bashrc中加入以上启动参数（这样做能隐藏启动参数）  
```
vim ~/.bashrc
```
在文末加入以下设置
```
...
export NSUSER=<登陆用户名>
export NSPWD=<登陆密码>
export EMAIL=<代理邮箱名> 
export EMAIL0=<订阅邮箱名>
```
保存并退出，然后让配置生效
```
source ~/.bashrc
```

部署程序（每次代码更新后运行）
```
cd ~/northstar
bash deploy.sh
```

外部插件包部署(假设已经下载好)
```
cd ~/northstar-external
bash deploy.sh
```

然后回到northstar目录启动程序
```
cd ~/northstar
bash startup.sh
```
注意：startup.sh脚本中包括了JVM的启动参数，假定服务器配置是2核4G，如有不同应该按实际情况修改

查询日志
```
cd ~/logs/
```

终止程序
```
kill `pgrep java`
```

## 注意事项
- 请勿直接使用master分支的最新代码，应该使用最新的tag来作为开发基线
- 服务器时间校正为北京时间
- 尽量不要在开市期间重启程序
- 编写策略逻辑时如需使用时间属性，务必使用TICK行情自带的时间戳，否则策略回测时会不准确
- 本项目为量化爱好者及JAVA开发者搭建，对交易行为并不负责
- 当前项目只包含一个示例策略，若要开发其他量化策略，需要自行开发并需要一定的JAVA基础

## 温馨提示
对于仅为了满足交易需求的朋友而言，如果市场上现成的产品可以满足您的策略需求的话，请尽量使用市场上的付费产品例如文华、MC、金字塔等。因为开源项目存在大量的学习与调试成本，除非有成熟的技术背景以及存在付费产品无法实现的策略方案，才建议使用开源方案。

## 特别鸣谢
[redtorch项目](https://github.com/sun0x00/redtorch)作者。本项目演化自redtorch，并保留了小部分其源码，同时感谢redtorch作者的技术分享。