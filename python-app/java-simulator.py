from time import sleep
import requests

def init_info():
    # send request to server
    url = "http://localhost:5001/init-info"
    data = {
        "indicator_symbol": "rb0000@SHFE@FUTURES",
        "agent_name": "ppo",
        "is_train": True,
        "model_version": 1
    }
    post_response = requests.post(url, json=data)
    print(post_response.json())    
    # assert 0
    
def get_action():
    url = "http://localhost:5001/get-action"
    data = {
        "unified_symbol": "rb0000@SHFE@FUTURES",
        "open_price": 100,
        "high_price": 100,
        "low_price": 100,
        "close_price": 100,
        "last_reward": 0
    }
    post_response = requests.post(url, json=data)
    print(post_response.json())

def simulate():
    init_info()
    sleep(1)
    
    for i in range(1000):
        get_action()
        sleep(1)
        
simulate()