"""train_round.py 端到端测试：版本目录契约、增量读取、预处理冻结、checkpoint 签名、algo 分发。"""
import json
import sys
from pathlib import Path

import numpy as np
import pytest

import train_round
from expstore import list_shards, load_shard


def _write_csv(path, rows, state_dim=4):
    header = ",".join([f"s{i}" for i in range(state_dim)] + ["action", "reward"]
                      + [f"ns{i}" for i in range(state_dim)] + ["dt", "terminated"])
    lines = [header]
    rng = np.random.default_rng(7)
    for _ in range(rows):
        s = rng.normal(size=state_dim).round(4)
        ns = rng.normal(size=state_dim).round(4)
        a = rng.integers(0, 2)
        r = round(float(rng.normal()), 4)
        dt = int(rng.integers(1, 5))
        term = int(rng.random() < 0.1)
        lines.append(",".join([str(v) for v in s] + [str(a), str(r)]
                              + [str(v) for v in ns] + [str(dt), str(term)]))
    path.write_text("\n".join(lines) + "\n", encoding="utf-8")


def _make_model_dir(tmp_path, algo="DQN", preprocessing=None, hyper_params=None):
    model_dir = tmp_path / "m"
    version_dir = model_dir / "versions" / "v1"
    (version_dir / "rounds").mkdir(parents=True)
    (model_dir / "metadata.json").write_text(json.dumps({
        "name": "rl-trend-dqn", "algo": algo, "state_dim": 4, "action_dim": 2,
        "actions": ["LEFT", "RIGHT"],
        "preprocessing": {"type": "identity"}, "spec_hash": "x", "latest_version": 1,
    }), encoding="utf-8")
    (version_dir / "metadata.json").write_text(json.dumps({
        "version": 1, "parent_version": 0, "weight_inherited": False, "spec_hash": "x",
        "preprocessing": preprocessing or {"type": "identity"},
        "training": {"rounds": 3, "hyper_params": hyper_params or {
            "lr": 0.001, "gamma": 0.99, "batchSize": 8, "bufferCapacity": 1000,
            "minBuffer": 4, "tau": 0.01, "maxGradSteps": 50}, "seed": 42},
    }), encoding="utf-8")
    return model_dir, version_dir


def _run(model_dir, *extra, seed="1"):
    argv = ["train_round.py", "--model-dir", str(model_dir), "--version", "1",
            "--grad-steps", "5", "--seed", str(seed)] + list(extra)
    old = sys.argv
    sys.argv = argv
    try:
        train_round.main()
    finally:
        sys.argv = old


class TestShardIO:
    def test_list_and_load(self, tmp_path):
        rounds = tmp_path / "rounds"
        rounds.mkdir()
        _write_csv(rounds / "replay-r0-c1.csv", 3)
        _write_csv(rounds / "replay-r0-c0.csv", 5)
        _write_csv(rounds / "replay-r1-c0.csv", 2)
        (rounds / "notes.txt").write_text("not a shard")

        shards = list_shards(rounds)
        assert [p.name for p in shards] == ["replay-r0-c0.csv", "replay-r0-c1.csv", "replay-r1-c0.csv"]

        s, a, r, ns, dt, term = load_shard(shards[0], 4)
        assert s.shape == (5, 4)
        assert ns.shape == (5, 4)
        assert dt.shape == (5,)
        assert set(np.unique(a)).issubset({0, 1})

    def test_load_rejects_wrong_columns(self, tmp_path):
        bad = tmp_path / "replay-r0-c0.csv"
        bad.write_text("a,b\n1,2\n")
        with pytest.raises(ValueError, match="列数"):
            load_shard(bad, 4)


class TestTrainRound:
    def test_first_round_produces_all_artifacts(self, tmp_path, capsys):
        model_dir, version_dir = _make_model_dir(tmp_path)
        _write_csv(version_dir / "rounds" / "replay-r0-c0.csv", 20)

        _run(model_dir)
        out = capsys.readouterr().out

        assert "LOSS=" in out and "TRAINED_STEPS=5" in out and "NEW_SAMPLES=20" in out
        assert (version_dir / "checkpoint.pt").exists()
        assert (version_dir / "model.pt").exists()
        assert (version_dir / "buffer.npz").exists()
        ingested = json.loads((version_dir / "ingested.json").read_text())
        assert ingested == ["replay-r0-c0.csv"]

        ver_meta = json.loads((version_dir / "metadata.json").read_text())
        assert ver_meta["checkpoint_signature"].startswith("sha256:")
        # 网络结构由 registry 定义并回传（不再来自模型级 metadata 的 network 字段）
        assert ver_meta["network"] == {"type": "mlp", "hidden": [128, 128], "activation": "relu"}

        # model.pt 可被 TorchScript 加载（DJL 同格式）
        import torch
        net = torch.jit.load(str(version_dir / "model.pt"))
        assert net(torch.zeros(1, 4)).shape == (1, 2)

    def test_incremental_ingestion_and_step_accumulation(self, tmp_path, capsys):
        model_dir, version_dir = _make_model_dir(tmp_path)
        _write_csv(version_dir / "rounds" / "replay-r0-c0.csv", 10)
        _run(model_dir)
        sig1 = json.loads((version_dir / "metadata.json").read_text())["checkpoint_signature"]

        # 第二轮：新增分片，旧分片不重复入库
        _write_csv(version_dir / "rounds" / "replay-r1-c0.csv", 8)
        _run(model_dir)
        out = capsys.readouterr().out
        assert "NEW_SAMPLES=8" in out

        buf = np.load(version_dir / "buffer.npz")
        assert len(buf["states"]) == 18, "两轮样本累积入持久化 buffer"
        assert len(buf["dts"]) == 18

        sig2 = json.loads((version_dir / "metadata.json").read_text())["checkpoint_signature"]
        assert sig1 != sig2, "累计步数变化应更新签名"

    def test_standardize_stats_frozen_on_first_train(self, tmp_path):
        model_dir, version_dir = _make_model_dir(
            tmp_path, preprocessing={"type": "standardize", "mean": [], "std": []})
        _write_csv(version_dir / "rounds" / "replay-r0-c0.csv", 20)

        _run(model_dir)
        ver_meta = json.loads((version_dir / "metadata.json").read_text())
        pp = ver_meta["preprocessing"]
        assert pp["type"] == "standardize"
        assert len(pp["mean"]) == 4 and len(pp["std"]) == 4, "统计量冻结写入版本 metadata"

        # 第二轮：统计量不变（冻结），新数据沿用旧统计量
        frozen_mean = list(pp["mean"])
        _write_csv(version_dir / "rounds" / "replay-r1-c0.csv", 20)
        _run(model_dir)
        pp2 = json.loads((version_dir / "metadata.json").read_text())["preprocessing"]
        assert pp2["mean"] == frozen_mean

        # buffer 中的状态应已被标准化（均值接近 0）
        buf = np.load(version_dir / "buffer.npz")
        assert abs(float(buf["states"].mean())) < 0.5

    def test_lr_override_applies(self, tmp_path):
        model_dir, version_dir = _make_model_dir(tmp_path)
        _write_csv(version_dir / "rounds" / "replay-r0-c0.csv", 10)
        _run(model_dir, "--lr", "0.123")

        import torch
        ckpt = torch.load(version_dir / "checkpoint.pt", weights_only=False)
        assert abs(ckpt["optimizer"]["param_groups"][0]["lr"] - 0.123) < 1e-9

    @pytest.mark.parametrize("seed", [-(2**62), 2**40 + 7, 0])
    def test_out_of_range_seed_accepted(self, tmp_path, capsys, seed):
        """Java 端可能传入 64 位 long 种子（负值或超过 2**32-1），Python 端应自行规整到 numpy 合法范围。"""
        model_dir, version_dir = _make_model_dir(tmp_path)
        _write_csv(version_dir / "rounds" / "replay-r0-c0.csv", 10)
        _run(model_dir, seed=seed)
        assert "TRAINED_STEPS=5" in capsys.readouterr().out
        assert (version_dir / "checkpoint.pt").exists()

    def test_unknown_algo_rejected(self, tmp_path):
        model_dir, _ = _make_model_dir(tmp_path, algo="PPO")
        with pytest.raises(SystemExit) as exc:
            _run(model_dir)
        assert exc.value.code == 2

    def test_missing_version_dir_rejected(self, tmp_path):
        model_dir, version_dir = _make_model_dir(tmp_path)
        import shutil
        shutil.rmtree(version_dir)
        with pytest.raises(SystemExit) as exc:
            _run(model_dir)
        assert exc.value.code == 2


class TestPreprocessingFreezeWithExistingBuffer:
    """统计量冻结时存量 raw buffer 的一次性标准化转换（train_round.py 冻结分支）。"""

    def test_existing_raw_buffer_converted_once_on_freeze(self, tmp_path, capsys):
        from dqn import ReplayBuffer

        model_dir, version_dir = _make_model_dir(
            tmp_path, preprocessing={"type": "standardize", "mean": [], "std": []})

        # 场景：buffer.npz 已存在且为未标准化的 raw states（统计量尚未冻结的遗留数据）
        rng = np.random.default_rng(3)
        raw_states = rng.normal(loc=5.0, scale=2.0, size=(10, 4)).astype(np.float32)
        raw_next = rng.normal(loc=5.0, scale=2.0, size=(10, 4)).astype(np.float32)
        buf = ReplayBuffer(1000, 4)
        buf.add(raw_states, rng.integers(0, 2, 10), rng.normal(size=10).astype(np.float32),
                raw_next, np.ones(10, dtype=np.float32), np.zeros(10, dtype=np.float32))
        buf.save(version_dir / "buffer.npz")

        # 新增一轮经验分片（raw）
        _write_csv(version_dir / "rounds" / "replay-r0-c0.csv", 20)
        new_states, _, _, new_next, _, _ = load_shard(version_dir / "rounds" / "replay-r0-c0.csv", 4)

        _run(model_dir)
        assert "PREPROCESSING_FROZEN" in capsys.readouterr().out

        # 冻结的统计量应基于 存量 + 新增 合并计算
        stacked = np.concatenate([raw_states, new_states])
        mean = stacked.mean(axis=0)
        std = stacked.std(axis=0)
        std[std == 0] = 1.0
        pp = json.loads((version_dir / "metadata.json").read_text())["preprocessing"]
        assert pp["mean"] == [round(float(v), 8) for v in mean]
        assert pp["std"] == [round(float(v), 8) for v in std]

        # 存量 raw 状态已被一次性转换为标准化值；新增分片按冻结统计量标准化后入库
        saved = np.load(version_dir / "buffer.npz")
        assert np.allclose(saved["states"][:10], (raw_states - mean) / std, atol=1e-5)
        assert np.allclose(saved["next_states"][:10], (raw_next - mean) / std, atol=1e-5)
        assert np.allclose(saved["states"][10:], (new_states - mean) / std, atol=1e-5)
        assert np.allclose(saved["next_states"][10:], (new_next - mean) / std, atol=1e-5)

        # 第二轮重跑（无新增分片）：统计量不变，存量数据不重复转换
        states_after_round1 = saved["states"].copy()
        next_after_round1 = saved["next_states"].copy()
        saved.close()  # NpzFile 持有文件句柄，Windows 上不关闭会阻塞第二轮的原子替换
        _run(model_dir)
        pp2 = json.loads((version_dir / "metadata.json").read_text())["preprocessing"]
        assert pp2["mean"] == pp["mean"] and pp2["std"] == pp["std"], "统计量冻结后不得重算"
        with np.load(version_dir / "buffer.npz") as saved2:
            assert np.array_equal(saved2["states"], states_after_round1), "冻结后不得二次转换存量数据"
            assert np.array_equal(saved2["next_states"], next_after_round1)


class TestAtomicWrite:
    """原子写：一轮完整训练后版本目录不残留任何临时文件，且产物完整可读。"""

    def test_no_tmp_residue_and_artifacts_intact(self, tmp_path):
        model_dir, version_dir = _make_model_dir(tmp_path)
        _write_csv(version_dir / "rounds" / "replay-r0-c0.csv", 20)
        _run(model_dir)

        leftovers = [p.name for p in version_dir.rglob("*") if ".tmp" in p.name]
        assert leftovers == [], f"原子写不应残留临时文件: {leftovers}"

        buf = np.load(version_dir / "buffer.npz")
        assert set(buf.files) == {"states", "actions", "rewards", "next_states", "dts", "terminated"}
        assert len(buf["states"]) == 20
        assert json.loads((version_dir / "ingested.json").read_text(encoding="utf-8")) == ["replay-r0-c0.csv"]
        ver_meta = json.loads((version_dir / "metadata.json").read_text(encoding="utf-8"))
        assert ver_meta["checkpoint_signature"].startswith("sha256:")


class TestIngestionCrashConsistency:
    def test_crash_between_buffer_save_and_ingested_write_reingests(self, tmp_path, capsys):
        """崩溃一致性（注释级行为记录，不强制改代码）：

        train_round 先原子保存 buffer.npz，再原子写 ingested.json。若进程在两者之间崩溃，
        下一轮会因 ingested.json 缺失/过期而把已入库分片重复读入 buffer（at-least-once）。
        该顺序是有意为之：重复样本只轻微偏移采样分布，而反过来（先写 ingested 后存 buffer）
        会在崩溃时永久丢失经验，故现状即最优顺序，无需额外协调。
        """
        model_dir, version_dir = _make_model_dir(tmp_path)
        _write_csv(version_dir / "rounds" / "replay-r0-c0.csv", 10)
        _run(model_dir)
        assert len(np.load(version_dir / "buffer.npz")["states"]) == 10

        # 模拟崩溃点：buffer.npz 已保存，ingested.json 尚未写出
        (version_dir / "ingested.json").unlink()
        _run(model_dir)
        out = capsys.readouterr().out
        assert "NEW_SAMPLES=10" in out, "分片应被重复入库（at-least-once）"
        assert len(np.load(version_dir / "buffer.npz")["states"]) == 20
