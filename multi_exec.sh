#!/bin/bash

count=$1
test_file=$2
logs_folder=$HOME/EpTOlogs
sim_res_folder=$(basename -- "$test_file")
sim_res_folder="${sim_res_folder%.*}"


mkdir -p $logs_folder/$sim_res_folder
cd actors

for i in $(seq 1 $count)
do
    nohup mvn exec:java@single -Drun.config=$test_file > /dev/null
    cp $logs_folder/execution.log $logs_folder/$sim_res_folder/exec_$i.log
    echo "done simulation $i"
done
