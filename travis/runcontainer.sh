#!/bin/bash

SUCCESS_TOKEN=SUCCESS
RESULT=${SUCCESS_TOKEN}
while [ $# -ge 1 ]
do
  if [ "${1}" = "oracle" ]; then
    CONTAINER_NAMES="${CONTAINER_NAMES} oraclefreeslick"
  elif [ "${1}" = "db2" ]; then
    CONTAINER_NAMES="${CONTAINER_NAMES} db2freeslick"
  else
    echo "Unknown container type ${1}"
    exit 1
  fi
  shift
done

for try in 1 2 3 4 5
do
  for CONTAINER_NAME in ${CONTAINER_NAMES}
  do
    echo "Trying to start container ${CONTAINER_NAME} #$try"
    RUNNING=$(docker inspect  --format="{{ .State.Running}}" ${CONTAINER_NAME}  2> /dev/null)
    if [ $? -eq 1 ]; then
      if [ "${CONTAINER_NAME}" = "oraclefreeslick" ]; then
	RESULT=$(docker run -d -p 49160:22 -p 49161:1521 --name ${CONTAINER_NAME} wnameless/oracle-xe-11g && echo -e "\n${SUCCESS_TOKEN}")
      elif [ "${CONTAINER_NAME}" = "db2freeslick" ]; then
	RESULT=$(docker run -d -p 50000:50000 --name ${CONTAINER_NAME} -e DB2INST1_PASSWORD=db2inst1-pwd -e LICENSE=accept  ibmcom/db2express-c:latest "db2start" &&
	docker cp db2freeslick:/home/db2inst1/sqllib/java/db2jcc4.jar . &&
	mvn install:install-file -DgroupId=com.ibm -DartifactId=db2jcc4 -Dversion=4.19.26 -Dpackaging=jar -Dfile=./db2jcc4.jar &&
	docker exec -i -u db2inst1 -t db2freeslick bash -l -c "db2 create database DSNFREE" &&
	echo -e "\n${SUCCESS_TOKEN}")
# This bit is properly weird and took me ages to find a workaround. I will raise a bug with db2 (or the db2 docker image)
# basically, without a connection to the db in a shell that is kept alive, aggregate functions with nulls behave incorrectly
# so the AggregateTest.testGroupBy test fails
	LAST_LINE=$(echo "${RESULT}" | tail -n 1)
	if [ "${LAST_LINE}" = "${SUCCESS_TOKEN}" ]; then
	  docker exec -i db2freeslick bash -c "su - db2inst1 -c 'db2 connect to DSNFREE && while sleep 65535;do :; done'" &
	fi
      fi
    elif [ "$RUNNING" = "false" ]; then
      echo "Container ${CONTAINER_NAME} exists, but stopped. Starting ..."
      RESULT=$(docker start ${CONTAINER_NAME} && echo -e "\n${SUCCESS_TOKEN}")
    elif [ "$RUNNING" = "true" ]; then
      echo "Container ${CONTAINER_NAME} already running"
      RESULT=${SUCCESS_TOKEN}
    fi
    LAST_LINE=$(echo "${RESULT}" | tail -n 1)
    if [ "${LAST_LINE}" != "${SUCCESS_TOKEN}" ]; then
      echo "Container startup failed. Retry ..."
      break
    fi
  done
  if [ "${LAST_LINE}" = "${SUCCESS_TOKEN}" ]; then
    echo "Container startup succeeded"
    exit 0
  fi

  echo "Removing container: ${CONTAINER_NAME}"
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
