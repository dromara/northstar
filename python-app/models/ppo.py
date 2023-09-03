import warnings
from typing import Any, ClassVar, Dict, Optional, Type, TypeVar, Union
import torch
import numpy as np
from gymnasium import spaces
from torch.nn import functional as F

from stable_baselines3.common.base_class import BaseAlgorithm
from stable_baselines3.common.on_policy_algorithm import OnPolicyAlgorithm
from stable_baselines3.common.policies import ActorCriticCnnPolicy, ActorCriticPolicy, BasePolicy, MultiInputActorCriticPolicy
from stable_baselines3.common.type_aliases import GymEnv, MaybeCallback, Schedule
from stable_baselines3.common.utils import get_device, explained_variance, get_schedule_fn
from stable_baselines3.ppo.ppo import PPO
from stable_baselines3.common.buffers import RolloutBuffer

class ModifiedPPO(BaseAlgorithm):
    policy_aliases: ClassVar[Dict[str, Type[BasePolicy]]] = {
        "MlpPolicy": ActorCriticPolicy,
        "CnnPolicy": ActorCriticCnnPolicy,
        "MultiInputPolicy": MultiInputActorCriticPolicy,
    }
    
    class __init__():
        def __init__(
            self,
            policy: Union[str, Type[ActorCriticPolicy]],
            learning_rate: Union[float, Schedule] = 3e-4,
            n_steps: int = 2048,
            batch_size: int = 64,
            n_epochs: int = 10,
            gamma: float = 0.99,
            gae_lambda: float = 0.95,
            clip_range: Union[float, Schedule] = 0.2,
            clip_range_vf: Union[None, float, Schedule] = None,
            normalize_advantage: bool = True,
            ent_coef: float = 0.0,
            vf_coef: float = 0.5,
            max_grad_norm: float = 0.5,
            use_sde: bool = False,
            sde_sample_freq: int = -1,
            target_kl: Optional[float] = None,
            stats_window_size: int = 100,
            tensorboard_log: Optional[str] = None,
            policy_kwargs: Optional[Dict[str, Any]] = None,
            verbose: int = 0,
            seed: Optional[int] = None,
            device: Union[torch.device, str] = "auto",
            _init_setup_model: bool = True,
        ):
            super(ModifiedPPO, self).__init__()
            
            if normalize_advantage:
                assert batch_size > 1, "`batch_size` must be greater than 1. See https://github.com/DLR-RM/stable-baselines3/issues/440"      
            
            self.gamma = gamma
            self.gae_lambda = gae_lambda
            self.ent_coef = ent_coef
            self.vf_coef = vf_coef
            self.max_grad_norm = max_grad_norm
            self.batch_size = batch_size
            self.n_epochs = n_epochs
            self.clip_range = clip_range
            self.clip_range_vf = clip_range_vf
            self.normalize_advantage = normalize_advantage
            self.target_kl = target_kl
            self._stats_window_size = stats_window_size
            self.tensorboard_log = tensorboard_log
            self.seed = seed
            self.policy_kwargs = {} if policy_kwargs is None else policy_kwargs
