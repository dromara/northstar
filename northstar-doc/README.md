# Northstar 使用文档

Northstar 盈富量化交易平台的用户使用文档，基于 **VitePress** 构建为静态网站。

## 文档内容

| 章节 | 说明 |
|---|---|
| 安装与启动 | 环境准备、软件包下载、程序启动、日志查询、数据服务权限配置 |
| 平台简介与核心概念 | 网关、模组、策略等核心术语与整体架构 |
| 快速上手 | 登录、界面布局、菜单、快捷键 |
| ⏪ 历史回放 | 回放网关创建、回放控制面板、逐K/调速/时间线跳转 |
| 💹 模拟盘交易 | SIM 网关、出入金、手工下单、画线交易、账户绩效 |
| 🤖 程序化交易 | 模组管理、策略加载、运行时监控、策略探索批量回测 |
| 👥 多账户交易 | 网关即账户、多账户切换、一模组多账户、合约配置 |
| 🎯 操盘手训练 | 回放 + 模拟盘组合的复盘训练完整流程 |
| 附录 | 常见问题（FAQ）、选项速查表 |

## 使用方法

```bash
# 安装依赖
pnpm install

# 本地预览（开发模式，热更新）
pnpm dev

# 构建静态网站（输出到 docs/.vitepress/dist/）
pnpm build

# 预览构建产物
pnpm preview
```

## 部署

`pnpm build` 后的产物是纯静态文件（`docs/.vitepress/dist/`），可直接部署到任意静态服务器（Nginx、GitHub Pages、对象存储等），例如：

```bash
# 任意静态服务器均可，如：
npx serve docs/.vitepress/dist
```

## 目录结构

```
northstar-doc/
├── package.json            # 脚本与依赖（vitepress）
├── docs/
│   ├── index.md            # 首页
│   ├── .vitepress/
│   │   └── config.mts      # 站点配置（导航/侧边栏/搜索）
│   ├── guide/              # 入门
│   ├── features/           # 五大核心功能
│   └── appendix/           # FAQ 与速查表
└── README.md
```
