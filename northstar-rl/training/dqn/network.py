"""Q 网络定义：输入状态向量，输出各动作的 Q 值。"""

import torch.nn as nn


class QNetwork(nn.Module):
    """MLP：state(state_dim) -> hidden -> hidden -> Q(action_dim)"""

    def __init__(self, state_dim, action_dim, hidden=128):
        super().__init__()
        self.net = nn.Sequential(
            nn.Linear(state_dim, hidden), nn.ReLU(),
            nn.Linear(hidden, hidden), nn.ReLU(),
            nn.Linear(hidden, action_dim),
        )

    def forward(self, x):
        return self.net(x)
