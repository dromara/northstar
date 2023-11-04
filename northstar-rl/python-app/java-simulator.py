import random
from time import sleep
import requests

SLEEP_TIME = 0.1

def init_info():
    # send request to server
    url = "http://localhost:5001/init-info"
    data = {
        "indicator_symbol": "rb0000@SHFE@FUTURES",
        "agent_name": "ppo",
        "is_train": False,
        "model_version": 1
    }
    post_response = requests.post(url, json=data)
    print(post_response.json())    
    # assert 0
    
def get_action():
    url = "http://localhost:5001/get-action"
    data = {
        "unified_symbol": "rb0000@SHFE@FUTURES",
        "open_price": random.random(),
        "high_price": random.random(),
        "low_price": random.random(),
        "close_price": random.random(),
        "last_reward": random.random(),
    }
    post_response = requests.post(url, json=data)
    print(post_response.json())

def simulate():
    init_info()
    sleep(SLEEP_TIME)
    
    for i in range(1000):
        get_action()
        sleep(SLEEP_TIME)
        
simulate()