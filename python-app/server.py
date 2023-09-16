import os
import time
import random
import numpy as np
import torch as th
import gymnasium as gym
from flask import Flask, request, jsonify
from agents.ppo import PPO, RolloutBuffer
from agents.dqn import DQN
from stable_baselines3.common.utils import obs_as_tensor
from env import TradingEnv

app = Flask(__name__)

AGENTS = {
    "ppo": PPO,
    "dqn": DQN,
}

class Server:
    def __init__(self) -> None:
        self.agent_name = None
        self.is_train = None
        self.models = {}
        self.action = None
        self.state_dim = 4
        self.action_dim = 3
        self.n_iter = 0
        
    def _get_model_id(self, indicator_symbol, model_version):
        return f"{indicator_symbol}-{self.agent_name}-{model_version}"
    
    def load_model(self, indicator_symbol, model_version):
        model = AGENTS[self.agent_name](self.state_dim, self.action_dim)
        self.models[indicator_symbol] = model
        
    def load_model_from_file(self, indicator_symbol, model_version):
        model_id = self._get_model_id(indicator_symbol, model_version)
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
        # print(data)
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
        
        
        model = self.models[unified_symbol]
        state = [open_price, high_price, low_price, close_price]
        self.action = model.select_action(state, last_reward)
        print(f"action: {self.action}")
        
        if not os.path.exists("models"):
            os.makedirs("models")

        if self.n_iter % 100 == 0 and self.n_iter != 0:
            model.update()
            model.save(f"models/model_{self.n_iter}.pth")
        self.n_iter += 1
            
        return jsonify({"action": self.action})