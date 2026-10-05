"""pytest 配置：把 training/ 目录加入 sys.path（train_round.py 与 dqn 包的所在目录）。"""
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))
