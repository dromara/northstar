import gymnasium as gym

from stable_baselines3 import PPO
from stable_baselines3.common.env_util import make_vec_env
from stable_baselines3.common.buffers import RolloutBuffer

# Parallel environments
vec_env = make_vec_env("CartPole-v1")

model = PPO("MlpPolicy", None, verbose=1)
rollout_buffer = RolloutBuffer(buffer_size=1000, observation_space=vec_env.observation_space, action_space=vec_env.action_space)


model.collect_rollouts( vec_env, 
                        callback=None,
                        rollout_buffer=rollout_buffer,
                        n_rollout_steps=1000)
model.train(total_timesteps=25000)
model.save("ppo_cartpole")

del model # remove to demonstrate saving and loading

model = PPO.load("ppo_cartpole")

obs = vec_env.reset()
while True:
    action, _states = model.predict(obs)
    obs, rewards, dones, info = vec_env.step(action)
    # vec_env.render("human")