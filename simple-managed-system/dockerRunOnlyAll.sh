#!/bin/bash
RED='\033[0;31m'
GREEN='\033[0;32m'
NC='\033[0m' # No Color

PrintError() {
  echo -e "${RED}$1${NC}"
}

PrintSuccess() {
  echo -e "${GREEN}$1${NC}"
}

docker ps >/dev/null 2>&1 || PrintError "Cannot connect to Docker daemon"

ExtractEnv()
{
  # Get the service name from the application.properties file
  SERVICE_IMPLEMENTATION_NAME=`awk -v FS="IMPLEMENTATION_NAME=" 'NF>1{print $2}' ./src/main/resources/application.properties`
  if [ "$SERVICE_IMPLEMENTATION_NAME" = "" ]; then
    PrintError "UNKNOWN SERVICE IMPLEMENTATION NAME. Make sure that IMPLEMENTATION_NAME is set in application.properties. Using spring.application.name property"
    SERVICE_IMPLEMENTATION_NAME=`awk -v FS="spring.application.name=" 'NF>1{print $2}' ./src/main/resources/application.properties`
    if [ "$SERVICE_IMPLEMENTATION_NAME" = "" ]; then
      PrintError "UNKNOWN SERVICE IMPLEMENTATION NAME. Make sure that IMPLEMENTATION_NAME is set in application.properties"
      exit 1
    fi
  fi

  # Get the port of the service from the application.properties file
  SERVICE_PORT=`awk -v FS="SERVER_PORT=" 'NF>1{print $2}' ./src/main/resources/application.properties`
  if [ "$SERVICE_PORT" = "" ]; then
    PrintError "UNKNOWN SERVER PORT. Make sure that SERVER_PORT is set in application.properties"
    exit 1
  fi

  ROOT_PATH="$( cd "$( dirname "${BASH_SOURCE[0]}" )/.." && pwd && cd .. )"
  #PORT_MAPPING="${SERVICE_PORT}"
  PORT_MAPPING="${SERVICE_PORT}:${SERVICE_PORT}"
  TARGET="linux/arm64/v8"

  # Use private IP address of the machine by default
  HOST=`ifconfig | grep '\<inet\>' | cut -d ' ' -f2 | grep -v '127.0.0.1'`
  EUREKA_IP_PORT="${HOST}:58082"

}

SCRIPTS_PATH="$( cd "scripts" && pwd && cd .. )"


cd "servers/eureka-registry-server/" || return
ExtractEnv
docker create -p $PORT_MAPPING -e HOST=$HOST -e SERVER_PORT=$SERVICE_PORT -e EUREKA_IP_PORT=$EUREKA_IP_PORT -i -t --name "${SERVICE_IMPLEMENTATION_NAME}_${SERVICE_PORT}" $SERVICE_IMPLEMENTATION_NAME
bash "$SCRIPTS_PATH/dockerRun.sh"
cd ../..
sleep 2
cd "servers/config-server/" || return
ExtractEnv
docker create -p $PORT_MAPPING -e HOST=$HOST -e SERVER_PORT=$SERVICE_PORT -e EUREKA_IP_PORT=$EUREKA_IP_PORT -i -t --name "${SERVICE_IMPLEMENTATION_NAME}_${SERVICE_PORT}" $SERVICE_IMPLEMENTATION_NAME
bash "$SCRIPTS_PATH/dockerRun.sh"
cd ../..
echo; echo
echo "Waiting for the config server to be ready..."
sleep 10



PrintSuccess "Starting all the containers..."
echo


for d in */; do
  if [ "${d: -8}" = "service/" ] ; then
    echo; echo; echo
    PrintSuccess "Starting $d"
    cd "$d" || return
    ExtractEnv
    docker create -p $PORT_MAPPING -e HOST=$HOST -e SERVER_PORT=$SERVICE_PORT -e EUREKA_IP_PORT=$EUREKA_IP_PORT -i -t --name "${SERVICE_IMPLEMENTATION_NAME}_${SERVICE_PORT}" $SERVICE_IMPLEMENTATION_NAME
    bash "$SCRIPTS_PATH/dockerRun.sh"
    cd ..
    echo; echo; echo
  fi
done

echo
PrintSuccess "SETUP DONE. All containers should be up and running."
