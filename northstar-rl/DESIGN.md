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
    /** 动作→下单翻译，训练与实盘共用同一份逻辑，保证训推一致 */
    void onAction(TradeAction action, IModuleStrategyContext ctx, Contract contract);
    /** 决策点触发条件：每根 bar 结束后由环境评估（必须廉价，纯指标判断，不做模型推理） */
    boolean shouldObserve(IModuleStrategyContext ctx, Contract contract);
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

### 5.2 采样架构：任务—Runner—环境三层结构 + 数据缓存

**结构**：**一个训练任务对应一个 RLTrainingRunner；Runner 持有 1..n 个 TradingEnvironment（每合约一个）；每个 TradingEnvironment 只组合一对 MarketReplay × ModuleSandbox**：

- 一个 TradingEnvironment 就是一个"单合约世界"，gym 语义（reset/step/done）天然属于单环境，多合约是调度层的概念；
- **MarketReplay 只绑定一个合约**，数据来源于合约绑定的 dataSource（`IContract.dataSource`），加载逻辑复用 `PlaybackContext` 的分页加载机制；
- **ModuleSandbox 对应一个模组沙箱**，一个模组只绑定一个 agent 实例（与实盘模组的设计逻辑一致）——因此 RLAgent 实例随 Environment 一一绑定；
- DJL Predictor 非线程安全，每 Environment 独立 RLAgent 实例天然满足约束；串行采样下任意时刻只有一个 Environment 活跃。

**训练目标唯一**：一个训练任务训练的是**同一个 model**——不同合约可看作同一游戏里的不同挑战，所有 Environment 产生的 transition 汇入**同一份经验集**（ExperienceStore 任务级共享），Python 每轮在同一批汇聚数据上更新同一份权重。推论：

- 各 Environment 的 RLAgent 实例只是同一 `model.pt` 的多个只读副本（规避 Predictor 线程安全问题），**每轮训练完成后必须同步刷新全部实例的权重**；
- 刷新方式须规避文件锁：训练产物先写临时文件，各 Agent 实例关闭旧句柄后原子切换（Windows 上直接覆盖在用文件会失败）。

**数据加载与缓存**：RL 训练会反复重跑同一批数据（episode 数 × 轮数），I/O 必须前置：

- **任务启动时并行加载**各合约全时段数据，缓存于各 Environment 的 MarketReplay 内（Bar 数组）；之后所有 episode 的 `reset()` 只移动游标，**零 I/O**；
- 内存估算：M1 bar ≈ 100B，3 年 ≈ 25 万根 ≈ 25MB/合约，10 合约 ≈ 250MB，可接受；超出时按合约分段加载 + LRU 置换（v2）；
- 跨任务共享缓存（key = unifiedSymbol + 时段）列入 v2，v1 为任务级缓存；
- 指标预热逐 episode 重算（指标对象无法简单快照），成本 = 预热 bar 数 × episode 数；如需优化再引入指标快照机制（v2）。

**采样调度**：Runner 串行遍历 Environment（内存恒定），每个 Environment 内随机时间切片跑 episodes；各合约的 episode 配额默认均分，可按数据量加权；首轮无模型时纯随机冷启动。

### 5.3 行情回放与随机定位（MarketReplay）

MarketReplay 对外只有两个时间控制方法，设计底线是**只做游标算术，永远不做 I/O、不知道任何 RL 概念**。

**`locate(timeSlice, seed)`——随机定位一个 episode 切片**：

- 随机切片选择：episode 起点在训练时段内随机取值，随机源显式传入 `Random(seed)`——**seed 记入版本 metadata，同一 seed 可完全复现采样序列**（出问题可回放排查；换 seed 重训可做最低成本的稳健性检验）；
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
3. 数据层：数据缓存从**预热起始日**开始加载；创建任务时静态校验"预热时段 bar 数 ≥ warmupBars"，不满足直接拒绝——预热不足从运行期事故变成配置期错误；
4. 判定层：预热完成的判据是**指标全部进入稳定态**（复用实盘 unstable bar 机制），数够根数只是必要条件。

### 5.4 经验数据：增量读取

- 契约沿用 PoC 的 CSV 格式（`s...,action,reward,ns...,dt,terminated`，Python 按 stateDim 通用切分；`dt` 为决策间隔 bar 数，见 §5.5）；
- **Python 侧增量读取**：持久化 replay buffer（如 .npy），每轮只读新增分片，避免"每轮全量加载 CSV"的 O(n²) I/O；
- DQN 追加进持久 replay buffer；PPO on-policy 每轮样本训完即弃——差异收敛在 `ExperienceStore` 实现里，调度循环不变。

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

## 6. 可观测与动态调参

### 6.1 轮边界调参

训练按轮推进，每轮开始前重读任务最新超参数——**UI 修改参数下一轮生效**（语义清晰，避免一轮训练中途状态撕裂）。epsilon/lr 衰减轨迹由"当前轮数 + 配置起止值"在每轮边界计算，改参即改轨迹。

### 6.2 每轮指标（RLRoundMetrics）

| 类别 | 指标 |
|---|---|
| 采样侧 | round、epsilon、平均奖励、累计回报、平均决策间隔（avg bars per decision，观测触发条件松紧） |
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
| 训推行为一致 | observe/shouldObserve/onAction 钩子训练与实盘共用同一代码路径 |
| 合约间状态隔离 | 每合约独立 TradingEnvironment（Replay×Sandbox×Agent 各一） |
| 经验汇聚唯一模型 | ExperienceStore 任务级共享；各 Agent 实例同源，每轮同步刷新权重 |

## 10. v1 范围与延后项

**v1 范围**：DQN、事件驱动决策点（shouldObserve 触发）、指数合约采样、显式验证时段、人工确认上线。

**v2 延后**：

- PPO（字段清单机制已预留，训练侧需实现 ppo/ 包与 on-policy ExperienceStore）；
- Tick 级推理（需实测 native 调用开销）；
- 实盘经验持续学习（live 采样）；
- 多任务并发训练的资源仲裁（v1 强制串行队列）。
