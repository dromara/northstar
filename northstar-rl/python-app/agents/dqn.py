import torch
import torch.nn as nn
from .base import BaseRL
from utils.device import set_device

# from ..utils import set_device

class DQN(BaseRL):
    def __init__(self,
                 state_dim, 
                 action_dim):
        
        self.state_dim = state_dim
        self.action_dim = action_dim
        
        # self.device = set_device()
        
        self.Q = nn.Sequential(
            nn.Linear(state_dim, 64),
            nn.ReLU(),
            nn.Linear(64, 64),
            nn.ReLU(),
            nn.Linear(64, action_dim)
        )
        
        self.Q_target = nn.Sequential(
            nn.Linear(state_dim, 64),
            nn.ReLU(),
            nn.Linear(64, 64),
            nn.ReLU(),
            nn.Linear(64, action_dim)
        )
        
        
        
            
    #     pass
    
    # def select_action(self, state, last_reward):
    #     with torch.no_grad():
    #         state = torch.FloatTensor(state).to(self.device)
    #         action, action_logprob = 