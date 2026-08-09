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

工具调用相关（`org.dromara.northstar.copilot.tool`）：

- `CopilotTool` —— 工具类标记注解，配合 `ToolLoader` 实现运行时自动发现
- `ToolLoader` —— 基于 reflections 的包扫描工具加载器

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

## 工具调用（Tool Calling）

工具调用对所有 OpenAI 兼容平台（Kimi、DeepSeek、MiniMax、GLM、OpenAI）写法完全一致。

### 自定义一个工具

只需两步：在普通方法上加 `@Tool` 描述工具用途，用 `@ToolParam` 描述入参：

```java
public class ExchangeTools {

    @Tool(description = "查询指定期货交易所的每手交易手续费，返回单位为元")
    public double getCommissionRate(@ToolParam(description = "交易所代码，例如 SHFE、DCE") String exchange) {
        return switch (exchange.toUpperCase()) {
            case "SHFE" -> 3.0;
            case "DCE" -> 1.5;
            default -> -1;
        };
    }
}
```

注册后，模型会在需要时自动回调本地方法，并把返回结果融入回答：

```java
String content = chatClient.prompt()
        .user("上期所(SHFE)每手手续费是多少元？")
        .tools(new ExchangeTools())   // 注册工具
        .call()
        .content();
```

### `tools(...)` 接受的五种参数形态

`tools(Object...)` 是万能入口，会自动识别每个元素的类型并分派：

| 传入类型 | 处理方式 |
|---|---|
| `@Tool` 注解的 POJO | 扫描所有 `@Tool` 方法，每个方法生成一个回调 |
| `ToolCallback` | 原样直接注册 |
| `ToolCallbackProvider` | 注册时不解析，**请求发起时才懒加载**其回调 |
| `ToolCallback[]` / `ToolCallbackProvider[]` | 数组拆开，逐个按类型分派 |
| `Collection`（如 `List`） | 逐个遍历，逐个按类型分派 |

支持混合传参，并可用 `toolContext(...)` 携带上下文：

```java
chatClient.prompt()
        .user("...")
        .tools(new ExchangeTools(), existingCallback, myProvider)
        .toolContext(Map.of("tenantId", "acme"))
        .call()
        .content();
```

注意事项：

- 参数不能为 null、不能含 null 元素；POJO 中必须至少有一个 `@Tool` 方法，否则抛 `IllegalArgumentException`
- **同名工具不允许重复注册**（无论来自哪种形态），冲突时抛 `IllegalStateException` 并指出冲突的工具名

### 动态加载工具

工具数量多或需要插件化时，不必在使用处手动 `new`：给工具类加上 `@CopilotTool` 注解，
由 `ToolLoader` 在运行时扫描包并自动实例化：

```java
@CopilotTool
public class ExchangeTools { ... }

// 使用处：扫描加载指定包下的全部工具（返回 List，可直接传入 tools()）
chatClient.prompt()
        .user("...")
        .tools(ToolLoader.load("org.dromara.northstar.copilot"))
        .call()
        .content();
```

### 作用域：请求级 `tools()` vs 全局 `defaultTools()`

- `prompt().tools(...)` 注册的工具**仅对当前请求生效**，不会泄漏到后续请求
- `ChatClient.builder(chatModel).defaultTools(...)` 注册的工具对同一 Builder 构建的**每个请求**都生效，
  且与请求级工具是**叠加**关系：

```java
ChatClient client = ChatClient.builder(chatModel)
        .defaultTools(new ExchangeTools())   // 每个请求都可用
        .build();

client.prompt().user("...").call().content();                    // 只有 ExchangeTools
client.prompt().user("...").tools(new GreetingTools()).call()    // ExchangeTools + GreetingTools
        .content();
```

> 实现细节：Spring AI 只在模型的 options 属于 `ToolCallingChatOptions`（如 `OpenAiChatModel`）时，
> 才会把注册的工具合并进 Prompt；自定义 `ChatModel` 需让 `getOptions()` 返回 `ToolCallingChatOptions`。

## 测试

`src/test/java/.../llm/` 下每个平台一个测试类（`KimiChatClientTest`、`DeepSeekChatClientTest`、
`MiniMaxChatClientTest`、`GlmChatClientTest`、`OpenAiChatClientTest`），演示同步 / 流式 / 完整响应 / 工具调用 / 动态加载工具五种调用方式。

另有两个离线测试类（无需 API Key、不触网）：

- `LlmChatClientFactoryTest` —— 工厂构建、工具元数据注册与本地执行、动态加载扫描
- `ToolsRegistrationTest` —— 通过捕获 Prompt 的假 ChatModel，验证 `tools(...)` 五种参数形态的分派规则、
  同名工具冲突校验、请求级与 `defaultTools` 的作用域语义

工具示例类为 `ExchangeTools`（@Tool + @CopilotTool）。

测试通过 `@EnabledIfEnvironmentVariable` 控制：**未配置对应平台 API Key 时自动跳过**，配置后即可真实调用：

```bash
# Windows PowerShell 示例
$env:DEEPSEEK_API_KEY="sk-xxx"
mvn -f northstar-copilot/pom.xml test -Dtest=DeepSeekChatClientTest
```
