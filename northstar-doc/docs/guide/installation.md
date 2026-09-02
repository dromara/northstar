# 安装与启动

本文介绍如何在 Linux / Windows 服务器上从零部署 Northstar 平台。

## 环境准备

### 环境要求

**硬件最低配置**

| 项目 | 要求 |
|---|---|
| CPU | 2 核 |
| 内存 | 4G |
| 磁盘 | 10G |
| 网络 | 100Mbps |

**软件依赖**

| 项目 | 要求 |
|---|---|
| JDK | 25（9.1.x 版本；旧版本为 JDK 21） |

### 环境脚本安装

执行对应的环境安装脚本，自动安装环境依赖（JDK）。

#### Linux 环境

```bash
curl https://pro.quantit.tech/assets/env.sh | sh
```

#### Windows 环境

打开 `powershell` 命令行环境，输入以下命令：

```powershell
Invoke-WebRequest https://pro.quantit.tech/assets/env.ps1 -OutFile env.ps1; powershell -noexit ".\env.ps1"
```

::: tip
假如执行报错，需要把脚本下载到本地后右键选择 powershell 执行。
:::

### 安装后的路径说明

- **Linux** 环境下，根目录是 `~/`
- **Windows** 环境下，根目录是 `c:/`

安装完成后的目录结构：

```
.
├── northstar-dist      # 程序目录
└── northstar-env       # 运行环境目录
```

### 软件包下载

下载地址：https://gitee.com/dromara/northstar/releases

下载后，放置到上述 `northstar-dist` 目录。最终的目录结构如下：

```
.
├── northstar-dist                               # 程序目录
│   ├── northstar-<version>.jar                  # 主程序
│   ├── northstar-external-<version>.jar         # 策略包（非必需）
│   └── northstar-gateway-xxx-<version>.jar      # 实盘网关包（非必需）
└── northstar-env                                # 运行环境目录
```

::: info 关于策略包与实盘网关包
- **策略包** 是自行实现的，具体可参考知识星球里的策略教程专栏；
- **实盘网关包** 地址（注意看版本说明）：
  - [CTP](https://gitee.com/dromara/northstar-gateway-ctp)【已经过生产环境验证】
  - [TIGER](https://gitee.com/dromara/northstar-gateway-tiger)（仅提供代码示例，需自行完善）
  - [BINANCE](https://gitee.com/dromara/northstar-gateway-binance)【已经过生产环境验证】
- 不使用实盘交易时，策略包与网关包均非必需，平台内置的 PLAYBACK 与 SIM 渠道开箱即用。
:::

## 程序启动

在 `northstar-dist` 目录下，使用 `java -jar` 便可以启动程序：

```bash
java -jar "-Dloader.path=$(pwd)" -Denv=prod northstar-<版本号>.jar
```

::: warning 注意事项
- 要加上环境参数 `-Denv=prod`
- 要指定外置依赖路径 `"-Dloader.path=$(pwd)"`（`$(pwd)` 是用于输出当前路径的命令，**注意引号**），策略包与网关包依靠它加载
- 版本号按实际填，例如 `northstar-9.1.0.jar`
- 若使用 Windows 系统，以上命令必须用 `powershell` 执行，**切勿用 `cmd` 命令行窗口**
:::

启动后，通过浏览器访问：

- 若在本地启动，访问 `https://localhost/`
- 若在服务器启动，访问 `https://你的服务器IP/`

启动成功后即可看到登录页，使用默认账号 `admin` / `123456` 登录（详见 [快速上手](/guide/getting-started)）。

### 日志查询

可以在以下路径找到程序运行日志：

```
.
├── northstar-dist   # 程序目录
│   ├── logs         # 运行日志目录
│   └── data         # 数据文件目录
└── northstar-env    # 运行环境目录
```

## 设置数据服务权限

程序在初始状态下，是**没有任何历史数据**的。在以下场景下，程序需要用到历史数据：

- 历史回放
- 行情复盘
- 自动化交易

历史数据权限**按市场划分**，分别为：

- 国内期货
- A 股
- 港股
- 美股
- 币圈
- 外汇黄金

### 付费使用说明

付费后，用户可以拿到一串**数据服务 token**，然后按以下方式设置即可。

#### Windows 环境

1. 按下组合功能键 `win + R`，输入 `powershell`，然后确定；
2. 在打开的命令行界面执行：

```powershell
setx NS_DS_SECRET <数据服务token>
```

::: warning
**配置完成后，重启电脑才能生效。**
:::

#### Linux 环境

执行：

```bash
vim ~/.bashrc
```

在文末加入以下设置：

```bash
export NS_DS_SECRET=<数据服务token>
```

保存并退出，然后让配置生效：

```bash
source ~/.bashrc
```

设置完成后重启程序，首页的"已购服务"卡片即可看到订阅的数据服务与到期日。

## 更多教程与社区交流

- 策略开发教程：知识星球策略教程专栏
- 使用过程中有任何问题，欢迎在官方交流群提问
![星球海报](/images/星球海报.png)
