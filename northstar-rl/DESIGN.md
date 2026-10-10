# Northstar 强化学习交易设计文档

> 本文档汇总 RL 交易能力的完整设计，包括模型定义、训练架构、可观测性、版本血缘与前端交互。
> 设计基于 `northstar-rl` PoC 的既有链路，并结合批判性评审的结论收敛而成。
> 正文即 v1 落地的实际契约（字段命名、状态机、评估口径等均与源码一致），不再有单独的"实现差异"附录。

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
│    ├─ 采样器（按合约环境串行，数据启动时预载缓存）→ 经验分片 CSV      │
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

**超参数键命名（v1 实际契约）**：键为 camelCase（`lr`、`gamma`、`epsilonStart`、`epsilonEnd`、`epsilonDecayRounds`、`batchSize`、`bufferCapacity`、`minBuffer`、`tau`、`maxGradSteps`），由 `DqnHyperParams`（`DynamicParams` 子类）统一定义，Java/Python/UI 三方共享同一套键。`minBuffer` 默认值三方（Java `DqnHyperParams`、Python 兜底、种子 metadata）统一为 500。

### 3.2 模型目录结构

```
northstar-rl/models/{modelId}/
├── metadata.json              # 模型级传输载荷（Java 拥有、可重建；非身份载体）
└── versions/
    └── v{n}/
        ├── metadata.json      # 版本级：血缘、超参取值、训练数据统计、评估指标、网络结构（Python 回写）
        ├── checkpoint.pt      # 训练态全量（policy/target/optimizer）
        ├── model.pt           # TorchScript 推理产物（DJL 加载）
        ├── buffer.npz         # 持久化 replay buffer（Python 跨轮状态）
        ├── ingested.json      # 已入库经验分片清单（增量读取依据）
        └── rounds/            # 每轮经验数据分片 replay-r{n}-c{m}.csv
```

版本目录内的全部内容（metadata/checkpoint/model/buffer/ingested/rounds）都是**版本级长期资产**（归属原则见 §5.2「数据归属原则」）。

**JSON 字段命名（v1 实际契约）**：为与 PoC 及 Python 训练侧既有代码保持一致，模型/版本 metadata 的 JSON 字段采用 snake_case（如 `state_dim`、`strategy_class`、`spec_hash`、`latest_version`、`hyper_params`）；REST 层 VO 仍为 camelCase。

### 3.3 模型级 metadata（传输协议）

```json
{
  "name": "dqn-demo",
  "strategy_class": "org.dromara.northstar.rl.strategy.RlTestStrategy",
  "algo": "DQN",
  "state_dim": 6,
  "action_dim": 3,
  "actions": ["HOLD", "BUY", "SELL"],
  "features": [
    { "name": "ret1", "normalizer": "RATIO" },
    { "name": "position", "normalizer": "NONE" }
  ],
  "preprocessing": { "type": "identity", "mean": [], "std": [] },
  "spec_hash": "91de2fdfbc70a327",
  "latest_version": 3
}
```

**模型级 metadata 不是身份载体，而是传输协议**：定义权在代码（策略现场 spec + Python 按模型名的网络注册表），本文件仅是 Java 写出、Python 读入、可随时重建的载荷。任何生产路径不得因其缺失/陈旧而失败：

- **缺失即 bootstrap**：`RLModelStore.syncModel(strategy)` 以策略现场 spec 引导生成（`latest_version=0`，`preprocessing` 只写 type）；创建训练任务等入口一律先 sync，不再要求 metadata 预置；
- **陈旧即重写**：文件中的 `spec_hash` 与现场 spec 不一致时，重写全部可变字段，**保留 `latest_version` 与 `preprocessing.mean/std`**（版本资产指针与训练侧统计量不得丢失）；
- 模型名 = 网络结构身份：`RLStrategy.modelName()` 声明（如 `dqn-demo`），模型目录名与 `name` 字段均取自该值；Python 端以同名 key 在 `registry.py` 注册网络构建函数，**network 结构字段不再出现在模型级 metadata**，改由 Python 每轮训练后回写到版本级 metadata（§3.4）；
- 训练算法 = `RLStrategy.rlAlgo()` 声明（`RLAlgo` 枚举，v1 仅 `DQN`，`PPO` 为 v2 预留），与模型名正交（同一网络主干可被不同算法复用）；`algo` 随 bootstrap/重写写入本文件，Java 超参注册表（`RLAlgorithms`）、Java 推理侧（`RLAgent`）与 Python `train_round.py` 均据其分发；**algo 是身份级字段**：`syncModel` 检出持久化值与策略声明不一致即快速失败（checkpoint 结构与超参历史按算法解释，变更算法须换新 modelName 建新模型），不随 spec 陈旧重写而变化；
- 旧版文件遗留的 `network` 字段由 Gson 默认忽略，加载不报错；`features` 持久化特征清单（名称+归一化方式，顺序敏感），供 fork 校验失败时输出结构化 diff（见 §4.1），旧版 metadata 无该字段时按空清单处理。

### 3.4 版本级 metadata（出生证明）

```json
{
  "version": 3,
  "parent_version": 2,
  "weight_inherited": true,
  "spec_hash": "91de2fdfbc70a327",
  "network": { "type": "mlp", "hidden": [128, 128], "activation": "relu" },
  "checkpoint_signature": "sha256:...",
  "preprocessing": { "type": "standardize", "mean": [...], "std": [...] },
  "training": {
    "rounds": 30,
    "warmup_start_date": "2022-01-01",
    "train_start_date": "2023-01-01",
    "validation_start_date": "2024-07-01",
    "contracts": ["rbINDEX", "cuINDEX"],
    "seed": 1234567890,
    "trigger_summary": "策略=..., 决策点超时强触发=240bar, 权益击穿阈值=0.5, 单局最长=720bar",
    "hyper_params": { "lr": 0.0002, "gamma": 0.99 }
  },
  "accepted_by": { "winRate": 0.53, "profitFactor": 2.2, "trades": 187 }
}
```

任何一个线上模组绑定的模型版本，都能反推出：从哪个版本继承权重、用了什么特征定义、在哪个数据区间训练、用什么随机种子与触发条件、凭什么被判定达标。注意：`accepted_by` 仅在任务完结时最后一次评估**满足 §5.6 达标标准**才写入（未达标的版本没有该字段）；版本级 `preprocessing` 为冻结后的预处理统计量（fork 时沿用父版本，见 §7.4）；`network` 由 Python 每轮训练后写入，Java 当作不透明数据读取并透传到 `GET /rl/models` 的 VO（模型尚无已训练版本时该字段缺省，VO 的 network 为 null）。

## 4. 策略侧接口（RLStrategy）

后端定义 `RLStrategy` 接口，**只有实现了该接口的策略才能被训练**（`GET /rl/strategies` 只返回这类策略）。策略代码定义模型的"形状"：

```java
public interface RLStrategy extends TradeStrategy {
    String modelName();                                // 模型名 = 网络结构身份（Python registry.py 同名注册键）
    RLModelSpec rlSpec();                              // 状态特征清单 + 动作枚举 + 预处理类型
    float[] observe(IModuleStrategyContext ctx, Contract contract);  // 状态向量构建
    /** 动作→下单翻译，训练与实盘共用同一份逻辑，保证训推一致 */
    void onAction(TradeAction action, IModuleStrategyContext ctx, Contract contract);
    /** 决策点触发条件：每根 bar 结束后由环境评估（必须廉价，纯指标判断，不做模型推理） */
    boolean shouldObserve(IModuleStrategyContext ctx, Contract contract);
    double reward(RLRewardContext ctx);                // 奖励函数（含手续费/成本扣除）
}

public record RLFeatureSpec(String name, NormalizerType normalizer, String description) {}

public record RLModelSpec(
    List<RLFeatureSpec> features,     // 顺序敏感，顺序即向量下标；stateDim = features.size()
    Class<? extends Enum<?>> actionEnum,
    PreprocessType preprocessing      // 预处理类型（identity / standardize / custom）
) {}
```

接口另有两个带默认实现的方法：`default int rlWarmupBars() { return 60; }`（指标预热所需 bar 数，创建训练任务时静态校验预热时段 bar 数不低于该值，见 §5.3）；`default RLStatePreprocessor rlPreprocessor()`（**仅 `preprocessing = CUSTOM` 时被调用**，默认实现快速失败——声明 CUSTOM 却不提供预处理器会在创建训练任务时（syncModel 校验）暴露。

要点：

- **量纲一致性**：状态特征必须是归一化/比例量（收益率、相对均线偏离、盈亏比），由 Translator 预处理保证与合约无关，多合约采样才成立；
- **预处理三取值的执行位置**（变换必须恰好发生一次，训推两侧不得遗漏或重复）：`identity` 不做任何变换；`standardize` 由 Python 训练侧在经验入 buffer 前用冻结统计量变换（推理侧 Java Translator 用同一份 mean/std）；`custom` 完全在 Java 侧——`TradingEnvironment` 落盘 transition 前用策略的 `RLStatePreprocessor` 变换（Python 对 custom 直通），推理侧 `RLAgent` 的 Translator 进网络前用同一份变换；custom 变换必须**无状态、确定性、保长、不改入参**（需要数据相关统计量请改用 standardize），metadata 只记录 `type: "custom"`，变换逻辑本身不可重建、随策略代码演进（specHash 钉类型，实现漂移由 observe golden 测试同款手段兜底）。
- **RL 动作翻译成普通下单调用**：`ctx.submitOrderReq(...)`，既有风控过滤器、状态机、资金校验全部继续生效，RL 不绕过任何安全边界；
- **训推一致**：`observe()` / `shouldObserve()` / `onAction()` 三个钩子在训练环境与实盘模组中执行完全相同的代码路径；实盘 `onMergedBar` 里先判 `shouldObserve()`，为 true 才走 `observe() → act → onAction()`；
- **探索与簿记归环境**：ε-greedy 在动作枚举层面由环境拦截，transition 记录由环境写入，策略零 RL 簿记负担。

`RLRewardContext` 是环境在每个决策点构造的"结算单"（事件驱动语义下，奖励按决策区间而非逐 bar 计算）：

| 字段 | 提供者 | 用途 |
|---|---|---|
| `prevEquity` / `currEquity` | ModuleSandbox | 区间收益主成分 = `currEquity − prevEquity` |
| `trades`（区间成交记录） | ModuleSandbox | **显式扣除交易成本**——回放是"最新价立即全成"，不扣成本模型会学到高频刷单策略 |
| `dt`（间隔 bar 数） | MarketReplay 游标 | ① 奖励整形参考；② 写入 transition 供训练侧 `γ^dt` 折扣 |
| `episodeEnd` | 环境 | 区分 truncated（数据耗尽，正常切完）与 terminated（权益击穿，真失败，通常配重罚） |

策略的 `reward()` 是纯函数计算（不碰游标、不记账），止损只是普通决策点，不得与 terminated 混淆。

### 4.1 策略演进防护（specHash）

- `specHash` = 对 `features（name+normalizer，顺序敏感）+ actions + preprocessing` 稳定序列化后取 SHA-256 前 16 位（**不含模型名**），写入模型级与版本级 metadata；
- 模型级 metadata 是传输协议而非身份载体，因此**创建训练任务不再因 specHash 不一致失败**：入口先 `syncModel`，陈旧 metadata 被现场 spec 重写（保留 `latest_version` 与预处理统计量，见 §3.3）；
- 快速失败的校验时机收敛到三处：**fork 版本**（`baseVersion > 0` 时父版本 specHash 必须与策略现场规格一致，否则拒绝 fork）、**模组加载模型推理**、**单元测试**（golden specHash）；
- 校验失败输出**结构化 diff**（哪个特征变了/新增了什么），不是只报 hash mismatch——模型级 metadata 持久化了特征清单（§3.3 的 `features` 字段），校验点据此与当前策略规格逐项比对；旧版 metadata 无特征清单时退化为 hash 提示；
- 处置路径只有两条：有意变更 → 以新规格从零训练新版本（或创建新模型）；无意变更 → 按 diff 回退策略代码；
- 配套单测模板：固定输入快照 → `observe()` 输出与 golden 快照逐位比对（specHash 只钉形状，observe 的特征计算逻辑漂移由 golden 测试兜底），让无意识改动在开发期暴露。

## 5. 训练任务

### 5.1 任务定义（策略驱动）

训练任务基于 **strategy** 定义（参考新建模组的策略选择交互），字段：

| 字段 | 说明 |
|---|---|
| 策略 | 仅列出实现 RLStrategy 接口的策略 |
| 模型 | 与策略一一绑定（modelId 必须等于策略的 modelName()）；metadata 无需预置，创建任务时由策略现场 spec 自动 bootstrap/重写（§3.3） |
| 基准版本 | "从零训练"(0) 或某历史版本；决定超参数预填值 |
| 权重继承 | `inheritWeights`：基准版本 > 0 时默认 true（复制父版本 checkpoint）；显式 false = 不复制 checkpoint 从零训练，但继承特征定义与数据配置（见 §7.3） |
| 合约 | ContractSelection 组件，多选；以**指数合约**为基准（规避具体合约到期问题）。与策略探索一致，前端 `ContractSelection` 使用 `channelType="PLAYBACK"`（回放网关订阅的指数合约），不新增合约查询接口，不改动 `ContractSelection` 组件；后端按 unifiedSymbol 从数据服务解析合约并加载历史数据 |
| 预热起始日 / 训练起始日 / 验证起始日 | 三个日期框定三段范围：预热=[预热起始, 训练起始)、训练=[训练起始, 验证起始)、验证=[验证起始, 数据末日]；校验严格递增 |
| 总轮数 / 每轮局数 | 训练预算 |
| 沙箱初始资金 | 每局初始资金（initBalance > 0） |
| 决策点超时强触发 bar 数 | maxHoldingBars ≥ 1，纯事件触发的失效兜底（见 §5.5） |
| 权益击穿阈值 | ruinThreshold ∈ (0, 1)，权益跌破初始资金 × 阈值判 terminated |
| 单局最长 bar 数 | maxEpisodeBars ≥ 1 |
| 超参数 | 字段清单来自模型的 Hyperparameter 定义（见 §3.1 的键命名契约），值预填自基准版本 metadata |

三个起始日期的设计同时解决了：训练/验证集人为划分（防过拟合）、指标预热范围（episode 前必须有预热前缀，只喂数据不采样）。

**任务持久化**：任务落 `RL_TRAINING_TASK` 表（taskId + dataStr JSON，含每轮指标列表），模式同 `EXPLORER_TASK`。

### 5.2 采样架构：任务—Runner—环境三层结构 + 数据缓存

**结构**：**一个训练任务对应一个 RLTrainingRunner；Runner 持有 1..n 个 TradingEnvironment（每合约一个）；每个 TradingEnvironment 只组合一对 MarketReplay × ModuleSandbox**：

- 一个 TradingEnvironment 就是一个"单合约世界"，gym 语义（reset/step/done）天然属于单环境，多合约是调度层的概念；
- **MarketReplay 只绑定一个合约**，数据来源于合约绑定的 dataSource（`IContract.dataSource`），由 `RLTrainingDataManager` 按周分页自实现加载（未复用 `PlaybackContext`）；
- **ModuleSandbox 对应一个模组沙箱**，一个模组只绑定一个 agent 实例（与实盘模组的设计逻辑一致）——因此 RLAgent 实例随 Environment 一一绑定；
- DJL Predictor 非线程安全，每 Environment 独立 RLAgent 实例天然满足约束；串行采样下任意时刻只有一个 Environment 活跃。

**训练目标唯一**：一个训练任务训练的是**同一个 model**——不同合约可看作同一游戏里的不同挑战，所有 Environment 产生的 transition 汇入**同一份经验集**（ExperienceStore 任务级共享），Python 每轮在同一批汇聚数据上更新同一份权重。推论：

- 各 Environment 的 RLAgent 实例只是同一 `model.pt` 的多个只读副本（规避 Predictor 线程安全问题），**每轮训练完成后必须同步刷新全部实例的权重**；
- 刷新方式须规避文件锁：训练产物先写临时文件，各 Agent 实例关闭旧句柄后原子切换（Windows 上直接覆盖在用文件会失败）。

**数据加载与缓存**：RL 训练会反复重跑同一批数据（episode 数 × 轮数），I/O 必须前置：

- **数据加载独立于任务创建与训练启动**：任务创建后需显式触发数据加载，各合约全时段数据从数据源一次性拉取并落地为本地 CSV（`<data-root>/<taskId>/<unifiedSymbol>.csv`，写临时文件后原子改名，文件存在即数据完整）；已存在的合约文件直接跳过，**任务数据仅加载一次，程序重启后仍然有效**（断点续传）；
- **训练启动时从 CSV 读入**并缓存于各 Environment 的 MarketReplay 内（Bar 数组）；之后所有 episode 的 `reset()` 只移动游标，**零 I/O**；
- 内存估算：M1 bar ≈ 100B，3 年 ≈ 25 万根 ≈ 25MB/合约，10 合约 ≈ 250MB，可接受；超出时按合约分段加载 + LRU 置换（v2）；
- 跨任务共享缓存（key = unifiedSymbol + 时段）列入 v2，v1 为任务级缓存；
- 指标预热逐 episode 重算（指标对象无法简单快照），成本 = 预热 bar 数 × episode 数；如需优化再引入指标快照机制（v2）。

**数据归属原则（任务级 vs 版本级）**：任务不拥有版本，任务只是产出一个版本的过程。数据按生命周期划分存放位置：

- **任务级（临时生产资料）→ 任务目录** `<data-root>/<taskId>/`：只有行情 CSV。它是本任务从数据源拉取的输入副本，任务删除即删；
- **版本级（长期资产）→ 版本目录** `models/{modelId}/versions/v{n}/`：checkpoint.pt、model.pt、rounds/ 经验分片、buffer.npz、ingested.json、版本 metadata。它们回答的是"这个版本是怎么训练出来的"，任务终结（FINISHED/FAILED）后仍需留存，供后续任务 fork 继承、模组绑定、血缘复盘。

训练产物不放任务目录的理由：① model 是跨任务迭代的长期资产，一个 model 会经历多次训练任务进化，产物归任务会导致版本随任务删除而丢失；② 断点续训要求 rounds/ + buffer.npz + ingested.json + checkpoint.pt 四处跨轮状态在同一版本目录内自洽（train_round.py 全部从版本目录读取），拆到任务目录会破坏这份自洽性；③ 模组按固定路径绑定版本目录，要求版本路径长期稳定、只增不改；④ 失败任务的部分训练成果保留后可被后续任务作为基准版本继承。

**采样调度**：Runner 串行遍历 Environment（内存恒定），每个 Environment 内随机时间切片跑 episodes；各合约的 episode 配额按余数分配（`base = 每轮局数 / 环境数`，前 `每轮局数 % 环境数` 个环境各 +1，配额为 0 的环境跳过），可按数据量加权；首轮无模型时纯随机冷启动。

### 5.3 行情回放与随机定位（MarketReplay）

MarketReplay 对外只有两个时间控制方法，设计底线是**只做游标算术，永远不做 I/O、不知道任何 RL 概念**。

**`locate(startIdx, length, warmupBars)`——定位一个 episode 切片**（随机起点的选取在 TradingEnvironment 采样层完成，MarketReplay 只做定位）：

- 随机切片选择：episode 起点由 TradingEnvironment 在训练时段内随机取值，随机源显式传入 `Random(seed)`——**seed 记入版本 metadata，同一 seed 可完全复现采样序列**（出问题可回放排查；换 seed 重训可做最低成本的稳健性检验）。seed 取值限定在 [0, 2^32−1]（Python 端 numpy 播种只允许该范围）：Java 端生成时即取 32 位无符号值，轮次派生 `roundSeed = (seed + round) & 0xFFFFFFFF`；Python 端 trainer 对传入种子再做一次 `& 0xFFFFFFFF` 规整，兼容存量 metadata 中的 64 位种子；
- 预热前缀回溯：游标落在 `episodeStart − warmupBars` 而非起点本身，预热段只喂数据不采样、不计 dt、不计奖励；
- 边界保护：历史前缀不足时 clamp 或重选切片。

**`advanceBar()`——显式单步推进**，返回三态：

```java
enum AdvanceResult {
    WARMUP,     // 预热段：bar 喂给指标递推，不产生观测、不允许下单、不记录经验
    OK,         // 正式段：正常推进
    EXHAUSTED   // 数据耗尽 → truncated（正常结束，不是失败）
}
```

采用显式步进而非迭代器/流，因为环境需要在每根 bar 之后插入 `shouldObserve()` 判定与强制触发检查，节奏控制权必须在环境手里。

**为什么起点随机而不是每次回到同一时点**：历史回放是确定性数据，固定起点等于把同一条轨迹给模型背诵——loss 照降、回报照涨、换时段即作废（CartPole 的随机初始状态在死数据上的等价物就是随机切片）。随机起点同时保证状态分布覆盖率与 replay buffer 样本多样性。配套原则是**训练随机、评估固定**：验证评估每轮在同一验证时段跑贪心策略，轮间指标才严格可比。

**预热保证（四层收口）**：

1. 游标层：locate 回溯 warmupBars 根，预热段 `WARMUP` 语义隔离；
2. 配置层：`warmupBars = max(策略各指标回看周期) × 安全系数`（EMA/ATR 等递推指标需 2~3 倍 burn-in），与实盘模组的 `weeksOfDataForPreparation` 语义对齐；
3. 数据层：数据缓存从**预热起始日**开始加载；启动训练时静态校验"预热时段 bar 数 ≥ warmupBars"，不满足直接拒绝——预热不足从运行期事故变成配置期错误；
4. 判定层：episode 正式段的**初始决策点**推迟到策略 `shouldObserve()` 首次返回 true（或距首个正式 bar 达到 maxHoldingBars 强触发上限）——`shouldObserve()` 内含策略的指标就绪守卫（如 RlTestStrategy 的 `isReady()`），指标未稳定时不会采到第一批 transition；数够根数只是必要条件。

### 5.4 经验数据：增量读取

- 契约沿用 PoC 的 CSV 格式（`s...,action,reward,ns...,dt,terminated`，Python 按 stateDim 通用切分；`dt` 为决策间隔 bar 数，见 §5.5）；
- **Python 侧增量读取**：持久化 replay buffer（buffer.npz + ingested.json 记录已入库分片清单），每轮只读新增分片，避免"每轮全量加载 CSV"的 O(n²) I/O；
- DQN 追加进持久 replay buffer；PPO on-policy 每轮样本训完即弃——Java 侧 `ExperienceStore` 是算法无关的分片 writer，算法差异收敛在 Python 训练侧（`train_round.py` 按 metadata.algo 分发到各自算法包），调度循环不变；
- **原子写**：全部跨轮状态文件（checkpoint.pt、model.pt、buffer.npz、ingested.json、版本/模型 metadata.json）都先写临时文件再原子替换，规避 Windows 文件锁与崩溃半截写；
- Python 侧测试位于 `training/tests/`（pytest），使用 env 脚本准备的 rl-venv 运行：`cd northstar-rl/training && ~/northstar-env/rl-venv/bin/python -m pytest tests/`（Windows 为 `c:\northstar_env\rl-venv\Scripts\python`）。

### 5.5 决策点触发机制（事件驱动决策 / SMDP）

决策不按固定 bar 频率触发，而由策略通过 `shouldObserve()` 定义"何时需要重新观测"（SMDP / Options 框架：一个动作持续到终止条件满足，决策点事件驱动）。典型场景：动作为做多且方向正确时，整段波段不触发监测；出现"涨不动"迹象或止损触发时，才记录一次状态转移。

**环境主循环**（`TradingEnvironment.step()` 语义从"推进一根 bar"变为"执行动作，推进到下一个决策点"）：

```
s = observe(); a = agent(s)（训练时 ε-greedy）; onAction(a) 下单
loop:
    推进一根 bar（指标照常更新）      // 指标是递推的，被跳过的 bar 仍需正常喂数
    累积该 bar 的浮动盈亏
    if shouldObserve() 或 强制触发点: break
s' = observe(); R = 区间累计收益 - 交易成本
记录 transition (s, a, R, s', dt, terminated)
```

**收益与代价**：

- 省下的是**模型推理开销**（触发点才跑神经网络）与**经验数据量**（transition 只记录在决策点，replay buffer 大幅缩小）；
- 省不掉**回放本身**：指标递推依赖连续 bar 序列，数据加载、bar 合成、指标更新的开销仍在。

**训练侧配套改动**：

- 经验 CSV 增加 `dt` 列（间隔 bar 数）；DQN target 计算改为 `R + γ^dt · max Q(s')`——不定长转移必须按 `γ^Δt` 折扣，否则"持有 5 根赚 100"与"持有 50 根赚 100"被视为等价，模型学不到时间价值；
- **强制触发点**（纯事件触发的失效兜底）：
  - 超时触发：maxHoldingBars（如 240 根），防死行情里永不触发导致数据饿死；
  - 回合边界：数据耗尽时强制平仓并记录最后一条 transition（truncated 语义）；
  - 风控触发：权益击穿阈值为 terminated（真失败）；止损只是普通决策点，两者不得混淆。

  结算口径细节：权益击穿分支的奖励按强平前权益计算，强制平仓本身的手续费不计入该区间的 `trades`（强平是环境的清算动作，不是策略的决策结果）。

**风险与监控**：

- 触发条件松紧直接决定训练数据密度：太松饿死训练，太紧退化为 bar 级决策。训练指标中加入**平均决策间隔（avg bars per decision）**推送到 UI，作为触发条件合理性的直接观测；
- 事件触发天然偏向"有波动"的状态（选择偏差），平淡行情下模型行为未经训练；实盘与训练使用同一 `shouldObserve()`，行为一致，风险可控；
- 触发条件属策略语义、不进 specHash（它改变转移语义而非模型输入输出形状），但训练时的触发条件摘要需记入版本 metadata 保证可复盘。

### 5.6 达标标准

验证时段回测上同时满足：

- 胜率 > 50%
- 平均盈亏比 > 2
- **交易笔数 ≥ 100**（统计显著性约束，防止小样本偶然达标）

数学口径：胜率 50% + 盈亏比 2 → 单笔期望 = 0.5×2R − 0.5×1R = 0.5R > 0（R 为平均亏损）。

**统计口径**：多合约的成交记录汇总后统一计算（与回测口径一致：一笔交易 = 一对开平仓，FIFO 配对）；盈亏比 = 平均盈利 / 平均亏损绝对值。

该判定由后端强制执行：任务完结时取**最后一次验证评估**，三条同时满足才把评估指标写入版本 metadata 的 `accepted_by`（未达标的版本没有 `accepted_by` 字段）；无亏损交易时盈亏比记为 `null`（分母为 0 不可比，序列化为字符串 "Infinity" 会损坏前端），判定上"无亏损且有盈利"视为盈亏比达标。「上线」为人工动作：模组显式绑定固定版本目录（策略参数 `推理模型版本目录`）。

## 6. 可观测与动态调参

### 6.1 轮边界调参与串行队列

训练按轮推进，每轮开始前重读任务最新超参数——**UI 修改参数下一轮生效**（语义清晰，避免一轮训练中途状态撕裂）。epsilon/lr 衰减轨迹由"当前轮数 + 配置起止值"在每轮边界计算，改参即改轨迹。

多任务通过**全局信号量按轮串行执行**；暂停中的任务不占用训练资源（轮边界释放信号量）。

### 6.2 每轮指标（RLRoundMetrics）

| 类别 | 指标 |
|---|---|
| 采样侧 | round、epsilon、平均奖励、累计回报、平均决策间隔（avg bars per decision，观测触发条件松紧） |
| 训练侧 | loss（Python stdout 结构化输出，Java 解析） |
| 评估侧（每 5 轮 + 最后一轮一次） | 验证集胜率、盈亏比、交易笔数、收益率 |
| 全局 | 当前 lr、进度（currentRound/totalRounds） |

推送通道：Runner 轮边界经 `EventEmitterHolder` 直接发 SSE（事件名 `RL_TRAINING`），不经 Disruptor 事件引擎。评估侧指标无数据（评估未触发或无成交）时为 `null`，前端留空显示。

### 6.3 REST 接口

| 端点 | 说明 |
|---|---|
| `GET /rl/strategies` | RL 策略列表（仅 RLStrategy 实现类） |
| `GET /rl/models` | 模型列表（含 hyperParamFields、versionDetails、network；network 为最新版本 metadata 中 Python 回写的结构信息，无已训练版本时为 null） |
| `POST /rl/training` | 创建训练任务（仅参数校验与版本 fork，不加载数据、不启动训练，初始状态 CREATED） |
| `GET /rl/training` | 任务列表 |
| `GET /rl/training/{taskId}` | 任务详情 + 每轮指标 |
| `GET /rl/training/{taskId}/load-data` | 异步加载训练数据（落地 CSV，已存在则跳过；LOADING → READY） |
| `GET /rl/training/{taskId}/start` | 启动训练（仅 READY 状态可启动，Runner 只读 CSV） |
| `PUT /rl/training/{taskId}/params` | 动态调整超参数（下一轮生效；FINISHED 拒绝、FAILED 允许——修复问题后重试前可调；取值须为有限数值） |
| `GET /rl/training/{taskId}/pause` `/resume` `/stop` | 任务控制 |
| `GET /rl/training/{taskId}/retry` | 重试失败任务（清理失败轮经验分片后从 `currentRound` 断点续训，见 §6.4） |
| `DELETE /rl/training/{taskId}` | 删除任务（RUNNING/PAUSED 需先停止；删除顺序与版本回收规则见 §6.4） |

### 6.4 任务状态机与故障恢复

**三段式生命周期（创建 → 数据加载 → 启动训练）**：

- `createTask` 只做参数/策略/模型绑定校验（modelId 必须等于策略 modelName）与版本 fork：入口先 `syncModel` 确保模型级 metadata 与现场 spec 一致（缺失即 bootstrap、陈旧即重写，§3.3），落盘后任务处于 `CREATED`；落库失败回收 fork 的版本目录并回退 `latest_version`，回收失败以 suppressed 异常透出；
- `loadData` 异步逐合约从数据源拉取数据并落地 CSV（已存在则跳过），全部就绪进入 `READY`；任一失败回到 `CREATED` 并记录原因（可重试，断点续传）；
- `start` 仅接受 `READY` 任务，同步构建训练环境（读 CSV + 预热/时段静态校验），失败则任务置 `FAILED` 并透出原因。

**服务重启恢复**：checkpoint 与经验分片均已持久化在版本目录，重启后 RUNNING 任务标记为 PAUSED、LOADING 任务标记回 `CREATED`；`resume` 时同步重建 runner 从 `currentRound` 继续（同样只读 CSV），重建失败直接报错。

**失败重试**（`retry`，仅接受 FAILED）：训练循环任一轮抛异常即置 FAILED 并记录原因。retry 先丢弃失败轮（`currentRound`）的全部经验分片并把对应文件名从 ingested.json 移除（分片按 轮次×合约 命名，ExperienceStore 为 APPEND 语义，不清理会重复追加、且已 ingested 的旧文件名会遮蔽重采样分片），再重建 runner 从 `currentRound` 续训——采样序列由 (seed, round) 确定，重试可复现。checkpoint/buffer 由 Python 原子写保证一致，失败轮不会残留半提交训练状态；若失败前 buffer 已持久化该轮样本（buffer 已存但 ingested 未更新的崩溃窗口），重试后该轮样本重复入库一次，与进程崩溃的 at-least-once 语义一致。重试启动失败保持 FAILED 并更新原因，可再次重试。FAILED 任务亦允许 `updateParams`（修复超参等问题后重试）。

**任务删除**（仅拒绝 RUNNING/PAUSED，需先停止）：

- LOADING 任务通过取消标记协作式终止加载循环并自清残留文件；
- 未开始训练的任务（CREATED/LOADING/READY）删除时清理 CSV 数据目录并回收 fork 的版本（与创建失败语义一致）；版本回收支持非 LIFO 顺序——回收非最新版本时 `latest_version` 不变，回收最新版本时回退至剩余最大版本号；
- 已训练/失败的任务保留版本目录（训练成果可能被模组绑定或被后续任务作为基准版本引用）；
- 删除按「等待训练循环退出 → 删数据 → 回收版本 → 删记录」顺序执行，任何一步失败都会抛出并中止后续步骤（任务保留、可重试，不留"记录已删但版本/数据残留"的半提交状态）；
- 异步线程（加载/训练循环）的滞后落库与 SSE 推送通过存在性检查抑制，已删任务不会复活。

## 7. 版本血缘与再训练

1. **模型输入/输出形状由策略代码定义**（见 §3.3/§4，模型名 + 现场 spec），从源头消除 90% 的 checkpoint 不兼容；
2. **fork 校验**：父版本 specHash 必须与策略现场规格一致，且 `inheritWeights: true` 时父版本必须存在已训练的 checkpoint，不一致拒绝 fork（409 + 结构化差异详情）。`checkpoint_signature`（各层参数 shape 哈希 + optimizer 类型 + 累计步数）由 Python 每轮训练后更新，用于标识训练连续性；因模型名（网络结构身份）与 specHash 均不可漂移，network 比对待签名校验是恒真式，故 fork 处不做签名的静态比对（v1 的刻意弱化）；
3. **显式降级路径**：`inheritWeights: false` → 不复制 checkpoint 从零训练，但继承特征定义与数据配置，血缘记录 `weight_inherited: false`，语义诚实；
4. **预处理统计量永远冻结**：Translator 的 mean/std 在 v1 首次训练时计算并冻结；fork 一律沿用父版本统计量（保证输入分布一致，权重继承才有意义），**不提供重算入口**——想换统计量 = 创建新模型（v1 刻意收敛，比"重算与 inheritWeights 互斥"更保守）；`custom` 预处理无统计量概念（变换是策略代码，不冻结、随策略演进），fork 语义同 identity；
5. **模组绑版本必须固定版本号**，不允许绑 latest；模型切换 = 模组显式换绑 + 重建（保证行为可复盘，同时规避 Windows 文件锁问题——版本目录只增不改）。

## 8. 前端设计（已实现 PoC）

### 8.1 页面结构

归属「交易自动化」菜单组（`/module/rl/training`），位于"策略探索"之后：

| 页面 | 说明 |
|---|---|
| 任务列表 | 状态标签（待加载/数据加载中/待训练/运行中/已暂停/已完成/失败）、el-progress 进度、最新 avgReward/loss、操作（加载数据/开始训练/详情/暂停/继续/停止）；RUNNING 或 LOADING 时 3s 轮询 |
| 新建任务弹窗 | 双列紧凑布局（720px），策略→模型→基准版本级联；超参数区按 hyperParamFields 动态渲染；label-width 110px + nowrap 保证中英文不换行 |
| 训练详情 | descriptions 任务信息（含版本血缘 v base→target tag、三段范围同一行、进度条）；4 张 ECharts 曲线（累计回报+平均奖励双轴 / loss / epsilon+lr 双轴 / 验证集胜率+盈亏比双轴）；每轮指标明细表；调参弹窗（同样动态渲染，提示"下一轮生效"） |

### 8.2 图表防重叠约定

- y 轴名称 `nameLocation: "middle"` + `nameGap: 50`（竖向置于轴中部，避免与左上角标题相撞）；
- 标题居左、图例居右（`right: 0`）；
- grid 上边距 60 给标题/图例留位。

## 9. 关键不变量汇总

| 不变量 | 保证机制 |
|---|---|
| 模型名与 specHash 定义网络与输入形状 | 定义权在代码：RLStrategy.modelName() + rlSpec()；metadata 仅传输载荷，缺失可 bootstrap、陈旧可重写（§3.3） |
| 权重继承必然兼容 | 模型形状不可变 + fork 校验父版本 specHash 与 checkpoint 存在性 |
| 特征语义漂移必然暴露 | specHash 四处校验 + 结构化 diff（metadata 持久化特征清单）+ observe golden 测试 |
| 输入分布漂移受控 | 预处理统计量永远冻结，不提供重算入口 |
| 自定义预处理训推一致 | CUSTOM 变换仅存在于 Java 策略代码：训练落盘前与推理进网络前各执行一次，Python 直通；声明 CUSTOM 不提供预处理器时创建期（syncModel）快速失败 |
| 量纲与合约无关 | 特征归一化由 Translator 保证 |
| 每个版本可复盘 | 版本 metadata 完整血缘记录 |
| "达标"有统计意义 | 验证时段评估 + 胜率/盈亏比/≥100 笔约束 |
| 超参字段与模型一致 | Hyperparameter 类定义字段清单，UI 动态渲染 |
| 训推行为一致 | observe/shouldObserve/onAction 钩子训练与实盘共用同一代码路径 |
| 合约间状态隔离 | 每合约独立 TradingEnvironment（Replay×Sandbox×Agent 各一） |
| 经验汇聚唯一模型 | ExperienceStore 任务级共享；各 Agent 实例同源，每轮同步刷新权重 |
| 任务数据与模型资产分离 | 行情 CSV 归任务目录随任务删除；训练产物（checkpoint/model/rounds/buffer）归版本目录随版本保留（§5.2 数据归属原则） |
| 崩溃不产生半截文件 | 全部跨轮状态文件临时文件 + 原子替换（§5.4） |
| 任务删除无半提交状态 | 删数据 → 回收版本 → 删记录顺序执行，任一步失败即中止（§6.4） |

## 10. v1 范围与延后项

**v1 范围**：DQN、事件驱动决策点（shouldObserve 触发）、指数合约采样、显式验证时段、人工确认上线。

**v2 延后**：

- PPO（字段清单机制已预留，训练侧需实现 ppo/ 包与 on-policy ExperienceStore）；
- Tick 级推理（需实测 native 调用开销）；
- 实盘经验持续学习（live 采样）；
- 多任务并发训练的资源仲裁（v1 强制串行队列）。
