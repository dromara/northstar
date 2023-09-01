import math
import gym
import numpy as np

class TradingEnv(gym.Env):
    def __init__( self,
                  trading_fee_rate=0.0001,
                  
                 ):
        self.current_hold = 0
        self.observation_space = gym.spaces.Box(low=0, high=np.inf, shape=(4,), dtype=np.float32)
        self.action_space = spaces.Discrete(3, dtype=np.float32)
        
        self.state = self._initiate_state()
        
        self.trading_fee_rate = trading_fee_rate
    
    
     
    def reset(self):
        self.state = self._initiate_state()
        
    def render(self, mode="human", close=False):
        return self.state
    
    def step(self, action):
        self.step += 1
        
        state = self.get_price(self.step)
        
        
        