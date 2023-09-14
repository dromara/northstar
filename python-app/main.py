import torch
from server import Server
from flask import Flask, request, jsonify

app = Flask(__name__)
server = Server()

@app.route("/init-info", methods=["POST"])
def init_info():
    return server.init_info(request.json)
    
@app.route("/get-action", methods=["POST"])
def get_action():
    data = request.json
    return server.get_action(data)
    
app.run(port=5001)
# set device to cpu or cuda
device = torch.device('cpu')
if(torch.cuda.is_available()): 
    device = torch.device('cuda:0') 
    torch.cuda.empty_cache()
    print(f"Device set to : {str(torch.cuda.get_device_name(device))}")
else:
    print("Device set to : cpu")