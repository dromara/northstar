#!/bin/bash

set -e

COLLECTION=$1
SYMBOL=$2
START_DATE=$3

[[ -z $4 ]]&&END_DATE=20990101||END_DATE=$4

QUERY="{unifiedSymbol:\"$SYMBOL\",tradingDay:{"'$gte'":\"$START_DATE\","'$lte'":\"$END_DATE\"}}"

mongoexport -d NS_DB -c $COLLECTION -q $QUERY -o mongo-export-data.dat 

tar --remove-file -czvf mongo-export-data.tar.gz mongo-export-data.dat