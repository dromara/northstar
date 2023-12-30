import os
import numpy as np
import logging
import tensorflow as tf
from tensorflow.keras import layers, losses, Model
from utils import ReplayBuffer
from flask import Flask, request, jsonify


# 网络定义
class DQN(Model):
    def __init__(self, action_size):
        super(DQN, self).__init__()
        self.dense1 = layers.Dense(24, activation='relu')
        self.dense2 = layers.Dense(24, activation='relu')
        self.dense3 = layers.Dense(action_size, activation='linear')

    def call(self, state):
        x = self.dense1(state)
        x = self.dense2(x)
        return self.dense3(x)
    
# 交互代理
class DQNAgent:
    def __init__(self, state_size, action_size):
        self.state_size = state_size
        self.action_size = action_size
        self.memory = ReplayBuffer(10000)
        self.learning_batch_size = 64
        self.gamma = 0.95  # 折扣因子
        self.epsilon = 1.0  # 探索率
        self.epsilon_min = 0.01
        self.epsilon_decay = 0.995
        self.train_model = DQN(action_size)
        self.target_model = DQN(action_size)
        self.train_count = 0
        self.train_model.compile(optimizer='adam', loss='mse')
        self.target_model.compile(optimizer='adam', loss='mse')

    def remember(self, state, action, reward, next_state, done):
        state = np.reshape(state, (1, self.state_size))
        next_state = np.reshape(next_state, (1, self.state_size))
        action = np.reshape(action, (1,1))
        reward = np.reshape(reward, (1,1))
        done = np.reshape(done, (1,1))
        self.memory.add(state, action, reward, next_state, done)

    def act(self, state):
        state = np.reshape(state, (1, self.state_size))
        if np.random.rand() <= self.epsilon:
            return np.random.choice(self.action_size)
        q_values = self.train_model.predict(state, verbose=0)
        return np.argmax(q_values[0])

    def train(self):
        batch_size = self.learning_batch_size
        if self.memory.size() < batch_size:
            return
        self.train_count += 1
        states, actions, rewards, next_states, dones = self.memory.sample(batch_size)
        q_values = self.train_model.predict(states, verbose=0)
        q_targets = self.target_model.predict(states, verbose=0)
        next_q_values = self.target_model.predict(next_states, verbose=0)
        for i in range(batch_size):
            q_targets[i][0][actions[i]] = rewards[i] + self.gamma * np.max(next_q_values[i]) * (1 - dones[i])
        self.train_model.fit(states, q_targets, verbose=0)
        if self.epsilon > self.epsilon_min:
            self.epsilon *= self.epsilon_decay

    def updateModel(self):
        self.target_model.set_weights(self.train_model.get_weights())
            
    def load(self, name):
        path = os.path.expanduser(f'~/northstar-agent/dqn/{name}.tf')
        if os.path.exists(path):        
            self.train_model.load_weights(path)
            self.target_model.load_weights(path)

    def save(self, name):
        path = os.path.expanduser(f'~/northstar-agent/dqn/{name}.tf')        
        self.train_model.save_weights(path)
        self.target_model.save_weights(path)


logging.basicConfig(level=logging.DEBUG)
app = Flask(__name__)
model = DQNAgent(4, 2)


@app.route("/react", methods=["POST"])
def react():
    data = request.json
    state = data.get('values')
    return {'value': model.act(state)}

@app.route("/learn", methods=["POST"])
def learn():
    data = request.json

    # 处理数据和模型训练
    try:
        state = (data.get('state') or {}).get('values')
        action = (data.get('action') or {}).get('value')
        reward = (data.get('reward') or {}).get('value')
        next_state = (data.get('nextState') or {}).get('values')
        done = data.get('terminated')
        model.remember(state, action, reward, next_state, done)
        model.train()
        return jsonify({"status": "success"}), 200
    except Exception as e:
        # 如果有错误，返回错误信息
        return jsonify({"status": "error", "message": str(e)}), 500

@app.route("/update", methods=["GET"])
def update():    
    try:
        model.updateModel()
        return jsonify({"status": "success"}), 200
    except Exception as e:
        return jsonify({"status": "error", "message": str(e)}), 500

@app.route("/save", methods=["GET"])
def save():
    try:
        name = request.args.get('name')
        model.save(name)
        return jsonify({"status": "success"}), 200
    except Exception as e:
        return jsonify({"status": "error", "message": str(e)}), 500

@app.route("/load", methods=["GET"])
def load():
    try:
        name = request.args.get('name')
        model.load(name)
        return jsonify({"status": "success"}), 200
    except Exception as e:
        return jsonify({"status": "error", "message": str(e)}), 500


app.run(port=5002)