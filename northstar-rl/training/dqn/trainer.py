"""
通用 Double DQN 离线训练器（learner）。

只依赖经验数据 (s, a, r, s', dt, terminated)，不接触环境、不关心数据从哪来
（可以来自 Java 采样器、历史日志、仿真器等）。
换业务场景 = 换 state_dim/action_dim 与数据来源，本类不变。
"""
import copy
import random

import numpy as np
import torch
import torch.optim as optim

from .buffer import ReplayBuffer
from .network import QNetwork


class DqnTrainer:

    def __init__(self, state_dim, action_dim, *, hidden=(128, 128), activation="relu",
                 net_factory=None,
                 lr=5e-4, gamma=0.99, batch_size=128, buffer_capacity=100_000,
                 min_buffer=1_000, tau=0.005, seed=42, device=None):
        # Java 端可能传入 64 位 long（负值或超过 2**32-1），而 numpy 旧式播种限定 [0, 2**32-1]
        seed = int(seed) & 0xFFFFFFFF
        random.seed(seed)
        np.random.seed(seed)
        torch.manual_seed(seed)
        self.device = device or torch.device("cpu")
        self.state_dim = state_dim
        self.action_dim = action_dim
        self.gamma = gamma
        self.batch_size = batch_size
        self.min_buffer = min_buffer
        self.tau = tau  # target network 软更新系数
        self.total_steps = 0  # 累计梯度步数（checkpoint 签名的一部分）

        # net_factory 优先（网络结构由 registry.py 按模型名定义）；缺省用 hidden/activation 构造通用 QNetwork
        factory = net_factory or (lambda s, a: QNetwork(s, a, hidden, activation))
        self.policy_net = factory(state_dim, action_dim).to(self.device)
        self.target_net = factory(state_dim, action_dim).to(self.device)
        self.target_net.load_state_dict(self.policy_net.state_dict())
        self.target_net.eval()
        self.optimizer = optim.Adam(self.policy_net.parameters(), lr=lr)
        self.buffer = ReplayBuffer(buffer_capacity, state_dim)

    # ---------------- 经验数据 ----------------

    def add_experiences(self, states, actions, rewards, next_states, dts, terminated):
        """追加一批转移样本（numpy 数组，states/next_states 形状 [N, state_dim]）。"""
        self.buffer.add(
            np.asarray(states, dtype=np.float32),
            np.asarray(actions, dtype=np.int64),
            np.asarray(rewards, dtype=np.float32),
            np.asarray(next_states, dtype=np.float32),
            np.asarray(dts, dtype=np.float32),
            np.asarray(terminated, dtype=np.float32),
        )

    @property
    def buffer_size(self):
        return len(self.buffer)

    @property
    def ready(self):
        """样本量是否达到开始训练的下限。"""
        return len(self.buffer) >= self.min_buffer

    # ---------------- 训练 ----------------

    def train_steps(self, n):
        """执行 n 次梯度更新；样本不足时不训练。返回 (实际步数, 平均 loss)。"""
        if not self.ready:
            return 0, float("nan")
        losses = []
        for _ in range(n):
            losses.append(self._optimize())
            self.total_steps += 1
        return n, float(np.mean(losses))

    def _optimize(self):
        states, actions, rewards, next_states, dts, terminated = self.buffer.sample(self.batch_size)
        states = torch.tensor(states, device=self.device)
        actions = torch.tensor(actions, device=self.device).unsqueeze(1)
        rewards = torch.tensor(rewards, device=self.device)
        next_states = torch.tensor(next_states, device=self.device)
        # 不定长转移按 γ^dt 折扣，否则"持有 5 根赚 100"与"持有 50 根赚 100"被视为等价
        discounts = torch.pow(
            torch.tensor(self.gamma, device=self.device),
            torch.tensor(dts, device=self.device))
        terminated = torch.tensor(terminated, device=self.device)

        q_values = self.policy_net(states).gather(1, actions).squeeze(1)
        # Double DQN：policy net 选动作，target net 估值，缓解 Q 值过估计
        with torch.no_grad():
            next_actions = self.policy_net(next_states).argmax(dim=1, keepdim=True)
            next_q = self.target_net(next_states).gather(1, next_actions).squeeze(1)
            targets = rewards + discounts * next_q * (1.0 - terminated)

        loss = torch.nn.functional.smooth_l1_loss(q_values, targets)
        self.optimizer.zero_grad()
        loss.backward()
        torch.nn.utils.clip_grad_norm_(self.policy_net.parameters(), 100.0)
        self.optimizer.step()

        # target network 软更新 (Polyak averaging)
        with torch.no_grad():
            for tp, pp in zip(self.target_net.parameters(), self.policy_net.parameters()):
                tp.mul_(1.0 - self.tau).add_(self.tau * pp)
        return float(loss.item())

    # ---------------- 权重与导出 ----------------

    def get_weights(self):
        """当前 policy net 权重的深拷贝，用于最优 checkpoint。"""
        return copy.deepcopy(self.policy_net.state_dict())

    def set_weights(self, state_dict):
        self.policy_net.load_state_dict(state_dict)

    # ---------------- 跨进程持久化 ----------------
    # Python 训练进程每轮重启时会丢失状态，完整训练状态通过 checkpoint 文件跨轮传递。

    def save_checkpoint(self, path):
        """保存完整训练状态（policy/target/优化器/累计步数）。先写临时文件再原子替换（规避 Windows 文件锁）。"""
        tmp = str(path) + ".tmp"
        torch.save({
            "policy": self.policy_net.state_dict(),
            "target": self.target_net.state_dict(),
            "optimizer": self.optimizer.state_dict(),
            "total_steps": self.total_steps,
        }, tmp)
        import os
        os.replace(tmp, str(path))

    def load_checkpoint(self, path):
        ckpt = torch.load(str(path), map_location=self.device, weights_only=False)
        self.policy_net.load_state_dict(ckpt["policy"])
        self.target_net.load_state_dict(ckpt["target"])
        self.optimizer.load_state_dict(ckpt["optimizer"])
        self.total_steps = ckpt.get("total_steps", 0)

    def checkpoint_signature(self):
        """各层参数 shape 哈希 + optimizer 类型 + 累计步数（fork 兼容性静态校验依据）。"""
        import hashlib
        h = hashlib.sha256()
        for name, p in sorted(self.policy_net.state_dict().items()):
            h.update(name.encode("utf-8"))
            h.update(np.asarray(p.shape, dtype=np.int64).tobytes())
        h.update(type(self.optimizer).__name__.encode("utf-8"))
        h.update(str(self.total_steps).encode("utf-8"))
        return "sha256:" + h.hexdigest()[:16]

    def export_torchscript(self, out_path):
        """trace + freeze 导出 TorchScript，DJL PyTorch 引擎可直接加载。先写临时文件再原子替换。"""
        import os
        self.policy_net.eval()
        example = torch.zeros(1, self.state_dim, dtype=torch.float32, device=self.device)
        with torch.no_grad():
            traced = torch.jit.trace(self.policy_net, example)
            traced = torch.jit.freeze(traced)
        tmp = str(out_path) + ".tmp"
        traced.save(tmp)
        os.replace(tmp, str(out_path))
        self.policy_net.train()
