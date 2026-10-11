"""
单轮离线训练（learner，无状态进程）。

由 Java 端 RLTrainingRunner 逐轮调用（唯一的跨语言边界）：
  按模型级 metadata 的 algo 分发到 dqn/ 包（PPO 预留给 v2）->
  增量读取版本目录 rounds/ 下的新增经验分片到持久化 replay buffer ->
  执行 N 步梯度更新 -> 保存 checkpoint.pt（跨轮训练状态）-> 导出 model.pt（TorchScript，供 Java 推理）。

跨轮状态全部持久化在版本目录（checkpoint.pt + buffer.npz + ingested.json），
本进程本身不保留任何状态。
"""
import argparse
import json
import math
import sys
from pathlib import Path

import numpy as np

# Windows 中文系统默认代码页是 GBK，统一强制 UTF-8 输出
if sys.stdout.encoding and sys.stdout.encoding.lower() != "utf-8":
    sys.stdout.reconfigure(encoding="utf-8")
    sys.stderr.reconfigure(encoding="utf-8")

CHECKPOINT_FILE = "checkpoint.pt"
MODEL_FILE = "model.pt"
BUFFER_FILE = "buffer.npz"
INGESTED_FILE = "ingested.json"  # 已入库的经验分片清单（增量读取依据）


def _atomic_write_text(path, text):
    """先写临时文件再 os.replace 原子替换，规避 Windows 文件锁与崩溃半截写。"""
    import os
    tmp = str(path) + ".tmp"
    with open(tmp, "w", encoding="utf-8") as f:
        f.write(text)
    os.replace(tmp, str(path))


def resolve_gamma(hp_gamma, episode_bars):
    """折扣因子解析：显式配置（>0）优先；否则按"时间常数 τ = 单局 bar 数"自动推导 γ = e^(-1/τ)。

    γ 的时间常数须与 dt 的量纲（bar）匹配：固定 0.99 的 τ≈100 bar，单局较长时一次长持仓
    转移就会把 bootstrap 打到 e^(-1) 量级，Q 退化为短视。τ 对齐 episode 长度后，
    单局内的远期价值衰减被控制在温和区间，持仓时长偏好交给 reward shaping 表达。
    """
    if hp_gamma is not None and hp_gamma > 0:
        return float(hp_gamma)
    if episode_bars:
        return math.exp(-1.0 / episode_bars)
    return 0.99


def train_dqn(model_meta, version_dir, ver_meta, grad_steps, lr_override, seed, episode_bars=None):
    from dqn import DqnTrainer
    from expstore import list_shards, load_shard
    from registry import build_network, network_spec

    state_dim = model_meta["state_dim"]
    model_name = model_meta["name"]
    hp = ver_meta.get("training", {}).get("hyper_params", {})

    # 网络结构由 registry.py 按模型名定义（Python 端为唯一权威）；
    # metadata 仅作传输协议：维度从模型级 metadata 读，结构描述回写版本级 metadata
    trainer = DqnTrainer(
        state_dim, model_meta["action_dim"],
        net_factory=lambda s, a: build_network(model_name, s, a),
        lr=lr_override if lr_override is not None else hp.get("lr", 5e-4),
        gamma=resolve_gamma(hp.get("gamma"), episode_bars),
        batch_size=int(hp.get("batchSize", 128)),
        buffer_capacity=int(hp.get("bufferCapacity", 100_000)),
        min_buffer=int(hp.get("minBuffer", 500)),
        tau=hp.get("tau", 0.005),
        seed=seed,
    )

    # 恢复上一轮训练状态（首轮无 checkpoint -> 全新初始化）
    ckpt = version_dir / CHECKPOINT_FILE
    if ckpt.exists():
        trainer.load_checkpoint(ckpt)
        if lr_override is not None:  # checkpoint 中的优化器状态带有旧 LR，按本轮调度覆盖
            for g in trainer.optimizer.param_groups:
                g["lr"] = lr_override

    # 恢复持久化 replay buffer，并增量读取新增经验分片
    buffer_file = version_dir / BUFFER_FILE
    ingested_file = version_dir / INGESTED_FILE
    if buffer_file.exists():
        trainer.buffer.load(buffer_file)
    ingested = set(json.loads(ingested_file.read_text(encoding="utf-8"))) if ingested_file.exists() else set()
    new_shards = [p for p in list_shards(version_dir / "rounds") if p.name not in ingested]
    new_batches = []
    new_samples = 0
    for shard in new_shards:
        batch = load_shard(shard, state_dim)
        if batch is not None:
            new_batches.append(batch)
            new_samples += len(batch[0])
        ingested.add(shard.name)

    # 预处理：standardize 的统计量在 v1 首次训练时计算并冻结；fork 版本默认沿用父版本统计量。
    # 不变量：buffer.npz 中的状态在统计量冻结后一律为标准化后的值；冻结瞬间一次性转换存量数据。
    # custom：变换已由 Java 侧策略 RLStatePreprocessor 在经验落盘前完成，Python 直通（不冻结统计量）。
    pp = ver_meta.setdefault("preprocessing", {"type": "identity"})
    standardizing = pp.get("type") == "standardize"
    if standardizing and not pp.get("mean"):
        all_states = [b[0] for b in new_batches]
        if len(trainer.buffer) > 0:
            all_states.insert(0, trainer.buffer.states)
        if all_states:
            stacked = np.concatenate(all_states)
            mean = stacked.mean(axis=0)
            std = stacked.std(axis=0)
            std[std == 0] = 1.0
            pp["mean"] = [round(float(v), 8) for v in mean]
            pp["std"] = [round(float(v), 8) for v in std]
            if len(trainer.buffer) > 0:  # 存量 raw 数据一次性转换
                trainer.buffer.states = (trainer.buffer.states - mean) / std
                trainer.buffer.next_states = (trainer.buffer.next_states - mean) / std
            print(f"PREPROCESSING_FROZEN dim={state_dim}")
    if standardizing and pp.get("mean"):
        mean = np.asarray(pp["mean"], dtype=np.float32)
        std = np.asarray(pp["std"], dtype=np.float32)
        new_batches = [((b[0] - mean) / std, b[1], b[2], (b[3] - mean) / std, b[4], b[5])
                       for b in new_batches]
    for batch in new_batches:
        trainer.add_experiences(*batch)

    # 离线训练 -> 保存状态 -> 导出推理模型
    # （checkpoint.pt/model.pt/buffer.npz/ingested.json/metadata.json 均先写临时文件再原子替换，
    #   规避 Windows 文件锁与崩溃半截写）
    steps, avg_loss = trainer.train_steps(grad_steps)
    trainer.save_checkpoint(ckpt)
    trainer.export_torchscript(version_dir / MODEL_FILE)

    # 跨轮状态与签名落盘
    trainer.buffer.save(buffer_file)
    _atomic_write_text(ingested_file, json.dumps(sorted(ingested)))
    ver_meta["checkpoint_signature"] = trainer.checkpoint_signature()
    ver_meta["network"] = network_spec(model_name)  # 结构信息回传 Java 端
    _atomic_write_text(version_dir / "metadata.json",
                       json.dumps(ver_meta, ensure_ascii=False, indent=2))

    print(f"NEW_SAMPLES={new_samples} TRAINED_STEPS={steps} "
          f"BUFFER_SIZE={trainer.buffer_size} LOSS={avg_loss:.6f} GAMMA={trainer.gamma:.8f}")


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--model-dir", required=True, help="模型目录（含模型级 metadata.json）")
    ap.add_argument("--version", type=int, required=True, help="版本号（版本目录 versions/v{N}）")
    ap.add_argument("--grad-steps", type=int, required=True, help="本轮梯度更新步数")
    ap.add_argument("--lr", type=float, default=None,
                    help="覆盖版本 metadata 中的学习率（轮边界调参，恢复 checkpoint 后同样生效）")
    ap.add_argument("--seed", type=int, default=42)
    ap.add_argument("--episode-bars", type=int, default=None,
                    help="单局最长 bar 数（Java 任务参数传入；γ<=0 时按 τ=该值 自动推导折扣因子）")
    args = ap.parse_args()

    model_dir = Path(args.model_dir)
    version_dir = model_dir / "versions" / f"v{args.version}"
    if not version_dir.is_dir():
        print(f"版本目录不存在: {version_dir}", file=sys.stderr)
        sys.exit(2)

    model_meta = json.loads((model_dir / "metadata.json").read_text(encoding="utf-8"))
    ver_meta = json.loads((version_dir / "metadata.json").read_text(encoding="utf-8"))

    algo = model_meta.get("algo", "DQN").upper()
    if algo == "DQN":
        train_dqn(model_meta, version_dir, ver_meta, args.grad_steps, args.lr, args.seed, args.episode_bars)
    else:
        print(f"算法 {algo} 的训练侧实现将在 v2 提供（v1 仅支持 DQN）", file=sys.stderr)
        sys.exit(2)


if __name__ == "__main__":
    main()
