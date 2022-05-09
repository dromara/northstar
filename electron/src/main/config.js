const path = require('path')
const fs = require("fs");


var config_path = path.join(__dirname, '../../config.json')
if(!fs.existsSync(config_path)) {
    config_path = path.join(__dirname, '../config.json')
}
var script = fs.readFileSync(config_path)
var config = JSON.parse(script)

function getConfig(key) {
    let value = config[key]
    return value ? value : ''
}

module.exports = {
    getConfig
}