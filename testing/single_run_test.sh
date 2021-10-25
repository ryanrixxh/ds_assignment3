#!/usr/bin/env bash
echo Starting test ...
cd ..
parallel -u -k ::: 'java Member 2' 'java Member 3' &
sleep 2
java Member 1 prepare > testing/outputs/single_run_output.txt
wait
