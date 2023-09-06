import math
import gymnasium as gym
from gymnasium import Space
import numpy as np

class TradingEnv(gym.Env):
    def __init__(self,
                 
                 ):
        self.observation_space = gym.spaces.Box(low=0, high=np.inf, shape=(4,), dtype=np.float32)
        self.action_space = gym.spaces.Discrete(3)
        
        self.state = self._initiate_state()

    def _initiate_state(self):
        return [
            0, # open price
            0, # high price
            0, # low price
            0, # closed price
        ]
     
    def reset(self):
        self.state = self._initiate_state()
        
    def render(self, mode="human", close=False):
        return self.state
    
    def update_data(self, data):
        self.state = [
            data['open_price'],
            data['high_price'],
            data['low_price'],
            data['close_price']
        ]
        self.last_reward = data['last_reward']
    
    def step(self, action):
        return self.state, self.last_reward, False, {}
        
        
        