import gym
from flask import Flask, request, jsonify

app = Flask(__name__)
env = gym.make('CartPole-v0')

@app.route("/interact", methods=["POST"])
def interact():
    data = request.json
    print(data)
    action = data.get('value')
    print(action)
    next_state, reward, done,_info, _ = env.step(action)
    
    return {'state':{'values': next_state.tolist()}, 'reward':{'value': reward}, 'hasDone': done == 1}

@app.route("/reset", methods=["GET"])
def reset():
    return {'values': env.reset()[0].tolist()}
    
    
app.run(port=5001)