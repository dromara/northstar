class PPOConfig:
    def __init__(self,
                 state_dim=4,
                 action_dim=1,
                 lr_actor=3e-4,
                 lr_critic=3e-4,
                 gamma=0.99,
                 K_epochs=4,
                 eps_clip=0.2,
                 has_continuous_action_space=False) -> None:
        self.state_dim = state_dim
        self.action_dim = action_dim
        self.lr_actor = lr_actor
        self.lr_critic = lr_critic
        self.gamma = gamma
        self.K_epochs = K_epochs
        self.eps_clip = eps_clip
        self.has_continuous_action_space = has_continuous_action_space