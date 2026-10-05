# northstar-rl：强化学习模块（DJL/PyTorch 推理 + Python 离线训练）

Northstar Pro 的强化学习模块。完整设计见 [DESIGN.md](DESIGN.md)（模型定义、训练架构、可观测性、版本血缘、前端交互）。
采用**采样-训练分离**架构：Java 负责推理/采样/流程编排，Python 负责纯训练（无状态单轮子进程），
两侧通过**模型目录**（metadata.json + checkpoint.pt + model.pt + rounds/ 经验分片）解耦。

## 架构

```
Java RLTrainingRunner（northstar-main，按轮驱动）
 ├── TradingEnvironment（每合约一个：MarketReplay × RLModuleSandbox × RLAgent）→ 经验分片 CSV
 ├── python train_round.py --model-dir <dir> --version <n>   # 唯一跨语言边界，子进程无状态
 └── 验证时段贪心评估 → 指标 → SSE/REST 推送前端
```

- **推理/采样侧（Java/DJL）**：`DqnAgent` 是与场景解耦的模型调用壳；`RLAgent` 是可刷新的版本目录绑定壳
  （每轮训练完成后同步刷新全部实例，先关句柄规避 Windows 文件锁）。
- **训练侧（Python/PyTorch）**：`training/` 下为无状态单轮离线训练器；网络结构由 `registry.py`
  按模型名注册（Python 端为唯一权威），按模型 metadata 的 `algo` 分发（v1 仅 DQN；PPO 延后 v2）。
  跨轮状态持久化在版本目录（checkpoint.pt + buffer.npz + ingested.json）。
- **策略 SPI（northstar-api `org.dromara.northstar.strategy.rl`）**：`RLStrategy` 接口 +
  `RLModelSpec`/`RLFeatureSpec`/`RLRewardContext`/`TradeAction`；只有实现 RLStrategy 的策略可被训练。

## 模块结构

```
northstar-rl/
├── models/
│   ├── cartpole/                 # 示例模型（版本化目录）：metadata.json + versions/v1/
│   └── rl-trend-dqn/             # RlTrendStrategy 的种子模型（metadata.json，latest_version=0）
├── training/                     # Python 训练侧
│   ├── train_round.py            # 单轮训练入口：按 algo 分发、增量读取分片、统计量冻结
│   ├── registry.py               # 模型注册表：按模型名定义网络结构（Python 端唯一权威）
│   ├── expstore.py               # 经验分片 CSV 读取（与 Java ExperienceStore 同一契约）
│   ├── dqn/                      # DQN 包（network/buffer/trainer，支持 dt 折扣与持久化 buffer）
│   └── tests/                    # pytest 单元与端到端测试
└── src/main/java/org/dromara/northstar/rl/
    ├── agent/
    │   ├── DqnAgent.java         # 通用模型调用壳：fromVersionDir 按版本目录初始化
    │   └── RLAgent.java          # 可刷新推理壳（版本目录 + 权重热切换）
    ├── model/
    │   ├── RLModelMetadata.java  # 模型级 metadata（传输载荷，可现场重建，snake_case JSON 契约）
    │   ├── RLVersionMetadata.java# 版本级 metadata（血缘/超参取值/评估指标/网络结构回写）
    │   └── RLModelStore.java     # 模型目录仓库：syncModel 重建/引导、fork 版本（双重校验）
    ├── env/
    │   ├── CartPoleEnv.java      # CartPole-v1 物理环境（示例）
    │   ├── MarketReplay.java     # 行情回放游标：locate/advanceBar 三态，纯游标算术零 I/O
    │   └── AdvanceResult.java    # WARMUP/OK/EXHAUSTED
    ├── experience/
    │   ├── ExperienceStore.java  # 任务级经验仓库：replay-r{轮}-c{合约}.csv 分片
    │   └── Transition.java       # (s, a, r, s', dt, terminated)
    ├── strategy/
    │   └── RlTrendStrategy.java  # 示例 RL 策略（6 维状态 × 3 动作，训练/实盘同一钩子路径）
    ├── training/
    │   ├── DqnHyperParams.java   # DQN 超参数定义（DynamicParams/@Setting，UI 动态渲染）
    │   ├── RLAlgorithms.java     # algo -> Hyperparameter 注册表与默认值表
    │   ├── RLTrainExecutor.java  # 单轮训练执行器接口（RLTrainResult 记录）
    │   └── PythonTrainExecutor.java  # Python 子进程实现（解析 LOSS= 等结构化输出）
    └── example/DqnInference.java     # 推理示例：加载模型版本目录跑完整回合
```

## 模型定义与版本号规则

### 定义权在代码，metadata.json 仅是传输协议

- **网络结构**：由 Python 端 `training/registry.py` 按**模型名**注册（唯一权威定义处）；
  Java 端 `RLStrategy.modelName()` 声明策略绑定的模型名（如 `RlTrendStrategy` → `rl-trend-dqn`），
  按名引用、不做结构假设。
- **维度与超参**：由 Java 端 `RLModelSpec` 定义（state_dim/action_dim/actions/features），
  经模型级 metadata.json 同步给 Python（`train_round.py` 读维度、按名取网络）。
- **metadata.json 可随时重建**：它是 Java 写出、Python 读入的同步载荷，不是身份凭证。
  目录缺失或文件陈旧时 `RLModelStore.syncModel(strategy)` 用策略现场 spec 引导生成/重写
  （保留 `latest_version` 与 preprocessing 的 mean/std）；**任何生产路径都不会因为它缺失而失败**。
  模型列表 = 磁盘目录扫描 ∪ 已注册策略声明的模型：策略声明了 `modelName()` 即出现在列表中，
  目录尚不存在时会在列模型时现场 bootstrap——无需预先手工准备模型目录。
- **结构信息回流**：Python 每轮训练后把网络结构描述写入版本级 metadata 的 `network` 字段，
  Java 只透传（前端用于展示，未训练过时为空）。

### 版本号的生成

- 版本号**按模型（modelId）递增**，不是全局递增；单调递增、**允许空洞**（刻意设计：
  规避 Windows 文件锁、版本目录只增不改、删除父版本不影响已复制 checkpoint 的子版本）。
- 唯一生成新版本的地方是 `RLModelStore.forkVersion()`（`newVersion = latest_version + 1`，
  写入模型级 metadata 的 `latest_version` 字段并落盘）。
- 模型目录的"存在"不以 metadata.json 为前提：目录不存在时创建训练任务会自动引导生成
  （latest_version 从 0 开始）。

### 训练任务与版本号的关系

**一个训练任务 = 恰好 fork 出一个新版本**（任务的 `targetVersion`）。创建任务时无条件
fork，即使选择从零训练（`baseVersion=0`）也会照常占用一个新版本号。

**v(n+1) 不保证是在 v(n) 的权重基础上训练得到的**——版本号表达的是"血缘/谱系第几代"，
不是"权重第几代继承"。是否继承权重由任务的两个参数决定：

| 参数 | 语义 |
|---|---|
| `baseVersion` | 父版本号；`0` 表示从零训练 |
| `inheritWeights` | 是否继承父版本权重；缺省值 = `baseVersion > 0` |

fork 时的校验与行为（`RLModelStore.forkVersion()` 双重校验）：

- `inheritWeights=true` 且 `baseVersion>0`：父版本必须存在、父版本 spec_hash 必须与**策略现场
  spec** 一致（防策略演进导致权重不兼容）、且父版本目录里必须有已训练的 `checkpoint.pt`
  ——满足后**物理复制** checkpoint 到新版本目录，训练从父权重继续；预处理统计量（mean/std）
  冻结沿用父版本。
- `inheritWeights=false` 或 `baseVersion=0`：不复制权重，从零初始化训练，但版本 metadata
  仍会记录 `parent_version` 与 `weight_inherited: false`，血缘可追溯。

因此同一模型下的 v1、v2 可能完全是两次独立训练，只是共享同一模型规格（specHash）。
策略演进（features/actions/preprocessing 变化）会改变 specHash，继承旧权重会被拒绝。

### 版本号的回收与重置

- **没有"重置到 0"的机制**，版本号只增不减（删除后重新 fork 仍继续 +1）。
- 删除版本（`RLModelStore.deleteVersion()`）**不是独立 REST 接口**，只在以下时机触发：
  1. 创建任务落库失败时回滚 fork 出的版本；
  2. 删除未开始训练（CREATED/LOADING/READY）的任务时回收其 fork 的版本；
     **已训练（FINISHED/FAILED）的任务删除时保留版本**；
  3. `resetAll()`（仅 e2e/dev profile 的系统重置）。
- 删除的是最新版本时，`latest_version` 回退到剩余最大版本号（允许出现空洞：
  如删掉 v3 后 latest=2，下次 fork 仍生成 v4）。
- **想从零开始重新编号**：手工删除整个模型目录 `models/{modelId}/`（该模型全部版本与血缘
  一并清除），下次创建任务会自动重新引导生成（latest_version 归 0）。
- Python 训练侧（`train_round.py`）不做版本管理：只按 `--version` 定位既有版本目录
  读写训练产物，从不创建或删除版本目录。

## 模型目录契约（Java/Python 共享，字段一律 snake_case）

```
models/{modelId}/
├── metadata.json              # 模型级（传输载荷，Java 拥有、可现场重建）：name/strategy_class/
│                              #   algo/state_dim/action_dim/actions/features[{name,normalizer}]/
│                              #   preprocessing/spec_hash/latest_version
└── versions/v{n}/
    ├── metadata.json          # 版本级：version/parent_version/weight_inherited/spec_hash/
    │                          #   checkpoint_signature/network{...Python 回写的结构信息}/
    │                          #   preprocessing/training{...hyper_params}/accepted_by
    ├── checkpoint.pt          # 训练态全量（policy/target/optimizer/total_steps）
    ├── model.pt               # TorchScript 推理产物（DJL 加载）
    ├── rounds/                # 每轮经验分片 replay-r{round}-c{contractIdx}.csv
    ├── buffer.npz             # 持久化 replay buffer（Python 增量读取）
    └── ingested.json          # 已入库分片清单
```

经验 CSV 列契约：`s0..s{stateDim-1},action,reward,ns0..ns{stateDim-1},dt,terminated`
（`dt` = 决策间隔 bar 数，DQN target 按 `γ^dt` 折扣）。

> 存放归属：版本目录内的全部内容都是版本级长期资产（跨任务迭代、模组绑定、血缘复盘所依赖）；
> 训练任务的行情数据属任务级临时生产资料，单独存放于 `<data-root>/<taskId>/`，随任务删除。
> 详见 `DESIGN.md` §5.2「数据归属原则」。

## 单元测试（保守示例调用）

```bash
mvn test                                    # 默认：环境/模型存储/回放游标/经验仓库/超参/校验逻辑，无需原生库
mvn test -Drl.inference.test=true           # 追加：真实加载 model.pt 的推理示例（win-x86_64）

# Python 侧测试（使用 env.sh / env.ps1 准备的 rl-venv；Windows 为 c:\northstar_env\rl-venv\Scripts\python）
cd training && ~/northstar-env/rl-venv/bin/python -m pytest tests/
```

`DqnInferenceExampleTest` 真实加载 `models/cartpole/versions/v1/model.pt` 推理，默认跳过，
需 `-Drl.inference.test=true` 显式开启（依赖 `pytorch-native-cpu` classifier 与平台一致，
默认 `win-x86_64`，可在 `pom.xml` 中通过 `pytorch.native.classifier` 属性切换）。

## Python 训练环境

训练任务由监控台「交易自动化 → 强化学习训练」创建并驱动（无需手工跑脚本）。
RL 训练 Python 环境（venv + numpy/torch/gymnasium 依赖）由仓库根目录的
`env.sh`（Linux，装到 `~/northstar-env/rl-venv`）/ `env.ps1`（Windows，装到 `c:\northstar_env\rl-venv`）
自动准备，并写入 `NS_RL_PYTHON` 供应用启动时探测使用。
本地调试 Python 训练侧如需独立环境，也可在任意位置自建 venv 后
`pip install -r training/requirements.txt`，并通过 `NS_RL_PYTHON` 指定。

## 在交易策略中使用（训练与实盘）

RL 策略实现 `RLStrategy` 接口（见 `RlTrendStrategy` 示例），需声明 `modelName()`：
策略绑定的模型名必须与 Python 端 `training/registry.py` 的注册 key 一致，
网络结构以 Python 注册表为准，Java 不参与定义。训练任务通过监控台
「交易自动化 → 强化学习训练」创建（REST 契约见 DESIGN.md §6.3）。
实盘模组绑定固定版本号：在策略初始化参数中配置 `推理模型版本目录`
（如 `models/rl-trend-dqn/versions/v1`），模组加载时校验 specHash，不一致快速失败。

## 常见问题

- **新增一个 RL 模型要改哪里**：① Python 端 `training/registry.py` 按新模型名注册网络结构；
  ② Java 端新建 RLStrategy 实现并声明同名 `modelName()`；无需手工准备 metadata.json，
  首次创建训练任务时自动生成模型目录。
- **Linux/macOS 运行**：把 `pom.xml` 里 `pytorch.native.classifier` 改成
  `linux-x86_64` / `osx-x86_64` / `osx-aarch64` 即可。
- **GPU 推理**：把 native 依赖换成 `pytorch-native-cuXXX` 对应 CUDA 版本即可。
- **Windows 控制台中文乱码**：`train_round.py` 已强制 UTF-8 输出；Java 侧可执行
  `chcp 65001` 或给 JVM 加 `-Dstdout.encoding=UTF-8`。
- **Python 环境**：训练依赖 `training/requirements.txt`（torch/numpy/gymnasium/pytest）；
  服务端通过 `northstar.rl.python` 配置解释器路径。
