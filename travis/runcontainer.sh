#!/bin/bash

SUCCESS_TOKEN=SUCCESS
CONTAINER_NAME=oracledocker


for try in 1 2 3 4 5
do
  echo "Trying to start container #$try"
  RESULT=$(docker run -d -p 49160:22 -p 49161:1521 --name ${CONTAINER_NAME} wnameless/oracle-xe-11g && echo -e "\n${SUCCESS_TOKEN}")
  LAST_LINE=$(echo "${RESULT}" | tail -n 1)
  if [ "${LAST_LINE}" = "${SUCCESS_TOKEN}" ]
  then
    echo "Container startup succeeded"
    exit 0
  fi

  echo "Removing container:"
  docker stop ${CONTAINER_NAME} && docker rm ${CONTAINER_NAME}

  echo "Restart Docker:"
  sudo restart docker
  echo "Resetting iptables:"
  sudo iptables -F

  echo "Docker status:"
  sudo service docker status

  echo "Sleeping.."
  sleep 5

  echo "Docker status:"
  sudo service docker status
done 
# if we got here, container startup has failed
exit 1
