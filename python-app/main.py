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