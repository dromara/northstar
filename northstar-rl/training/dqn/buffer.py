"""经验回放池：定长 FIFO，随机均匀采样。"""
import numpy as np


class ReplayBuffer:
    """存储转移样本 (s, a, r, s', dt, terminated)，按 numpy 数组分段存储以支持批量采样。

    dt 为决策间隔 bar 数（事件驱动决策 / SMDP 语义）；
    bar 级决策的旧数据等价于 dt=1。
    """

    def __init__(self, capacity, state_dim):
        self.capacity = capacity
        self.states = np.empty((0, state_dim), dtype=np.float32)
        self.actions = np.empty(0, dtype=np.int64)
        self.rewards = np.empty(0, dtype=np.float32)
        self.next_states = np.empty((0, state_dim), dtype=np.float32)
        self.dts = np.empty(0, dtype=np.float32)
        self.terminated = np.empty(0, dtype=np.float32)

    def add(self, states, actions, rewards, next_states, dts, terminated):
        """批量追加样本；超出容量时丢弃最旧的。"""
        self.states = np.concatenate([self.states, states])[-self.capacity:]
        self.actions = np.concatenate([self.actions, actions])[-self.capacity:]
        self.rewards = np.concatenate([self.rewards, rewards])[-self.capacity:]
        self.next_states = np.concatenate([self.next_states, next_states])[-self.capacity:]
        self.dts = np.concatenate([self.dts, dts])[-self.capacity:]
        self.terminated = np.concatenate([self.terminated, terminated])[-self.capacity:]

    def __len__(self):
        return len(self.states)

    def sample(self, batch_size):
        idx = np.random.randint(0, len(self), size=batch_size)
        return (self.states[idx], self.actions[idx], self.rewards[idx],
                self.next_states[idx], self.dts[idx], self.terminated[idx])

    # ---------------- 跨轮持久化（增量读取配套） ----------------

    def save(self, path):
        """持久化到 .npz，Python 训练进程无状态，跨轮状态全在版本目录。

        先写临时文件再 os.replace 原子替换，规避 Windows 文件锁与崩溃半截写；
        临时文件名以 .npz 结尾，避免 np.savez_compressed 自动追加后缀。
        """
        import os
        path = str(path)
        tmp = path + ".tmp.npz"
        np.savez_compressed(tmp, states=self.states, actions=self.actions,
                            rewards=self.rewards, next_states=self.next_states,
                            dts=self.dts, terminated=self.terminated)
        os.replace(tmp, path)

    def load(self, path):
        data = np.load(str(path))
        self.states = data["states"]
        self.actions = data["actions"]
        self.rewards = data["rewards"]
        self.next_states = data["next_states"]
        self.dts = data["dts"] if "dts" in data else np.ones_like(self.rewards)
        self.terminated = data["terminated"]
