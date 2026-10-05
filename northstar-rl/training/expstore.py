"""经验分片读取：与 Java 侧 ExperienceStore 的 CSV 契约对应。

分片文件：rounds/replay-r{round}-c{contractIdx}.csv
列契约（按 state_dim 通用切分）：
    s0,...,s{state_dim-1},action,reward,ns0,...,ns{state_dim-1},dt,terminated
"""
import re
from pathlib import Path

import numpy as np

_SHARD_PATTERN = re.compile(r"replay-r(\d+)-c(\d+)\.csv$")


def list_shards(rounds_dir):
    """按 (round, contractIdx) 排序列出全部经验分片路径。"""
    rounds_dir = Path(rounds_dir)
    if not rounds_dir.is_dir():
        return []

    def sort_key(p):
        m = _SHARD_PATTERN.match(p.name)
        return (int(m.group(1)), int(m.group(2))) if m else (1 << 30, p.name)

    return sorted((p for p in rounds_dir.iterdir() if _SHARD_PATTERN.match(p.name)), key=sort_key)


def load_shard(path, state_dim):
    """读取一个分片，返回 (states, actions, rewards, next_states, dts, terminated) 六元组。"""
    data = np.genfromtxt(path, delimiter=",", dtype=np.float32, skip_header=1)
    if data.size == 0:
        return None
    if data.ndim == 1:
        data = data.reshape(1, -1)
    expected_cols = 2 * state_dim + 4
    if data.shape[1] != expected_cols:
        raise ValueError(
            f"{path.name} 列数 {data.shape[1]} 与契约不符（期望 {expected_cols} = 2*state_dim({state_dim})+4）")
    return (data[:, 0:state_dim],                      # states
            data[:, state_dim],                        # actions
            data[:, state_dim + 1],                    # rewards
            data[:, state_dim + 2:2 * state_dim + 2],  # next_states
            data[:, 2 * state_dim + 2],                # dt（决策间隔 bar 数）
            data[:, 2 * state_dim + 3])                # terminated
