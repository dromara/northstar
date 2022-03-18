# Northstar 智盈高频量化交易平台网页监控端

**开源声明：**

**本项目归入 dromara 开源组织运营的初心，是希望可以有更多志同道合的朋友一起参与项目的开发，并且能借其在交易市场上有所收获！**  
**借用组织的口号：一个人或许能走的更快，但一群人会走的更远。**

本前端界面是对接 northstar 智盈高频量化交易平台用的，具体设计请移步  
https://gitee.com/dromara/northstar

## 使用说明

https://gitee.com/dromara/northstar-monitor/wikis

### 启动步骤

假设当前环境是全新的服务器
首先确保nodejs环境已经安装  

然后在版本发布页面 https://gitee.com/dromara/northstar-monitor/releases 找到最新版的 dist 压缩包下载，解压，进入目录，输入以下命令运行：
```
node bundle.js
```

### 本地模拟
假设你没有任何期货账户，也不要紧，Northstar智盈交易平台已经实现了本地行情及账户模拟，使你在不接通外部真实行情的情况下，都可以体验软件。  

请按以下步骤操作：
1. 在`[网关管理]`，新建网关，填入网关ID，网关类型选择`[SIM]`，保存
2. 在`[账户管理]`，新建账户，填入账户ID，账户类型选择`[SIM]`，行情网关选择刚刚新建的模拟网关，点开`[账户配置]`随便填，保存

这时模拟行情与模拟账户都已经自动处于`[连线]`状态，切换到手工交易，选择刚刚填入的模拟账户，选择合约，就应该可以见到行情跳动了。  



## 本地开发

项目目录包含了 express 作为前端服务，用作反向代理  
项目启动有两种模式：
一种是与 JAVA 服务端交互

```
npm run start
```

另一种是 mock 数据

```
npm run start-mock
```

目前的 Mock 数据还不完备，仅对少量接口提供了 mock 数据，可以自行添加  
详细添加方法可参考 server/mock/api 下的例子


## 参考项目
[KLineChart](https://github.com/liihuu/KLineChart)，一个很牛逼的K线控件库。
- 参考示例：https://liihuu.github.io/KLineChart/ （可能需要翻墙）
- 代码示例：https://github.com/liihuu/KLineChartSample
- 参考文档：https://www.yuque.com/liihuu/klinechart
