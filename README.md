# Northstar盈富量化交易软件

**开源声明：**  
**本项目归入dromara开源组织运营的初心，是希望可以有更多志同道合的朋友一起参与项目的开发，并且能借其在交易市场上有所收获！**  
**借用组织的口号：一个人或许能走的更快，但一群人会走的更远。**  
**本项目仅属于技术分享，不构成任何交易建议。使用者自身在交易前，需要清楚其可能面对的交易风险与相关法律规定，并为自身行为负责！**

这是一个用户可以自行编写交易策略程序的量化交易软件。
用户监控台效果（监控台仅用于给用户提供一个可视化窗口，以方便进行程序的监控与管理）：
![输入图片说明](https://images.gitee.com/uploads/images/2022/0619/230500_fe02aedd_1676852.png "屏幕截图.png")
![输入图片说明](https://images.gitee.com/uploads/images/2022/0619/230527_2e868183_1676852.png "屏幕截图.png")
![输入图片说明](https://images.gitee.com/uploads/images/2022/0619/230813_a3991d60_1676852.png "屏幕截图.png")
![输入图片说明](https://images.gitee.com/uploads/images/2022/0620/105911_4e5622ee_1676852.png "屏幕截图.png")
![Image](https://images.gitee.com/uploads/images/2021/0606/220710_eeab5dd9_1676852.png)
![Image](https://images.gitee.com/uploads/images/2021/0606/220728_32ef6b37_1676852.png)

## 产品简介
这是一个面向程序员的开源高频量化交易软件，用于期货、股票、外汇、炒币等多种交易场景，实现自动交易。暂时只对接了国内期货交易所，理论上可以对接任意交易所。

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
**详细文档请参考 [【官网文档】](https://northstar-doc-1gwbhfxd522a1168-1310148562.ap-shanghai.app.tcloudbase.com/)**

## 实盘注意事项
为了更好地了解实盘用户的使用情况，程序对期货公司做了一定的管理，如需要进行实盘交易，请联系作者咨询。

## 运行环境
建议使用Linux云服务器，或者Windows系统（MAC系统没有试过，需自行摸索）

## 程序架构
- B/S架构
- northstar项目为服务端（包含了web网页监控端）
- 交互协议HTTP + websocket
- 数据库、缓存为Redis（历史行情数据主要依赖数据服务，本地仅保存少量账户配置信息）
- 前端采用node14 + vue2.x
- 服务端采用java17（拥抱新技术） + springboot2

项目架构采用事件驱动+插件式开发
![输入图片说明](https://images.gitee.com/uploads/images/2022/0721/205844_7985317e_1676852.png "总体架构图.png")

## 技术支持
[https://northstar-doc-1gwbhfxd522a1168-1310148562.ap-shanghai.app.tcloudbase.com/guide/chat-group.html](https://northstar-doc-1gwbhfxd522a1168-1310148562.ap-shanghai.app.tcloudbase.com/guide/chat-group.html)

## 注意事项
- 请使用前先通读一遍 [【官网文档】](https://northstar-doc-1gwbhfxd522a1168-1310148562.ap-shanghai.app.tcloudbase.com/)**
- 请勿直接使用master分支的最新代码，应该使用最新的tag来作为开发基线
- 服务器时间校正为北京时间
- 尽量不要在开市期间重启程序
- 编写策略逻辑时如需使用时间属性，务必使用TICK行情自带的时间戳，否则策略回测时会不准确
- 本项目为量化爱好者及JAVA开发者搭建，对交易行为并不负责
- 使用者需要自行开发交易策略并需要一定的JAVA基础

## 特别鸣谢
[redtorch项目](https://github.com/sun0x00/redtorch)作者。本项目演化自redtorch，并保留了小部分其源码，同时感谢redtorch作者的技术分享。