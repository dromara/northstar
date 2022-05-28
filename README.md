# Northstar盈富量化交易平台

**开源声明：**  
**本项目归入dromara开源组织运营的初心，是希望可以有更多志同道合的朋友一起参与项目的开发，并且能借其在交易市场上有所收获！**  
**借用组织的口号：一个人或许能走的更快，但一群人会走的更远。**  
**本项目仅属于技术分享，不构成任何交易建议。使用者自身在交易前，需要清楚其可能面对的交易风险与相关法律规定，并为自身行为负责！**

![输入图片说明](https://images.gitee.com/uploads/images/2022/0103/205503_efb41f7c_1676852.png "login.PNG")
![Image text](https://images.gitee.com/uploads/images/2021/0609/223845_f3942e1e_1676852.png)

通过JAVA后台来编写程序化的交易策略，并提供页面监控界面。部署方法已集成在本项目的部署脚本中。

![Image](https://images.gitee.com/uploads/images/2021/0606/220710_eeab5dd9_1676852.png)
![Image](https://images.gitee.com/uploads/images/2021/0606/220728_32ef6b37_1676852.png)

## 产品特性
这是一个面向程序员的开源高频量化交易平台，用于期货、股票、外汇、炒币等多种交易场景，实现自动交易。暂时只对接了国内期货交易所，理论上可以对接任意交易所。

**功能特性：**
- 一站式平台，可适配对接不同的交易所；
- 允许多账户交易，能实现跨市套利等复杂逻辑；
- 灵活多变的自动化策略框架，能实现复杂的个性化交易逻辑，如多合约价差交易，高频交易，CTA交易，期权期货混合交易等等；
- 自然易操作的自动化模组管理，轻松掌握与管理自动化策略的运行状态；
- 直观易理解的API编程接口，并且提供了多种策略的编写范例，只需要掌握最基本的JAVA编程知识便可以上手编写自己的交易策略；
- 可实现完全自主的风控手段；
- 配有历史行情回放功能，便于操盘手进行回放训练，或用于验证策略模组；
- 私有化部署，确保策略安全；

## 适用人群
专业操作手、全栈技术爱好者、小型私募技术团队

## 产品架构：  
项目架构采用事件驱动+插件式开发
![Image](https://images.gitee.com/uploads/images/2021/1107/172130_9da2bdcd_1676852.png)

**开源不易，感谢点赞关注加收藏！**  
**详细文档请参考wiki： https://gitee.com/dromara/northstar/wikis/**

## 实盘注意事项
为了更好地了解实盘用户的使用情况，程序对期货公司做了一定的管理，如需要进行实盘交易，请联系作者咨询。

## 社群支持
![输入图片说明](https://images.gitee.com/uploads/images/2022/0107/210113_21d2977f_1676852.jpeg "微信图片_20220107210039.jpg")

## 运行环境
建议使用Linux云服务器，或者Windows系统（MAC系统没有试过，需自行摸索）

## 程序架构
- B/S架构
- northstar项目为服务端（包含了web网页监控端）
- northstar-external为用户自行扩展的项目，可参考[此项目](https://gitee.com/NorthstarQuan/northstar-external)
- 交互协议HTTP + websocket
- 数据库为Redis
- 前端采用node14 + vue2.x
- 服务端采用java17（拥抱新技术） + springboot

## 启动步骤
假设当前环境是全新的服务器  

### 环境准备（只需要运行一次）
**Linux环境下**，初始化系统环境。
```
curl https://gitee.com/dromara/northstar/raw/master/env.sh | sh
```

**Windows环境下**，初始化系统环境。打开powershell命令行窗口，输入以下命令
```
Invoke-WebRequest https://gitee.com/dromara/northstar/raw/master/env.ps1 -OutFile env.ps1; powershell -noexit ".\env.ps1"
```

### 程序包准备
下载最新版项目
[https://gitee.com/dromara/northstar/releases](https://gitee.com/dromara/northstar/releases)

**Linux环境下**
```
cd ~ && wget https://gitee.com/dromara/northstar/attach_files/1008262/download/northstar-3.5.jar
```

**Windows环境下**
```
Invoke-WebRequest https://gitee.com/dromara/northstar/attach_files/1008262/download/northstar-3.5.jar -OutFile northstar.jar
```

### 启动参数准备

**Linux环境下**
在.bashrc中加入启动参数（这样做能隐藏启动参数。若不用邮件通知与不修改默认的登陆账户密码可以不填）  
```
vim ~/.bashrc
```
在文末加入以下设置
```
...
export NSUSER=<登陆用户名>
export NSPWD=<登陆密码>
export SMTP_HOST=<邮箱SMTP，例如smtp.126.com>
export SMTP_SECRET=<邮箱认证码，在邮箱设置中生成的认证码>
export EMAIL=<代理邮箱名> 
export EMAIL0=<订阅邮箱名>
```
保存并退出，然后让配置生效
```
source ~/.bashrc
```

**Windows环境下**
待补充


### 启动程序
**Linux环境下**
```
curl https://gitee.com/dromara/northstar/raw/master/startup.sh | sh
```
注意：startup.sh脚本中包括了JVM的启动参数，假定服务器配置是2核4G，如有不同应该按实际情况自定义启动脚本

**Windows环境下**
待补充

### 部署验证
在浏览器直接访问部署服务的域名（端口使用了默认的80端口）， 应该可以看到以下界面，并可以登陆成功  
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
初始化系统环境（只需要运行一次），打开powershell命令行窗口，输入以下命令
```
Invoke-WebRequest https://gitee.com/dromara/northstar/raw/master/env.ps1 -OutFile env.ps1; powershell -noexit ".\env.ps1"
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