# northstar-rl：强化学习模块（DJL/PyTorch 推理 + Python 离线训练）

Northstar Pro 的强化学习模块，迁移自 DJL-PYTORCH POC，采用**采样-训练分离**架构，
为交易策略提供"预训练 DQN 模型调用"能力。本模块最终会被 `northstar-main` 与
`northstar-strategy-example` 引入（根 `pom.xml` 的 `dependencyManagement` 已托管版本），
从而让策略在 `AbstractStrategy` 中直接加载训练好的模型做动作决策。

## 架构

```
Java TrainingPipeline（常驻 JVM，循环驱动，模块内 exec 运行）
 ├── ExperienceCollector.collect()    进程内采样 -> replay.csv
 ├── python train_round.py            唯一的跨语言边界：单轮离线训练
 └── ExperienceCollector.evaluate()   进程内评估 -> 达标/最优模型保存
```

- **推理/采样侧（Java/DJL）**：`DqnAgent` 是与场景解耦的模型调用壳，供策略代码直接使用；
  `CartPoleEnv` 为示例环境（验证链路用，实际业务环境 = 行情/账户状态）。
- **训练侧（Python/PyTorch）**：`training/` 下为无状态单轮离线训练器，只依赖经验数据，
  不接触环境；跨轮状态持久化在 model 目录（`checkpoint.pt` + `replay.csv`，均不入库）。
- **model 目录自描述**：`metadata.json` 是 Java 与 Python 的共同初始化契约
  （输入/输出维度、动作表、预处理策略、训练超参）。

## 模块结构

```
northstar-rl/
├── models/
│   └── cartpole/                 # 示例 model 目录：model.pt + metadata.json（自描述）
├── training/                     # Python 训练侧（dqn 包 + train_round.py + requirements.txt）
└── src/main/java/org/dromara/northstar/rl/
    ├── agent/
    │   ├── DqnAgent.java         # 通用模型调用壳：fromModelDir 按 metadata.json 初始化
    │   └── ModelMetadata.java    # metadata.json 的 Java 映射
    ├── env/CartPoleEnv.java      # CartPole-v1 物理环境的 Java 复刻（示例环境）
    ├── training/
    │   ├── ExperienceCollector.java  # 经验采样器：ε-greedy 采集写 CSV / 贪心评估
    │   └── TrainingPipeline.java     # 一站式训练流水线（Java 驱动：采样->Python训练->评估）
    └── example/DqnInference.java     # 推理示例：加载 model 目录跑完整回合
```

## 在策略中使用（推理）

```xml
<!-- 策略模块 pom.xml（版本由根 pom dependencyManagement 托管） -->
<dependency>
    <groupId>org.dromara.northstar.pro</groupId>
    <artifactId>northstar-rl</artifactId>
</dependency>
```

```java
enum TradeAction { HOLD, BUY, SELL }  // 枚举名称/顺序必须与 metadata.json 的 actions 表一致

try (DqnAgent<TradeAction> agent = DqnAgent.fromModelDir(
        Path.of("models/trade"), TradeAction.class)) {   // 构造时校验枚举与动作表逐一匹配
    TradeAction action = agent.act(stateVector);  // 一步决策（argmax）
    float[] q = agent.qValues(stateVector);       // 或直接取 Q 值
}
```

- **输入预处理 = DJL `Translator`**：`FloatVectorTranslator`（默认直通，可继承覆盖
  `preprocess()` 定制）；`DqnAgent.standardizing(mean, std)` 标准化。
  **预处理参数必须与训练侧一致**。
- **输出固定为 argmax**：Q 值最大下标 = 枚举 ordinal。
- Agent 会做四类校验并快速失败：枚举与 metadata 动作表逐一匹配（名称+顺序）、
  枚举常量数 == actionDim（构造期）、输入长度 == inputDim、模型输出长度 == actionDim（推理期）。

## 单元测试（保守示例调用）

```bash
mvn test                                    # 默认：环境/metadata/采样/校验逻辑，无需原生库
mvn test -Drl.inference.test=true           # 追加：真实加载 model.pt 的推理示例（win-x86_64）
```

默认测试不加载 PyTorch 原生库、不调用 Python，覆盖：CartPole 环境物理与终止判定、
metadata 解析、DqnAgent 的快速失败校验与标准化预处理、随机策略采样写 CSV。
`DqnInferenceExampleTest` 是 `DqnInference` 的单测版本，会真实加载
`models/cartpole/model.pt` 推理，因此默认跳过，需 `-Drl.inference.test=true` 显式开启
（依赖 `pytorch-native-cpu` 的 classifier 与当前平台一致，默认 `win-x86_64`，
可在 `pom.xml` 中通过 `pytorch.native.classifier` 属性切换）。

## 训练（示例：CartPole）

```bash
cd northstar-rl
python -m venv .venv
.venv/Scripts/pip install -r training/requirements.txt   # Windows

mvn compile exec:java -Dexec.mainClass=org.dromara.northstar.rl.training.TrainingPipeline
```

- 每轮循环：进程内采样 20 局（ε 按轮数衰减，首轮模型不存在时纯随机冷启动）→
  调用 `python training/train_round.py` 做 Double DQN 离线训练 → 进程内贪心评估 10 局。
- Java 侧评估达标（均分 ≥ 475/500）提前结束；全程保存评估最优的模型文件作为最终产物。
- 可选参数（追加 `-Dexec.args="..."`）：`--model-dir`、`--rounds`、`--episodes-per-round`、
  `--eval-episodes`、`--accept`、`--python`、`--seed`。

## 常见问题

- **Linux/macOS 运行**：把 `pom.xml` 里 `pytorch.native.classifier` 改成
  `linux-x86_64` / `osx-x86_64` / `osx-aarch64` 即可。
- **GPU 推理**：把 native 依赖换成 `pytorch-native-cuXXX` 对应 CUDA 版本即可。
- **Windows 控制台中文乱码**：`train_round.py` 已强制 UTF-8 输出；Java 侧可执行
  `chcp 65001` 或给 JVM 加 `-Dstdout.encoding=UTF-8`。
