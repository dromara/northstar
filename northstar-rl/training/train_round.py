"""
单轮离线训练（learner，无状态进程）。

由 Java 端 TrainingPipeline 逐轮调用：
  加载 model 目录内的 replay.csv 全量经验 -> 执行 N 步梯度更新 ->
  保存 checkpoint.pt（跨轮训练状态）-> 导出 model.pt（TorchScript，供 Java 推理）。

跨轮状态全部持久化在 model 目录内（replay.csv + checkpoint.pt），
本进程本身不保留任何状态。
"""
import argparse
import json
import sys
from pathlib import Path

import numpy as np

# Windows 中文系统默认代码页是 GBK，统一强制 UTF-8 输出
if sys.stdout.encoding and sys.stdout.encoding.lower() != "utf-8":
    sys.stdout.reconfigure(encoding="utf-8")
    sys.stderr.reconfigure(encoding="utf-8")

from dqn import DqnTrainer

CHECKPOINT_FILE = "checkpoint.pt"


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--model-dir", required=True, help="model 目录（含 metadata.json）")
    ap.add_argument("--grad-steps", type=int, required=True, help="本轮梯度更新步数")
    ap.add_argument("--lr", type=float, default=None,
                    help="覆盖 metadata 中的学习率（恢复 checkpoint 后同样生效，用于跨轮 LR 调度）")
    ap.add_argument("--seed", type=int, default=42)
    args = ap.parse_args()

    model_dir = Path(args.model_dir)
    meta = json.loads((model_dir / "metadata.json").read_text(encoding="utf-8"))
    training = meta.get("training", {})
    state_dim = meta["state_dim"]

    trainer = DqnTrainer(
        state_dim, meta["action_dim"],
        hidden=meta.get("hidden", 128),
        lr=args.lr if args.lr is not None else training.get("lr", 5e-4),
        gamma=training.get("gamma", 0.99),
        batch_size=training.get("batch_size", 128),
        buffer_capacity=training.get("buffer_capacity", 100_000),
        min_buffer=training.get("min_buffer", 1_000),
        tau=training.get("tau", 0.005),
        seed=args.seed,
    )

    # 恢复上一轮训练状态（首轮无 checkpoint -> 全新初始化）
    ckpt = model_dir / CHECKPOINT_FILE
    if ckpt.exists():
        trainer.load_checkpoint(ckpt)
        if args.lr is not None:  # checkpoint 中的优化器状态带有旧 LR，按本轮调度覆盖
            for g in trainer.optimizer.param_groups:
                g["lr"] = args.lr

    # 从 model 目录加载全量经验数据（带表头的 CSV）
    data_file = model_dir / meta["data_file"]
    if data_file.exists():
        data = np.genfromtxt(data_file, delimiter=",", dtype=np.float32, skip_header=1)
        if data.ndim == 1:
            data = data.reshape(1, -1)
        trainer.add_experiences(
            data[:, 0:state_dim], data[:, state_dim], data[:, state_dim + 1],
            data[:, state_dim + 2:2 * state_dim + 2], data[:, -1])

    # 离线训练 -> 保存状态 -> 导出推理模型
    steps = trainer.train_steps(args.grad_steps)
    trainer.save_checkpoint(ckpt)
    trainer.export_torchscript(model_dir / meta["model_file"])
    print(f"TRAINED_STEPS={steps} BUFFER_SIZE={trainer.buffer_size}")


if __name__ == "__main__":
    main()
