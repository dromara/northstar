import time
import random
import numpy as np
import gymnasium as gym
from flask import Flask, request, jsonify
from stable_baselines3.common.buffers import RolloutBuffer
from stable_baselines3 import PPO
from env import TradingEnv

app = Flask(__name__)

env = TradingEnv()
model = PPO('MlpPolicy', env, verbose=1)
obs = env.reset()

last_action = [0]

@app.route('/init-info', methods=['POST'])
def init_info():
    data = request.get_json()
    is_train = data['is_train']
    agent_name = data['agent_name']
    return jsonify({"success": True})


@app.route('/get-action', methods=['POST'])
def get_action():
    global last_action
    start_time = time.time()
    data = request.get_json()
    env.update_data(data)
    obs, rewards, dones, info = env.step(last_action)
    action, _states = model.predict(obs)
    last_action = action
    print(f"action type: {type(action)}")
    end_time = time.time()
    print(f"get_action time: {end_time - start_time}, data: {data}, obs: {obs}, rewards: {rewards}, action: {action}")
    return jsonify({"action": int(action)})

app.run(port=5001)