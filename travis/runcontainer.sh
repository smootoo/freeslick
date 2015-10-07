#!/bin/bash

CONTAINER_TYPE=${1:-oracle} # oracle by default
SUCCESS_TOKEN=SUCCESS
if [ "${CONTAINER_TYPE}" = "oracle" ]; then
  CONTAINER_NAME=oraclefreeslick
elif [ "${CONTAINER_TYPE}" = "db2" ]; then
  CONTAINER_NAME=db2freeslick
else
  echo "Unknown container type ${CONTAINER_TYPE}"
  exit 1
fi

for try in 1 2 3 4 5
do
  echo "Trying to start container #$try"
  if [ "${CONTAINER_TYPE}" = "oracle" ]; then
    RESULT=$(docker run -d -p 49160:22 -p 49161:1521 --name ${CONTAINER_NAME} wnameless/oracle-xe-11g && echo -e "\n${SUCCESS_TOKEN}")
  elif [ "${CONTAINER_TYPE}" = "db2" ]; then
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
  LAST_LINE=$(echo "${RESULT}" | tail -n 1)
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
