import os
import time
import random
import numpy as np
import gymnasium as gym
from flask import Flask, request, jsonify
from stable_baselines3.common.buffers import RolloutBuffer
from stable_baselines3 import PPO
from env import TradingEnv

app = Flask(__name__)

AGENTS = {
    "ppo": PPO
}

env = TradingEnv()
model = PPO("MlpPolicy", env, verbose=1)
obs = env.reset()

last_action = [0]


class Server:
    def __init__(self) -> None:
        self.agent_name = None
        self.is_train = None
        self.models = {}
        
    def get_model_id(self, indicator_symbol, model_version):
        return f"{indicator_symbol}-{self.agent_name}-{model_version}"
    
    def load_model(self, indicator_symbol, model_version):
        model = AGENTS[self.agent_name]("MlpPolicy")
        model_id = self.get_model_id(indicator_symbol, model_version)
        self.models[model_id] = model
        
    def load_model_from_file(self, indicator_symbol, model_version):
        model_id = self.get_model_id(indicator_symbol, model_version)
        model_path = f"models/{model_id}"
        if os.exists(model_path):
            model = AGENTS[self.agent_name]("MlpPolicy")
            model.load(model_path)
            self.models[model_id] = model
            return True
        else:
            return False
    
    @app.route("/init-info", methods=["POST"])
    def init_info(self):
        # load data from request
        data = request.get_json()
        print(f"init_info: {data}")
        self.agent_name = data["agent_name"].lower()
        self.is_train = data["is_train"]
        indicator_symbol = data["indicator_symbol"]
        model_version = data["model_version"]
        
        if self.is_train:
            self.load_model(indicator_symbol, model_version)
            return jsonify({"success": True})
        else:
            if self.load_model_from_file(indicator_symbol, model_version):
                return jsonify({"success": True})
            else:
                return jsonify({"success": False})
        
        


    @app.route("/get-action", methods=["POST"])
    def get_action():
        global last_action
        start_time = time.time()
        data = request.get_json()
        env.update_data(data)
        obs, rewards, dones, info = env.step(last_action)
        action, _states = model.predict(obs)
        last_action = action
        # print(f"action type: {type(action)}")
        end_time = time.time()
        # print(f"get_action time: {end_time - start_time}, data: {data}, obs: {obs}, rewards: {rewards}, action: {action}")
        return jsonify({"action": int(action)})
    
if __name__ == "__main__":
    app.run(port=5001)