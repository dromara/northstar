import os
import time
import random
import numpy as np
import torch as th
import gymnasium as gym
from datetime import datetime
from flask import Flask, request, jsonify
from agents.ppo import PPO, RolloutBuffer
from agents.dqn import DQN
from stable_baselines3.common.utils import obs_as_tensor

app = Flask(__name__)

AGENTS = {
    "ppo": PPO,
    "dqn": DQN,
}

class Server:
    def __init__(self) -> None:
        self.n_iter = 0
        self.timestamp = datetime.now().strftime("%Y-%m-%d-%H-%M-%S")
            
    def load_model(self):
        self.model = AGENTS[self.agent_name](
            state_dim=self.state_dim,
            action_dim=self.action_dim,
            lr_actor=self.lr_actor,
            lr_critic=self.lr_critic,
            gamma=self.gamma,
            K_epochs=self.k_epochs,
            eps_clip=self.eps_clip
        )
        return True
        
    def load_model_from_file(self):
        if os.path.exists(self.model_path):
            self.model = AGENTS[self.agent_name]()
            self.model.load(checkpoint_path=self.model_path)
            return True
        else:
            return False

    def init_info(self, data):
        # load data from request
        self.is_train = data["isTrain"]
        self.indicator_symbol = data["indicatorSymbol"]
        
        # if train, load model from scratch; else, load model from file
        if self.is_train:
            self.agent_name = data["agentName"]
            self.state_dim = data["stateDim"]
            self.action_dim = data["actionDim"]
            self.lr_actor = data["lrActor"]
            self.lr_critic = data["lrCritic"]
            self.gamma = data["gamma"]
            self.k_epochs = data["kEpochs"]
            self.eps_clip = data["epsClip"]
            return {"success": self.load_model()}
        else:
            self.model_path = data["modelPath"]
            return {"success": self.load_model_from_file()}


    def get_action(self, data):
        # load data from request
        data = request.get_json()
        open_price = data["openPrice"]
        high_price = data["highPrice"]
        low_price = data["lowPrice"]
        close_price = data["closePrice"]
        last_reward = data["lastReward"]
        
        state = [open_price, high_price, low_price, close_price]
        self.action = self.model.select_action(state, last_reward)
        print(f"action: {self.action}")
        if self.is_train:
            folder = f"models/{self.agent_name}/{self.timestamp}"
            if not os.path.exists(folder):
                os.makedirs(folder)

            if self.n_iter % 1000 == 0 and self.n_iter != 0:
                self.model.update()
                self.model.save(f"{folder}/{self.indicator_symbol}-{self.n_iter}.pth")
                self.model.save(f"{folder}/{self.indicator_symbol}-latest.pth")
        
        self.n_iter += 1
            
        return {"actionID": self.action}