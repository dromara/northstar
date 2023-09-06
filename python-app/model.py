import gymnasium as gym
import numpy as np

from stable_baselines3 import PPO
from stable_baselines3.common.env_util import make_vec_env
from stable_baselines3.common.buffers import RolloutBuffer

model = PPO(
    policy="MlpPolicy",
    env=None
)
model.observation_space = gym.spaces.Box(low=0, high=np.inf, shape=(4,), dtype=np.float32)
model.action_space = gym.Space.Discrete(3, dtype=np.float32)
