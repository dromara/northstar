"""registry.py 单元测试：模型名注册表命中、未知名名报错、构建网络 shape 正确。"""
import pytest
import torch

from registry import MODEL_REGISTRY, build_network, network_spec


class TestBuildNetwork:
    def test_registered_model_builds_mlp_with_expected_shape(self):
        net = build_network("dqn-demo", state_dim=4, action_dim=2)
        assert isinstance(net, torch.nn.Module)
        assert net(torch.zeros(3, 4)).shape == (3, 2)
        # 结构与注册描述一致：128 -> 128 -> action_dim
        linear_layers = [m for m in net.net if isinstance(m, torch.nn.Linear)]
        assert [l.in_features for l in linear_layers] == [4, 128, 128]
        assert [l.out_features for l in linear_layers] == [128, 128, 2]

    def test_action_dim_controls_output_width(self):
        net = build_network("dqn-demo", state_dim=10, action_dim=5)
        assert net(torch.zeros(1, 10)).shape == (1, 5)

    def test_unknown_model_raises_key_error_listing_available(self):
        with pytest.raises(KeyError) as exc:
            build_network("no-such-model", 4, 2)
        assert "no-such-model" in str(exc.value)
        assert "dqn-demo" in str(exc.value)


class TestNetworkSpec:
    def test_spec_matches_registered_structure(self):
        assert network_spec("dqn-demo") == {
            "type": "mlp", "hidden": [128, 128], "activation": "relu"}

    def test_unknown_model_spec_raises_key_error(self):
        with pytest.raises(KeyError):
            network_spec("no-such-model")

    def test_registry_contains_rl_trend_dqn(self):
        assert "dqn-demo" in MODEL_REGISTRY
