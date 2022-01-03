#!/bin/bash

set -e

SYMBOL=$1
START_DATE=$2

[[ -z $3 ]]&&END_DATE=20990101||END_DATE=$3

QUERY="{unifiedSymbol:\"$SYMBOL\",tradingDay:{"'$gte'":\"$START_DATE\","'$lte'":\"$END_DATE\"}}"

mongoexport -d NS_DB -c DATA_CTP行情 -q $QUERY -o ctp-$SYMBOL-data.dat 

tar --remove-file -czvf ctp-$SYMBOL-data.tar.gz ctp-$SYMBOL-data.dat