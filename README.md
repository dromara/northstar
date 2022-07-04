# Northstar盈富量化交易平台
<p align="center">
    <img src ="https://img.shields.io/badge/version-4.0.0-yellow.svg"/>
    <img src ="https://img.shields.io/badge/licence-Apache2.0-blue.svg"/>
    <img src ="https://img.shields.io/badge/lang-JAVA|JS-orange.svg"/>
</p>

**开源声明：**  
**本项目归入dromara开源组织运营的初心，是希望可以有更多志同道合的朋友一起参与项目的开发，并且能借其在交易市场上有所收获！**  
**借用组织的口号：一个人或许能走的更快，但一群人会走的更远。**  
**本项目仅属于技术分享，不构成任何交易建议。使用者自身在交易前，需要清楚其可能面对的交易风险与相关法律规定，并为自身行为负责！**

这是一个用户可以自行编写交易策略程序的量化交易平台。
用户监控台效果（监控台仅用于给用户提供一个可视化窗口，以方便进行程序的监控与管理）：
![输入图片说明](https://images.gitee.com/uploads/images/2022/0619/230500_fe02aedd_1676852.png "屏幕截图.png")
![输入图片说明](https://images.gitee.com/uploads/images/2022/0619/230527_2e868183_1676852.png "屏幕截图.png")
![输入图片说明](https://images.gitee.com/uploads/images/2022/0619/230813_a3991d60_1676852.png "屏幕截图.png")
![输入图片说明](https://images.gitee.com/uploads/images/2022/0620/105911_4e5622ee_1676852.png "屏幕截图.png")
![Image](https://images.gitee.com/uploads/images/2021/0606/220710_eeab5dd9_1676852.png)
![Image](https://images.gitee.com/uploads/images/2021/0606/220728_32ef6b37_1676852.png)

## 产品简介
这是一个面向程序员的开源高频量化交易平台，用于期货、股票、外汇、炒币等多种交易场景，实现自动交易。暂时只对接了国内期货交易所，理论上可以对接任意交易所。

**功能特性：**
- 一站式平台，可适配对接不同的交易所；
- 灵活多变的自动化策略框架，能实现复杂的个性化交易逻辑，如多合约价差交易，算法高频交易，CTA交易，期权期货混合交易等等；
- 支持多账户交易，能实现跨市套利等复杂逻辑；
- 直观易理解的API编程接口，并且提供了多种策略的编写范例，只需要掌握最基本的JAVA编程知识便可以上手编写自己的交易策略；
- 支持高精度历史行情回放，便于操盘手进行回放训练，或用于验证策略模组；
- 自然易操作的自动化模组管理，轻松掌握与管理自动化策略的运行状态；
- 可实现完全自主的风控手段；
- 私有化部署，确保策略安全；

## 适用人群
专业量化操盘手、全栈技术爱好者、小型私募技术团队

**开源不易，感谢点赞关注加收藏！**  
**详细文档请参考wiki： https://gitee.com/dromara/northstar/wikis/**

## 实盘注意事项
为了更好地了解实盘用户的使用情况，程序对期货公司做了一定的管理，如需要进行实盘交易，请联系作者咨询。

## 社群支持
**为防止滥发广告的微信号加群，加群需收取100元红包（可抵扣数据服务费用）**
![输入图片说明](https://images.gitee.com/uploads/images/2022/0107/210113_21d2977f_1676852.jpeg "微信图片_20220107210039.jpg")

## 运行环境
建议使用Linux云服务器，或者Windows系统（MAC系统没有试过，需自行摸索）

## 程序架构
- B/S架构
- northstar项目为服务端（包含了web网页监控端）
- 交互协议HTTP + websocket
- 数据库、缓存为Redis7（历史行情数据主要依赖数据服务，本地仅保存少量账户配置信息）
- 前端采用node14 + vue2.x
- 服务端采用java17（拥抱新技术） + springboot3

项目架构采用事件驱动+插件式开发
![Image](https://images.gitee.com/uploads/images/2021/1107/172130_9da2bdcd_1676852.png)

## 启动步骤
假设当前环境是全新的服务器  

### 环境准备（只需要运行一次）
**Linux环境下**，初始化系统环境。
```
curl https://gitee.com/dromara/northstar/raw/master/env.sh | sh
```

目录结构如下：  
~/  
|--northstar-dist/ 	程序包目录  
|--northstar-env/	环境依赖包目录  


**Windows环境下**，初始化系统环境。打开powershell命令行窗口，输入以下命令（部分系统可能报错，需要把脚本下载到本地后右键选择powershell执行）
```
Invoke-WebRequest https://gitee.com/dromara/northstar/raw/master/env.ps1 -OutFile env.ps1; powershell -noexit ".\env.ps1"
```

目录结构如下：  
C:\  
|--northstar-dist\ 	程序包目录  
|--northstar-env\	环境依赖包目录  

### 程序包准备
直接放到上述提及的 northstar-dist 目录
```
https://gitee.com/dromara/northstar/attach_files/1115939/download/northstar-4.0.jar
```

### 启动程序
**Linux环境下**
```
nohup java -Xmn1g -Xmx1g -jar ~/northstar-dist/northstar.jar >ns.log &
```
注意：启动命令包括了JVM的启动参数，假定服务器配置是2核4G，如有不同应该按实际情况修改

**Windows环境下**
```
s
```

### 部署验证
在浏览器直接访问部署服务的域名（端口使用了默认的80端口），应该可以看到以下界面，并可以登陆成功  
![输入图片说明](https://images.gitee.com/uploads/images/2022/0103/205503_efb41f7c_1676852.png "login.PNG")

### 查询日志
```
cd ~/logs/
```

### 终止程序
```
kill `pgrep java`
```

## 开发环境配置（Windows环境）
大多数用户开发时都是使用windows环境

### 环境准备
初始化系统环境（只需要运行一次），打开powershell命令行窗口，输入以下命令（部分系统可能报错，需要把脚本下载到本地后右键选择powershell执行）
```
Invoke-WebRequest https://gitee.com/dromara/northstar/raw/master/dev-env.ps1 -OutFile dev-env.ps1; powershell -noexit ".\dev-env.ps1"
```

## 注意事项
- 请勿直接使用master分支的最新代码，应该使用最新的tag来作为开发基线
- 服务器时间校正为北京时间
- 尽量不要在开市期间重启程序
- 编写策略逻辑时如需使用时间属性，务必使用TICK行情自带的时间戳，否则策略回测时会不准确
- 本项目为量化爱好者及JAVA开发者搭建，对交易行为并不负责
- 使用者需要自行开发交易策略并需要一定的JAVA基础

## 特别鸣谢
[redtorch项目](https://github.com/sun0x00/redtorch)作者。本项目演化自redtorch，并保留了小部分其源码，同时感谢redtorch作者的技术分享。