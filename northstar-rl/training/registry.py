"""
网络结构注册表：Python 端是网络结构定义的唯一权威。

Java 端只负责编排并在模型级 metadata.json 中传递维度/超参（传输协议），
实际结构按模型名在此注册；train_round.py 通过 build_network() 构建网络，
并把 network_spec() 的结构描述回写到版本级 metadata（Python -> Java 的回传通道）。
"""
from typing import Callable

import torch.nn as nn

from dqn import QNetwork

# (state_dim, action_dim) -> nn.Module
MODEL_REGISTRY: dict[str, Callable[[int, int], nn.Module]] = {}

# 与 MODEL_REGISTRY 平行的结构描述（回写版本级 metadata 的 network 字段）
_NETWORK_SPECS: dict[str, dict] = {}


def _register(name: str, hidden: list[int], activation: str) -> None:
    """注册一个 MLP 模型：hidden 为各隐藏层维度，activation 见 dqn/network.py 支持列表。"""
    MODEL_REGISTRY[name] = (
        lambda state_dim, action_dim: QNetwork(state_dim, action_dim,
                                               hidden=hidden, activation=activation))
    _NETWORK_SPECS[name] = {"type": "mlp", "hidden": list(hidden), "activation": activation}


def build_network(name: str, state_dim: int, action_dim: int) -> nn.Module:
    """按模型名构建网络；未知名称抛 KeyError 并列出可用模型名。"""
    try:
        factory = MODEL_REGISTRY[name]
    except KeyError:
        raise KeyError(f"未知模型: {name}（可用：{sorted(MODEL_REGISTRY)}）") from None
    return factory(state_dim, action_dim)


def network_spec(name: str) -> dict:
    """返回模型的结构描述（写入版本级 metadata 的 network 字段）。"""
    try:
        return dict(_NETWORK_SPECS[name])
    except KeyError:
        raise KeyError(f"未知模型: {name}（可用：{sorted(_NETWORK_SPECS)}）") from None


# 结构与 Java 侧默认一致：RLModelMetadata.Network 默认 hidden=[128,128]、activation=relu
_register("dqn-demo", hidden=[128, 128], activation="relu")
_register("dqn-256-256", hidden=[256, 256], activation="relu")
