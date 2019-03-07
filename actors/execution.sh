#!/bin/bash
num_actors=10
# save pids related to all actors initiated
actors_pid=()
new_actor_id=1
new_port=10001
echo "Creating tracker tracker_0 with port number 10000"
nohup mvn exec:java@network -Dconfig.resource=tracker0.conf > /dev/null &
# save last process pid
actors_pid+=($!)
echo "Actor pid = $!"

# run maven exec for each acotr
# put a delay between actor creation, since
# it seems to create issues otherwise
for i in $(seq 1 $num_actors)
do
  echo "Creating peer actor_$new_actor_id with port number $new_port"
  nohup mvn exec:java@network -Dpeer=actor_$new_actor_id -Dport=$new_port > /dev/null &
  actors_pid+=($!)
  echo "Actor pid = $!"
  ((new_actor_id++))
  ((new_port++))
  sleep 0.5s
done

echo "Actor pids ${actors_pid[@]}"


sleep 1m
echo "Killing actors"
for i in ${actors_pid[@]}
do
  kill $i
done

