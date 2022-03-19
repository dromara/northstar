#!/bin/bash
cd server
nohup npm start 2>&1 >monitor.log &