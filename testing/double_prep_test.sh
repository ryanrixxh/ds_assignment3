#!/usr/bin/env bash
trap "exit" INT TERM ERR
trap "kill 0" EXIT
echo Starting test ...
cd ..
parallel -u -k ::: 'java Member 2' 'java Member 3' 'java Member 4' 'java Member 5' 'java Member 6' 'java Member 8' 'java Member 9' &
sleep 2
java Member 1 prepare > testing/outputs/double_prep_output_M1.txt &
sleep 2
java Member 7 prepare > testing/outputs/double_prep_output_M7.txt
wait
