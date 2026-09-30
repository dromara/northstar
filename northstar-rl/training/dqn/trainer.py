"""
通用 Double DQN 离线训练器（learner）。

只依赖经验数据 (s, a, r, s', terminated)，不接触环境、不关心数据从哪来
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

    def __init__(self, state_dim, action_dim, *, hidden=128, lr=5e-4, gamma=0.99,
                 batch_size=128, buffer_capacity=100_000, min_buffer=1_000,
                 tau=0.005, seed=42, device=None):
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

        self.policy_net = QNetwork(state_dim, action_dim, hidden).to(self.device)
        self.target_net = QNetwork(state_dim, action_dim, hidden).to(self.device)
        self.target_net.load_state_dict(self.policy_net.state_dict())
        self.target_net.eval()
        self.optimizer = optim.Adam(self.policy_net.parameters(), lr=lr)
        self.buffer = ReplayBuffer(buffer_capacity, state_dim)

    # ---------------- 经验数据 ----------------

    def add_experiences(self, states, actions, rewards, next_states, terminated):
        """追加一批转移样本（numpy 数组，states/next_states 形状 [N, state_dim]）。"""
        self.buffer.add(
            np.asarray(states, dtype=np.float32),
            np.asarray(actions, dtype=np.int64),
            np.asarray(rewards, dtype=np.float32),
            np.asarray(next_states, dtype=np.float32),
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
        """执行 n 次梯度更新；样本不足时不训练，返回实际步数。"""
        if not self.ready:
            return 0
        for _ in range(n):
            self._optimize()
        return n

    def _optimize(self):
        states, actions, rewards, next_states, terminated = self.buffer.sample(self.batch_size)
        states = torch.tensor(states, device=self.device)
        actions = torch.tensor(actions, device=self.device).unsqueeze(1)
        rewards = torch.tensor(rewards, device=self.device)
        next_states = torch.tensor(next_states, device=self.device)
        terminated = torch.tensor(terminated, device=self.device)

        q_values = self.policy_net(states).gather(1, actions).squeeze(1)
        # Double DQN：policy net 选动作，target net 估值，缓解 Q 值过估计
        with torch.no_grad():
            next_actions = self.policy_net(next_states).argmax(dim=1, keepdim=True)
            next_q = self.target_net(next_states).gather(1, next_actions).squeeze(1)
            targets = rewards + self.gamma * next_q * (1.0 - terminated)

        loss = torch.nn.functional.smooth_l1_loss(q_values, targets)
        self.optimizer.zero_grad()
        loss.backward()
        torch.nn.utils.clip_grad_norm_(self.policy_net.parameters(), 100.0)
        self.optimizer.step()

        # target network 软更新 (Polyak averaging)
        with torch.no_grad():
            for tp, pp in zip(self.target_net.parameters(), self.policy_net.parameters()):
                tp.mul_(1.0 - self.tau).add_(self.tau * pp)

    # ---------------- 权重与导出 ----------------

    def get_weights(self):
        """当前 policy net 权重的深拷贝，用于最优 checkpoint。"""
        return copy.deepcopy(self.policy_net.state_dict())

    def set_weights(self, state_dict):
        self.policy_net.load_state_dict(state_dict)

    # ---------------- 跨进程持久化 ----------------
    # Python 训练进程每轮重启时会丢失状态，完整训练状态通过 checkpoint 文件跨轮传递。

    def save_checkpoint(self, path):
        """保存完整训练状态（policy/target/优化器）。"""
        torch.save({
            "policy": self.policy_net.state_dict(),
            "target": self.target_net.state_dict(),
            "optimizer": self.optimizer.state_dict(),
        }, str(path))

    def load_checkpoint(self, path):
        ckpt = torch.load(str(path), map_location=self.device, weights_only=False)
        self.policy_net.load_state_dict(ckpt["policy"])
        self.target_net.load_state_dict(ckpt["target"])
        self.optimizer.load_state_dict(ckpt["optimizer"])

    def export_torchscript(self, out_path):
        """trace + freeze 导出 TorchScript，DJL PyTorch 引擎可直接加载。"""
        self.policy_net.eval()
        example = torch.zeros(1, self.state_dim, dtype=torch.float32, device=self.device)
        with torch.no_grad():
            traced = torch.jit.trace(self.policy_net, example)
            traced = torch.jit.freeze(traced)
        traced.save(str(out_path))
        self.policy_net.train()
