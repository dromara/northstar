# Northstar 强化学习交易设计文档

> 本文档汇总 RL 交易能力的完整设计，包括模型定义、训练架构、可观测性、版本血缘与前端交互。
> 设计基于 `northstar-rl` PoC 的既有链路，并结合批判性评审的结论收敛而成。

## 1. 背景与目标

为交易策略增加强化学习能力：交易模组通过加载预训练模型提升交易与盈利能力。

三条核心设计原则：

1. **Model 与 Strategy 强绑定**：模型的状态维度、动作空间、奖励函数由策略代码定义；一个模型可利用不同合约采样；模型分版本，支持在已有模型上再训练；神经网络由 Python 代码定义，Java 代码指定用哪个预定义模型（DQN/PPO）及具体参数。
2. **训练过程可观测、可动态调参**：学习率、gamma、epsilon 等可在 UI 动态调整，展示训练进程、累计回报等指标。
3. **训练效率**：一次训练任务允许定义多个合约进行训练。

## 2. 总体架构

沿用 PoC 验证过的「采样-训练分离」架构：

```
┌───────────────────────── Java（northstar）─────────────────────────┐
│  RLTrainingService（按轮驱动）                                       │
│    ├─ 采样器（串行，虚拟线程预取下一合约数据）→ 经验分片 CSV           │
│    ├─ python train_round.py（唯一跨语言边界，子进程无状态）           │
│    └─ 评估器（验证时段回测）→ 指标 → SSE 推送前端                     │
└──────────────────────────────┬──────────────────────────────────────┘
                               │ model 目录（文件契约）
┌──────────────────────────────┴──────────────────────────────────────┐
│  Python（PyTorch）                                                   │
│    train_round.py → 按 metadata.algo 分发到 dqn/ 或 ppo/ 包          │
│    读 checkpoint.pt + 经验数据 → 训练 → 存 checkpoint → 导出 model.pt │
└─────────────────────────────────────────────────────────────────────┘
```

- Java 负责：推理（DJL 加载 TorchScript）、采样、流程编排、任务生命周期、UI 服务；
- Python 负责：纯训练，无状态单轮子进程，崩溃不拖垮 JVM；
- 两侧通过 **model 目录**（`metadata.json` + `checkpoint.pt` + `model.pt` + 经验数据）解耦。

## 3. 模型定义

### 3.1 职责划分：Hyperparameter 类与 Metadata 类

| 类 | 定义时机 | 职责 |
|---|---|---|
| **Hyperparameter 类** | 定义 model 时 | 定义该模型的超参数**有哪些字段**（字段清单是模型定义的一部分）；复用 `DynamicParams` / `@Setting` / `ComponentField` 机制，UI 通过接口拿到字段清单后动态渲染表单，前端不硬编码任何超参数项 |
| **Metadata 类** | 每个版本一份，存 JSON | 承载该版本超参数的**具体值**；fork 再训练时从父版本 metadata 继承取值 |

算法差异（如 DQN 有 epsilon 三项、PPO 有裁剪率/熵系数）由各自的 Hyperparameter 类天然表达，前端零特判。

### 3.2 模型目录结构

```
northstar-rl/models/{modelId}/
├── metadata.json              # 模型级契约（Java/Python 共享，创建后不可变）
└── versions/
    └── v{n}/
        ├── metadata.json      # 版本级：血缘、超参取值、训练数据统计、评估指标
        ├── checkpoint.pt      # 训练态全量（policy/target/optimizer）
        ├── model.pt           # TorchScript 推理产物（DJL 加载）
        └── rounds/            # 每轮经验数据分片 replay-r{n}-c{m}.csv
```

### 3.3 模型级 metadata（不可变身份）

```json
{
  "name": "rb-trend-dqn",
  "strategyClass": "org.dromara.northstar.rl.strategy.RlTrendStrategy",
  "algo": "DQN",
  "stateDim": 12,
  "actionDim": 3,
  "actions": ["HOLD", "BUY", "SELL"],
  "network": { "hidden": [128, 128], "activation": "relu" },
  "preprocessing": { "type": "standardize", "mean": [], "std": [] },
  "specHash": "a3f9c21e...",
  "latestVersion": 3
}
```

**模型形状（strategyClass/algo/stateDim/actions/network）创建后终身不可变**；想改网络结构 = 创建新模型，而不是新版本。

### 3.4 版本级 metadata（出生证明）

```json
{
  "version": 3,
  "parentVersion": 2,
  "weightInherited": true,
  "specHash": "a3f9c21e...",
  "checkpointSignature": "sha256:...",
  "training": {
    "rounds": 30,
    "warmupStartDate": "2022-01-01",
    "trainStartDate": "2023-01-01",
    "validationStartDate": "2024-07-01",
    "contracts": ["rbINDEX", "cuINDEX"],
    "hyperParams": { "lr": 0.0002, "gamma": 0.99 },
    "acceptedBy": { "winRate": 0.53, "profitFactor": 2.2, "trades": 187 }
  }
}
```

任何一个线上模组绑定的模型版本，都能反推出：从哪个版本继承权重、用了什么特征定义、在哪个数据区间训练、凭什么被判定达标。

## 4. 策略侧接口（RLStrategy）

后端定义 `RLStrategy` 接口，**只有实现了该接口的策略才能被训练**（`GET /rl/strategies` 只返回这类策略）。策略代码定义模型的"形状"：

```java
public interface RLStrategy extends TradeStrategy {
    RLModelSpec rlSpec();                              // 状态特征清单 + 动作枚举 + 预处理类型
    float[] observe(IModuleStrategyContext ctx, Contract contract);  // 状态向量构建
    double reward(RLRewardContext ctx);                // 奖励函数（含手续费/成本扣除）
}

public record RLFeatureSpec(String name, NormalizerType normalizer, String description) {}

public record RLModelSpec(
    List<RLFeatureSpec> features,     // 顺序敏感，顺序即向量下标；stateDim = features.size()
    Class<? extends Enum<?>> actionEnum
) {}
```

要点：

- **量纲一致性**：状态特征必须是归一化/比例量（收益率、相对均线偏离、盈亏比），由 Translator 预处理保证与合约无关，多合约采样才成立；
- **RL 动作翻译成普通下单调用**：`ctx.submitOrderReq(...)`，既有风控过滤器、状态机、资金校验全部继续生效，RL 不绕过任何安全边界；
- **推理频率限定 Bar 级**（v1）：`onMergedBar` 中推理，避免 Tick 级 native 调用开销。

### 4.1 策略演进防护（specHash）

- `specHash` = 对 `features（name+normalizer，顺序敏感）+ actions + preprocessing` 稳定序列化后取 SHA-256 前 16 位，创建模型时写入 metadata；
- 四个校验时机全部快速失败：创建训练任务、fork 版本、模组加载模型推理、单元测试；
- 校验失败输出**结构化 diff**（哪个特征变了/新增了什么），不是只报 hash mismatch；
- 处置路径只有两条：有意变更 → 创建新模型；无意变更 → 按 diff 回退策略代码；
- 配套单测模板：固定输入快照 → `observe()` 输出与 golden 文件逐位比对，让无意识改动在开发期暴露。

## 5. 训练任务

### 5.1 任务定义（策略驱动）

训练任务基于 **strategy** 定义（参考新建模组的策略选择交互），字段：

| 字段 | 说明 |
|---|---|
| 策略 | 仅列出实现 RLStrategy 接口的策略 |
| 模型 | 按所选策略过滤；无可用模型时禁用并提示 |
| 基准版本 | "从零训练"(0) 或某历史版本；决定超参数预填值 |
| 合约 | ContractSelection 组件，多选；以**指数合约**为基准（规避具体合约到期问题） |
| 预热起始日 / 训练起始日 / 验证起始日 | 三个日期框定三段范围：预热=[预热起始, 训练起始)、训练=[训练起始, 验证起始)、验证=[验证起始, 数据末日]；校验严格递增 |
| 总轮数 / 每轮局数 | 训练预算 |
| 超参数 | 字段清单来自模型的 Hyperparameter 定义，值预填自基准版本 metadata |

三个起始日期的设计同时解决了：训练/验证集人为划分（防过拟合）、指标预热范围（episode 前必须有预热前缀，只喂数据不采样）。

### 5.2 采样架构：串行 + 预取

- 合约间**串行采样**控制内存（每个采样器持有独立 native 模型副本，串行使内存占用恒定为单模型）；
- 独立预取线程维护"下一合约行情数据"缓冲队列（容量 1~2 个合约），I/O 与采样计算重叠；
- 每合约独立环境实例与 DummyAccount，状态完全隔离；首轮无模型时纯随机冷启动。

### 5.3 经验数据：增量读取

- 契约沿用 PoC 的 CSV 格式（`s...,action,reward,ns...,terminated`，Python 按 stateDim 通用切分）；
- **Python 侧增量读取**：持久化 replay buffer（如 .npy），每轮只读新增分片，避免"每轮全量加载 CSV"的 O(n²) I/O；
- DQN 追加进持久 replay buffer；PPO on-policy 每轮样本训完即弃——差异收敛在 `ExperienceStore` 实现里，调度循环不变。

### 5.4 达标标准

验证时段回测上同时满足：

- 胜率 > 50%
- 平均盈亏比 > 2
- **交易笔数 ≥ 100**（统计显著性约束，防止小样本偶然达标）

数学口径：胜率 50% + 盈亏比 2 → 单笔期望 = 0.5×2R − 0.5×1R = 0.5R > 0（R 为平均亏损）。

## 6. 可观测与动态调参

### 6.1 轮边界调参

训练按轮推进，每轮开始前重读任务最新超参数——**UI 修改参数下一轮生效**（语义清晰，避免一轮训练中途状态撕裂）。epsilon/lr 衰减轨迹由"当前轮数 + 配置起止值"在每轮边界计算，改参即改轨迹。

### 6.2 每轮指标（RLRoundMetrics）

| 类别 | 指标 |
|---|---|
| 采样侧 | round、epsilon、平均奖励、累计回报 |
| 训练侧 | loss（Python stdout 结构化输出，Java 解析） |
| 评估侧（每 N 轮一次） | 验证集胜率、盈亏比、交易笔数、收益率 |
| 全局 | 当前 lr、进度（currentRound/totalRounds） |

推送通道复用现有事件引擎 + SSE。

### 6.3 REST 接口

| 端点 | 说明 |
|---|---|
| `GET /rl/strategies` | RL 策略列表（仅 RLStrategy 实现类） |
| `GET /rl/models` | 模型列表（含 hyperParamFields、versionDetails、network） |
| `POST /rl/training` | 创建并启动训练任务 |
| `GET /rl/training` | 任务列表 |
| `GET /rl/training/{taskId}` | 任务详情 + 每轮指标 |
| `PUT /rl/training/{taskId}/params` | 动态调整超参数（下一轮生效） |
| `GET /rl/training/{taskId}/pause` `/resume` `/stop` | 任务控制 |

## 7. 版本血缘与再训练

1. **模型形状不可变**（见 §3.3），从源头消除 90% 的 checkpoint 不兼容；
2. **fork 双重校验**：父版本 `checkpointSignature`（各层参数 shape 哈希 + optimizer 类型 + 累计步数）与新版本 network 配置 + 当前策略 specHash 静态比对，不一致拒绝 fork（409 + 差异详情）；
3. **显式降级路径**：`inheritWeights: false` → 不复制 checkpoint 从零训练，但继承特征定义与数据配置，血缘记录 `weightInherited: false`，语义诚实；
4. **预处理统计量冻结**：Translator 的 mean/std 在 v1 首次训练时计算并冻结；fork 默认沿用父版本统计量（保证输入分布一致，权重继承才有意义）；重算统计量必须与 `inheritWeights: false` 搭配，由后端强制；
5. **模组绑版本必须固定版本号**，不允许绑 latest；模型切换 = 模组显式换绑 + 重建（保证行为可复盘，同时规避 Windows 文件锁问题——版本目录只增不改）。

## 8. 前端设计（已实现 PoC）

### 8.1 页面结构

归属「交易自动化」菜单组（`/module/rl/training`），位于"策略探索"之后：

| 页面 | 说明 |
|---|---|
| 任务列表 | 状态标签、el-progress 进度、最新 avgReward/loss、操作（详情/暂停/继续/停止）；RUNNING 时 3s 轮询 |
| 新建任务弹窗 | 双列紧凑布局（720px），策略→模型→基准版本级联；超参数区按 hyperParamFields 动态渲染；label-width 110px + nowrap 保证中英文不换行 |
| 训练详情 | descriptions 任务信息（含版本血缘 v base→target tag、三段范围同一行、进度条）；4 张 ECharts 曲线（累计回报+平均奖励双轴 / loss / epsilon+lr 双轴 / 验证集胜率+盈亏比双轴）；每轮指标明细表；调参弹窗（同样动态渲染，提示"下一轮生效"） |

### 8.2 图表防重叠约定

- y 轴名称 `nameLocation: "middle"` + `nameGap: 50`（竖向置于轴中部，避免与左上角标题相撞）；
- 标题居左、图例居右（`right: 0`）；
- grid 上边距 60 给标题/图例留位。

## 9. 关键不变量汇总

| 不变量 | 保证机制 |
|---|---|
| 模型形状终身不变 | 模型级 metadata 不可变字段 + fork 只读继承 |
| 权重继承必然兼容 | checkpointSignature 静态校验 + 形状不可变 |
| 特征语义漂移必然暴露 | specHash 四处校验 + 结构化 diff + golden 测试 |
| 输入分布漂移受控 | 预处理统计量冻结 + 重算与权重继承互斥 |
| 量纲与合约无关 | 特征归一化由 Translator 保证 |
| 每个版本可复盘 | 版本 metadata 完整血缘记录 |
| "达标"有统计意义 | 验证时段评估 + 胜率/盈亏比/≥100 笔约束 |
| 超参字段与模型一致 | Hyperparameter 类定义字段清单，UI 动态渲染 |

## 10. v1 范围与延后项

**v1 范围**：DQN、Bar 级决策、指数合约采样、显式验证时段、人工确认上线。

**v2 延后**：

- PPO（字段清单机制已预留，训练侧需实现 ppo/ 包与 on-policy ExperienceStore）；
- Tick 级推理（需实测 native 调用开销）；
- 实盘经验持续学习（live 采样）；
- 多任务并发训练的资源仲裁（v1 强制串行队列）。
