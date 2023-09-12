import os
import time
import random
import numpy as np
import torch as th
import gymnasium as gym
from flask import Flask, request, jsonify
# from buffer import TradingRolloutBuffer
# from stable_baselines3 import PPO
from agents.ppo import PPO, RolloutBuffer
from stable_baselines3.common.utils import obs_as_tensor
from env import TradingEnv

app = Flask(__name__)

AGENTS = {
    "ppo": PPO
}

# env = TradingEnv()
# model = PPO("MlpPolicy", env, verbose=1)
# obs = env.reset()


class Server:
    def __init__(self) -> None:
        self.agent_name = None
        self.is_train = None
        self.models = {}
        self.observation_space = gym.spaces.Box(low=0, high=np.inf, shape=(4,), dtype=np.float32)
        self.action_space = gym.spaces.Discrete(3)
        self.rollout_buffer = RolloutBuffer()
        self.action = [0]
        
    def get_model_id(self, indicator_symbol, model_version):
        return f"{indicator_symbol}-{self.agent_name}-{model_version}"
    
    def load_model(self, indicator_symbol, model_version):
        model = AGENTS[self.agent_name](state_dim=4,
                                        action_dim=1,
                                        lr_actor=0.999,
                                        lr_critic=0.999,
                                        gamma=0.999,
                                        K_epochs=4,
                                        eps_clip=0.2,
                                        has_continuous_action_space=False)
        model_id = self.get_model_id(indicator_symbol, model_version)
        self.models[indicator_symbol] = model
        
    def load_model_from_file(self, indicator_symbol, model_version):
        model_id = self.get_model_id(indicator_symbol, model_version)
        model_path = f"models/{model_id}"
        if os.path.exists(model_path):
            model = AGENTS[self.agent_name]
            model.load(model_path)
            self.models[indicator_symbol] = model
            return True
        else:
            print(f"Model {model_id} not found")
            self.load_model(indicator_symbol, model_version)
            return False

    def init_info(self, data):
        # load data from request
        self.agent_name = data["agent_name"].lower()
        self.is_train = data["is_train"]
        indicator_symbol = data["indicator_symbol"]
        model_version = data["model_version"]
        print(data)
        # if train, load model from scratch; else, load model from file
        if self.is_train:
            self.load_model(indicator_symbol, model_version)
            return jsonify({"success": True})
        else:
            if self.load_model_from_file(indicator_symbol, model_version):
                return jsonify({"success": True})
            else:
                return jsonify({"success": False})


    def get_action(self, data):
        start_time = time.time()
        
        # load data from request
        data = request.get_json()
        unified_symbol = data["unified_symbol"]
        open_price = data["open_price"]
        high_price = data["high_price"]
        low_price = data["low_price"]
        close_price = data["close_price"]
        last_reward = data["last_reward"]
        print(self.models.keys())
        model = self.models[unified_symbol]
        state = [open_price, high_price, low_price, close_price]
        action = model.select_action(state)
       
        # with th.no_grad():
        #     # Convert to pytorch tensor or to TensorDict
        #     obs_tensor = obs_as_tensor(state, self.device)
        #     actions, values, log_probs = model.policy(obs_tensor)
        # actions = actions.cpu().numpy()
        
        # if self.is_train:
        #     self.rollout_buffer.add(
        #         obs_tensor,
        #         actions,
        #         last_reward,
        #         self._last_episode_starts,
        #         values,
        #     )    
        
        #     if self.rollout_buffer.full():
        #         model.rollout_buffer = self.rollout_buffer
        #         model.train() # 后续实现并发
        #         self.rollout_buffer.reset()
            
        return jsonify({"action": int(action)})