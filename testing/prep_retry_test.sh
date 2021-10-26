#!/usr/bin/env bash
#To change the amount of servers initiated by parallel add or delete arguments from line 8
#tests whether a member will retry with a higher id number and become sucessful on the second run
trap "exit" INT TERM ERR
trap "kill 0" EXIT
echo Starting test ...
cd ..
parallel -u -k ::: 'java Member 2 late' 'java Member 3 medium' 'java Member 4 medium' 'java Member 5 medium' 'java Member 6 medium' 'java Member 8 medium' 'java Member 9 medium' &
sleep 2
java Member 7 medium prepare > testing/outputs/prep_retry_output_M7.txt &
sleep 2
java Member 1 immediate prepare > testing/outputs/prep_retry_output_M1.txt
wait
