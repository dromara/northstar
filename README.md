# Northstar盈富量化平台

**免责声明：**  
**本项目仅属于技术分享，不构成任何交易建议。使用者自身在交易前，需要清楚其可能面对的交易风险与相关法律规定，并为自身行为负责！**

![输入图片说明](https://foruda.gitee.com/images/1681303466386742642/f1c0a30e_1676852.jpeg "GVP.jpg")

## 产品简介
这是一个面向程序员的量化交易软件，用于期货、股票、外汇、炒币等多种交易场景，实现自动交易。

已对接的网关示例：
- CTP网关：[https://gitee.com/NorthstarQuant/northstar-gateway-ctp](https://gitee.com/NorthstarQuant/northstar-gateway-ctp)
- 老虎网关：[https://gitee.com/NorthstarQuant/northstar-gateway-tiger](https://gitee.com/NorthstarQuant/northstar-gateway-tiger)
- OKX网关：[https://gitee.com/NorthstarQuant/northstar-gateway-okx](https://gitee.com/NorthstarQuant/northstar-gateway-okx)
- 币安网关：[https://gitee.com/NorthstarQuant/northstar-gateway-binance](https://gitee.com/NorthstarQuant/northstar-gateway-binance)

**功能特性：**
- 一站式平台，可适配对接不同的交易所；
- 灵活多变的自动化策略框架，能实现复杂的个性化交易逻辑，如多合约价差交易，算法高频交易，CTA交易，期权期货混合交易等等；
- 支持多账户交易，能实现跨市套利等复杂逻辑；
- 直观易理解的API编程接口，并且提供了多种策略的编写范例，只需要掌握最基本的JAVA编程知识便可以上手编写自己的交易策略；
- 支持高精度历史行情回放，便于操盘手进行回放训练，或用于验证策略模组；
- 自然易操作的自动化模组管理，轻松掌握与管理自动化策略的运行状态；
- 可实现完全自主的风控手段；
- 私有化部署，确保策略安全；

#### 用户监控台效果（监控台为用户提供一个可视化窗口，以方便进行程序的监控与管理）：
![输入图片说明](https://www.quantit.tech/assets/screenshot/preview-feature.gif "屏幕截图.png")
#### 策略可视化研发（可进行多周期叠加及自定义指标）：
![输入图片说明](https://www.quantit.tech/assets/screenshot/preview-strategy-study.gif "屏幕截图.png")
#### 策略回测：
![输入图片说明](https://www.quantit.tech/assets/screenshot/preview-playback.gif "屏幕截图.png")

## 适用人群
专业量化操盘手、全栈技术爱好者、小型私募技术团队

**详细文档请参考 [【官网文档】](https://www.quantit.tech/)**

## 运行环境
建议使用Linux云服务器，或者Windows系统（MAC系统不支持CTP、XTP动态库）

## 程序架构
- B/S架构
- northstar项目为服务端（包含了web网页监控端）
- 交互协议HTTP + websocket
- 数据库采用H2（历史行情数据主要依赖数据服务，本地仅保存少量账户配置信息）
- 前端监控台采用node14 + vue2.x
- 服务端采用java21 + springboot3

项目架构采用事件驱动+插件式开发
![输入图片说明](https://foruda.gitee.com/images/1684034911905355451/683de173_1676852.png "总体架构图")

## 技术支持
![](https://foruda.gitee.com/images/1674909142629211020/033daa70_1676852.jpeg "微信公众号.jpg")

## 注意事项
- 请使用前先通读一遍 [【官网文档】](https://www.quantit.tech/)  
- 请勿直接使用master分支的最新代码，应该使用最新的tag来作为开发基线
- 服务器时间校正为北京时间，时间不准会影响行情接收
- 尽量不要在开市期间重启程序，因为行情是实时接收的，重启会导致当天的K线数据会缺失
- 编写策略逻辑时如需使用时间属性，务必使用TICK行情自带的时间戳，否则策略回测时会不准确
- 本项目为技术分享，对交易行为并不负责
- 使用者需要自行开发交易策略并需要一定的JAVA基础

## 如何贡献代码
本项目欢迎提PR，可先fork到自己的项目中，然后提PR。为避免PR被拒绝，建议PR之前与作者进行充分的沟通

## 特别鸣谢
[redtorch](https://github.com/sun0x00/redtorch)作者，本项目保留了小部分其源码，同时感谢redtorch作者的技术分享。  
[klinechart](https://klinecharts.com/zh-CN)作者，提供了优秀的K线前端库，并提供了相关的技术支持。  
[electron-egg](https://www.yuque.com/u34495/mivcfg)作者，提供了简便易于上手的桌面化生成方案，并提供了相关的技术支持。  
