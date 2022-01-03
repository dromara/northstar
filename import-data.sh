#!/bin/bash

set -e

mongoimport --db NS_DB --collection DATA_CTP行情 --file $1