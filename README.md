 Northstar盈富量化平台

**免责声明：**  
**本项目仅属于技术分享，不构成任何交易建议。使用者自身在交易前，需要清楚其可能面对的交易风险与相关法律规定，并为自身行为负责！**

![多端图表分析](https://foruda.gitee.com/images/1774058878046188583/c3896234_1676852.png "屏幕截图")

![历史回放](https://foruda.gitee.com/images/1774059802333287433/2cd1aef0_1676852.gif "屏幕截图")

![自动化](https://foruda.gitee.com/images/1774060471531122258/c86a5fd5_1676852.png "屏幕截图")

![策略监控](https://foruda.gitee.com/images/1774060637481944200/93d230bb_1676852.png "屏幕截图")

## 产品简介
专业级量化交易软件，用于期货、股票、外汇、炒币等多种交易场景，实现自动交易。  
[【演示版网站】](https://northstar.quantit.tech:8443/#/)

已对接的网关示例：
- CTP网关：[https://gitee.com/NorthstarQuant/northstar-gateway-ctp](https://gitee.com/NorthstarQuant/northstar-gateway-ctp)
- 老虎网关：[https://gitee.com/NorthstarQuant/northstar-gateway-tiger](https://gitee.com/NorthstarQuant/northstar-gateway-tiger)
- 币安网关：[https://gitee.com/NorthstarQuant/northstar-gateway-binance](https://gitee.com/NorthstarQuant/northstar-gateway-binance)

**程序化交易**  
● 模组管理  
● 策略编写（JAVA）  
● 指标框架  
● 运行状态可视化  
● 自动风控  
● 资金隔离  
● 策略绩效  
● 策略熔断  

**模拟账户交易**  
● 支持实时行情的本地模拟（同时支持手工及程序化交易）  
● 支持历史回放的本地模拟（相当于复盘训练）  
● 提供账户绩效统计  

**历史行情回放**  
● 对接了六大主流交易市场（国内期货、A股、港股、美股、币圈、外汇）  
● 提供分钟级精度历史行情  

**前端GUI监控台**  
● 提供单租户的监控台  
● 方便配置网关、模组信息  
● 提供图表可视化  
● 方便日志查询  
● 支持中英文切换  

**AI助手**  
● 可对接主流的大模型平台API  
● 已有Deepseek及Kimi的对接案例  

**日志分析**  
● 隔离平台日志与模组日志  
● 可通过监控台实时控制日志打印级别  

**策略回测**  
● 可实现大规模的批量回测  
● 可实现快速回测  
● 可实现高精度的回测

**图表分析**  
● 提供多图同列  
● 提供多周期联动  
● 提供常用指标与画线工具  
● 支持自定义指标（Javascript脚本）
● 支持画线下单

**报警通知**  
● 对接了企业微信与邮件通知

**自定义网关**  
● 可扩展自行对接更多交易所

**私有化部署**


## 适用人群
专业量化操盘手、全栈技术爱好者、小型私募技术团队

## 运行环境
建议使用Linux云服务器，或者Windows系统（MAC系统不支持CTP、XTP动态库）

## 程序架构
- B/S架构
- northstar项目为服务端（包含了web网页监控端）
- 数据库采用H2（历史行情数据主要依赖数据服务，本地仅保存少量账户配置信息）

项目架构采用事件驱动+插件式开发
![输入图片说明](https://foruda.gitee.com/images/1684034911905355451/683de173_1676852.png "总体架构图")

## 技术支持
![输入图片说明](https://foruda.gitee.com/images/1774021779966463771/d5bfc03f_1676852.jpg "知识星球")

## 注意事项
- 更多详细文档请加星球获取
- 服务器时间校正为北京时间，时间不准会影响行情接收
- 尽量不要在开市期间重启程序，因为行情是实时接收的，重启会导致当天的K线数据会缺失
- 编写策略逻辑时如需使用时间属性，务必使用TICK行情自带的时间戳，否则策略回测时会不准确
- 本项目为技术分享，对交易行为并不负责
- 使用者需要自行开发交易策略并需要一定的JAVA基础

## 特别鸣谢
[redtorch](https://github.com/sun0x00/redtorch)作者，本项目保留了小部分其源码，同时感谢redtorch作者的技术分享。  
[klinechart](https://klinecharts.com/zh-CN)作者，提供了优秀的K线前端库，并提供了相关的技术支持。    
