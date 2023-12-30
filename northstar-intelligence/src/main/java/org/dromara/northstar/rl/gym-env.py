import gym
import sys
from waitress import serve
from flask import Flask, request, jsonify

envName = sys.argv[1]
app = Flask(__name__)
env = gym.make(envName)

@app.route("/interact", methods=["POST"])
def interact():
    data = request.json
    action = data.get('value')
    next_state, reward, done,_info, _ = env.step(action)
    return {'state':{'values': next_state.tolist()}, 'reward':{'value': reward}, 'hasDone': done == 1}

@app.route("/reset", methods=["GET"])
def reset():
    return {'values': env.reset()[0].tolist()}
    
    
serve(app, port=5001, threads=6)
