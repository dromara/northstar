"""DQN 训练侧单元测试：网络结构、回放池 dt 支持、γ^dt 折扣、年金校正、checkpoint 签名。"""
import numpy as np
import pytest
import torch

from dqn import DqnTrainer, QNetwork, ReplayBuffer


class TestQNetwork:
    def test_hidden_layers_and_activation(self):
        net = QNetwork(4, 2, hidden=[16, 8], activation="tanh")
        x = torch.zeros(3, 4)
        out = net(x)
        assert out.shape == (3, 2)
        # 16 -> 8 -> 2 三层 Linear
        linear_layers = [m for m in net.net if isinstance(m, torch.nn.Linear)]
        assert [l.in_features for l in linear_layers] == [4, 16, 8]
        assert [l.out_features for l in linear_layers] == [16, 8, 2]

    def test_scalar_hidden_is_backward_compatible(self):
        net = QNetwork(4, 2, hidden=16)
        assert net(torch.zeros(1, 4)).shape == (1, 2)

    def test_unknown_activation_fails_fast(self):
        with pytest.raises(ValueError, match="激活函数"):
            QNetwork(4, 2, activation="swish")


class TestReplayBuffer:
    def test_dt_column_round_trip(self, tmp_path):
        buf = ReplayBuffer(100, 2)
        states = np.random.rand(5, 2).astype(np.float32)
        next_states = np.random.rand(5, 2).astype(np.float32)
        buf.add(states, np.array([0, 1, 0, 1, 0]),
                np.arange(5, dtype=np.float32), next_states,
                np.array([1, 2, 3, 4, 5]), np.zeros(5))
        assert len(buf) == 5
        s, a, r, ns, dt, term = buf.sample(4)
        assert s.shape == (4, 2)
        assert dt.shape == (4,)

        # 持久化往返
        f = tmp_path / "buffer.npz"
        buf.save(f)
        buf2 = ReplayBuffer(100, 2)
        buf2.load(f)
        assert np.array_equal(buf2.dts, buf.dts)
        assert np.array_equal(buf2.states, buf.states)

    def test_capacity_evicts_oldest(self):
        buf = ReplayBuffer(10, 2)
        buf.add(np.zeros((8, 2)), np.zeros(8), np.zeros(8), np.zeros((8, 2)), np.ones(8), np.zeros(8))
        buf.add(np.ones((8, 2)), np.ones(8), np.ones(8), np.ones((8, 2)), np.ones(8), np.zeros(8))
        assert len(buf) == 10
        assert np.allclose(buf.states[-8:], 1.0)


class TestDqnTrainer:
    def _trainer(self, **kw):
        args = dict(hidden=[8], lr=1e-3, gamma=0.5, batch_size=4,
                    buffer_capacity=1000, min_buffer=1, tau=0.01, seed=7)
        args.update(kw)
        return DqnTrainer(2, 2, **args)

    def test_gamma_power_dt_discount(self, monkeypatch):
        """不定长转移必须按 γ^dt 折扣：验证 dt 确实作为指数进入 target 计算。"""
        trainer = self._trainer()
        dt_val = 3.0
        trainer.add_experiences(
            np.zeros((8, 2), dtype=np.float32), np.zeros(8, dtype=np.int64),
            np.ones(8, dtype=np.float32), np.zeros((8, 2), dtype=np.float32),
            np.full(8, dt_val, dtype=np.float32), np.zeros(8, dtype=np.float32))

        captured = {}
        real_pow = torch.pow

        def spy(base, exp):
            captured["exponent"] = exp
            return real_pow(base, exp)

        monkeypatch.setattr(torch, "pow", spy)
        steps, loss = trainer.train_steps(2)
        assert steps == 2
        assert not np.isnan(loss)
        assert "exponent" in captured
        assert torch.allclose(captured["exponent"],
                              torch.full((4,), dt_val)), "折扣指数应为 dt"

    def test_annuity_correction_scales_interval_reward(self, monkeypatch):
        """年金校正：dt>1 的区间奖励按 (1-γ^dt)/((1-γ)·dt) 折算；terminated 转移的 target 即校正后奖励。"""
        trainer = self._trainer()   # gamma=0.5
        dt_val = 4.0
        trainer.add_experiences(
            np.zeros((8, 2), dtype=np.float32), np.zeros(8, dtype=np.int64),
            np.ones(8, dtype=np.float32), np.zeros((8, 2), dtype=np.float32),
            np.full(8, dt_val, dtype=np.float32), np.ones(8, dtype=np.float32))  # terminated=1 -> bootstrap 清零

        captured = {}
        real_loss = torch.nn.functional.smooth_l1_loss

        def spy(input, target, **kw):
            captured["target"] = target.detach().clone()
            return real_loss(input, target, **kw)

        monkeypatch.setattr(torch.nn.functional, "smooth_l1_loss", spy)
        steps, loss = trainer.train_steps(1)
        assert steps == 1
        gamma, dt = 0.5, dt_val
        annuity = (1 - gamma ** dt) / ((1 - gamma) * dt)
        assert torch.allclose(captured["target"],
                              torch.full((4,), annuity, dtype=torch.float32)), \
            "target 应为年金校正后的奖励（bootstrap 已按 terminated 清零）"

    def test_annuity_factor_is_one_for_unit_dt(self, monkeypatch):
        """dt=1 时年金因子恒为 1，单 bar 转移行为与校正前一致。"""
        trainer = self._trainer()
        trainer.add_experiences(
            np.zeros((8, 2), dtype=np.float32), np.zeros(8, dtype=np.int64),
            np.ones(8, dtype=np.float32), np.zeros((8, 2), dtype=np.float32),
            np.ones(8, dtype=np.float32), np.ones(8, dtype=np.float32))

        captured = {}
        real_loss = torch.nn.functional.smooth_l1_loss

        def spy(input, target, **kw):
            captured["target"] = target.detach().clone()
            return real_loss(input, target, **kw)

        monkeypatch.setattr(torch.nn.functional, "smooth_l1_loss", spy)
        trainer.train_steps(1)
        assert torch.allclose(captured["target"], torch.ones(4)), "dt=1 时校正因子为 1，target 即原始奖励"

    def test_checkpoint_round_trip_and_signature(self, tmp_path):
        trainer = self._trainer()
        trainer.add_experiences(
            np.random.rand(16, 2).astype(np.float32), np.zeros(16, dtype=np.int64),
            np.zeros(16, dtype=np.float32), np.random.rand(16, 2).astype(np.float32),
            np.ones(16, dtype=np.float32), np.zeros(16, dtype=np.float32))
        trainer.train_steps(5)
        sig1 = trainer.checkpoint_signature()
        assert sig1.startswith("sha256:")

        ckpt = tmp_path / "checkpoint.pt"
        trainer.save_checkpoint(ckpt)
        assert ckpt.exists()
        assert not (tmp_path / "checkpoint.pt.tmp").exists(), "临时文件应已原子替换"

        fresh = self._trainer()
        fresh.load_checkpoint(ckpt)
        assert fresh.total_steps == trainer.total_steps == 5
        # 同一网络结构 + 同一优化器 + 同一累计步数 -> 签名一致
        assert fresh.checkpoint_signature() == sig1

        fresh.add_experiences(
            np.random.rand(4, 2).astype(np.float32), np.zeros(4, dtype=np.int64),
            np.zeros(4, dtype=np.float32), np.random.rand(4, 2).astype(np.float32),
            np.ones(4, dtype=np.float32), np.zeros(4, dtype=np.float32))
        fresh.train_steps(1)
        assert fresh.checkpoint_signature() != sig1, "累计步数变化应改变签名"

    def test_export_torchscript_loadable(self, tmp_path):
        trainer = self._trainer()
        out = tmp_path / "model.pt"
        trainer.export_torchscript(out)
        loaded = torch.jit.load(str(out))
        assert loaded(torch.zeros(1, 2)).shape == (1, 2)
        assert not (tmp_path / "model.pt.tmp").exists()

    def test_train_skips_when_buffer_too_small(self):
        trainer = self._trainer(min_buffer=100)
        trainer.add_experiences(
            np.zeros((4, 2), dtype=np.float32), np.zeros(4, dtype=np.int64),
            np.zeros(4, dtype=np.float32), np.zeros((4, 2), dtype=np.float32),
            np.ones(4, dtype=np.float32), np.zeros(4, dtype=np.float32))
        steps, loss = trainer.train_steps(10)
        assert steps == 0 and np.isnan(loss)
