# northstar-copilot

AI 助手模块，基于 **Spring AI 2.0**（配套 Spring Boot 4.x）搭建大模型接入框架。

## 设计说明

Kimi、DeepSeek、MiniMax、GLM、OpenAI 均提供 **OpenAI 兼容** 的 Chat Completions 接口，
因此框架统一使用 Spring AI 的 OpenAI ChatModel 接入，各平台仅 `baseUrl` / `apiKey` / `model` 不同：

| 平台 | baseUrl | 默认模型 | API Key 环境变量 | 模型覆盖环境变量 |
|---|---|---|---|---|
| Kimi（月之暗面） | `https://api.moonshot.cn/v1` | `kimi-k2-0905-preview` | `KIMI_API_KEY` | `KIMI_MODEL` |
| DeepSeek | `https://api.deepseek.com/v1` | `deepseek-chat` | `DEEPSEEK_API_KEY` | `DEEPSEEK_MODEL` |
| MiniMax（国内站） | `https://api.minimaxi.com/v1` | `MiniMax-M2` | `MINIMAX_API_KEY` | `MINIMAX_MODEL` |
| GLM（智谱） | `https://open.bigmodel.cn/api/paas/v4` | `glm-4-flash` | `GLM_API_KEY` | `GLM_MODEL` |
| OpenAI | `https://api.openai.com/v1` | `gpt-4o-mini` | `OPENAI_API_KEY` | `OPENAI_MODEL` |

> MiniMax 国际站请将 baseUrl 换成 `https://api.minimax.io/v1`；默认模型如已下线，可用对应 `*_MODEL` 环境变量覆盖。

## 核心类（`org.dromara.northstar.copilot.llm`）

- `LlmProvider` —— 平台枚举，封装 baseUrl、默认模型、环境变量名
- `LlmChatClientFactory` —— 按平台构建 Spring AI `ChatClient` / `ChatModel`

## 调用示例

```java
// 同步调用（API Key 取自环境变量）
ChatClient client = LlmChatClientFactory.create(LlmProvider.DEEPSEEK);
String answer = client.prompt()
        .system("你是一个量化交易助手")
        .user("用一句话解释什么是移动平均线")
        .call()
        .content();

// 流式调用
Flux<String> stream = client.prompt().user("你好").stream().content();

// 显式指定 Key 与模型
ChatClient kimi = LlmChatClientFactory.create(LlmProvider.KIMI, "sk-xxx", "kimi-k2-0905-preview");
```

## 测试

`src/test/java/.../llm/` 下每个平台一个测试类（`KimiChatClientTest`、`DeepSeekChatClientTest`、
`MiniMaxChatClientTest`、`GlmChatClientTest`、`OpenAiChatClientTest`），演示同步 / 流式 / 完整响应三种调用方式。

测试通过 `@EnabledIfEnvironmentVariable` 控制：**未配置对应平台 API Key 时自动跳过**，配置后即可真实调用：

```bash
# Windows PowerShell 示例
$env:DEEPSEEK_API_KEY="sk-xxx"
mvn -f northstar-copilot/pom.xml test -Dtest=DeepSeekChatClientTest
```
