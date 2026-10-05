"""Q 网络定义：输入状态向量，输出各动作的 Q 值。"""
import torch.nn as nn

_ACTIVATIONS = {
    "relu": nn.ReLU,
    "tanh": nn.Tanh,
    "sigmoid": nn.Sigmoid,
    "elu": nn.ELU,
}


class QNetwork(nn.Module):
    """MLP：state(state_dim) -> hidden[0] -> ... -> hidden[-1] -> Q(action_dim)

    网络结构由 training/registry.py 按模型名定义（Python 端为唯一权威）；
    想改某个模型的网络结构 = 改 registry 中该模型名的注册项。
    """

    def __init__(self, state_dim, action_dim, hidden=(128, 128), activation="relu"):
        super().__init__()
        if isinstance(hidden, int):  # 兼容旧的标量写法：单个隐藏层
            hidden = [hidden, hidden]
        act_cls = _ACTIVATIONS.get(activation.lower())
        if act_cls is None:
            raise ValueError(f"不支持的激活函数: {activation}（可选：{sorted(_ACTIVATIONS)}）")
        layers = []
        prev = state_dim
        for h in hidden:
            layers += [nn.Linear(prev, h), act_cls()]
            prev = h
        layers.append(nn.Linear(prev, action_dim))
        self.net = nn.Sequential(*layers)

    def forward(self, x):
        return self.net(x)
