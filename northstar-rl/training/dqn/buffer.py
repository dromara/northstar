"""经验回放池：定长 FIFO，随机均匀采样。"""

import numpy as np


class ReplayBuffer:
    """存储转移样本 (s, a, r, s', terminated)，按 numpy 数组分段存储以支持批量采样。"""

    def __init__(self, capacity, state_dim):
        self.capacity = capacity
        self.states = np.empty((0, state_dim), dtype=np.float32)
        self.actions = np.empty(0, dtype=np.int64)
        self.rewards = np.empty(0, dtype=np.float32)
        self.next_states = np.empty((0, state_dim), dtype=np.float32)
        self.terminated = np.empty(0, dtype=np.float32)

    def add(self, states, actions, rewards, next_states, terminated):
        """批量追加样本；超出容量时丢弃最旧的。"""
        self.states = np.concatenate([self.states, states])[-self.capacity:]
        self.actions = np.concatenate([self.actions, actions])[-self.capacity:]
        self.rewards = np.concatenate([self.rewards, rewards])[-self.capacity:]
        self.next_states = np.concatenate([self.next_states, next_states])[-self.capacity:]
        self.terminated = np.concatenate([self.terminated, terminated])[-self.capacity:]

    def __len__(self):
        return len(self.states)

    def sample(self, batch_size):
        idx = np.random.randint(0, len(self), size=batch_size)
        return (self.states[idx], self.actions[idx], self.rewards[idx],
                self.next_states[idx], self.terminated[idx])
