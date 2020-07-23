#!/usr/bin/env bash


# https://stackoverflow.com/questions/55073453/wait-for-kubernetes-job-to-complete-on-either-failure-success-using-command-line


# wait for completion as background process - capture PID
kubectl wait --timeout=2h --for=condition=complete -n $1 $2 &
completion_pid=$!

# wait for failure as background process - capture PID
kubectl wait --timeout=2h --for=condition=failed -n $1 $2 && exit 1 &
failure_pid=$! 

# capture exit code of the first subprocess to exit
wait -n $completion_pid $failure_pid

# store exit code in variable
exit_code=$?

if (( $exit_code == 0 )); then
  echo "Job completed"
else
  echo "Job failed with exit code ${exit_code}, exiting..."
fi

kubectl logs -n $1 $2

exit $exit_code
