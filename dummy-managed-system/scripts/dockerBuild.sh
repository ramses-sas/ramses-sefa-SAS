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

Help()
{
   # Display Help
   echo "Syntax: dockerBuild [-OPTION]"
   echo
   echo "By default, this script:"
   echo "- runs the microservice in the local mode."
   echo "     Only the microservices hosted on the same LAN can access it"
   echo "     All the server microservices, (i.e., the Eureka Server, the MySQL Server and the API Gateway) must be running locally too."
   echo "- makes the container expose the same port used by the microservice"
   echo "- creates a linux/arm64/v8 container."
   echo
   echo "Options:"
   echo "h     Display help."
   echo "p     Specify port to be exposed by the container. Defaults to the port used by the service."
   echo "t     Specify target platform for the container. Defaults to linux/arm64/v8."
   echo "r     Run the microservice in the remote mode."
   echo "         It uses the public IP address of the machine, obtained through an external service. "
   echo "         It usually requires a port forwarding for the microservice ports."
   echo "         The addresses of the Eureka Server, of the MySQL Server and of the API Gateway are retrieved from the config repository"
}

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
MYSQL_IP_PORT="${HOST}:3306"
EUREKA_IP_PORT="${HOST}:58082"
API_GATEWAY_IP_PORT="${HOST}:58081"
IS_REMOTE="no"

while getopts "hrp:t:" option; do
   case $option in
      h) # display Help
        Help
        exit;;
      p) # Port
        SERVICE_PORT=$OPTARG
        PORT_MAPPING="${SERVICE_PORT}:${SERVICE_PORT}";;
      t) # Target
        TARGET="${OPTARG}";;
      r) # Use public IP address of the machine
        HOST="$(curl https://ipinfo.io/ip)"
        IS_REMOTE="yes";;
     \?) # Wrong option
        PrintError "UNKNOWN OPTION $option"
        exit 1;;
   esac
done

echo ""
PrintSuccess "1/4 - Building the service using gradle provided by the project..."
"$ROOT_PATH/gradlew" clean
"$ROOT_PATH/gradlew" build
PrintSuccess "1/4 - DONE"
echo ""

echo "---------------------------------------------"

echo ""
PrintSuccess "2/4 Building the container image with name $SERVICE_IMPLEMENTATION_NAME for the platform $TARGET"
docker build --platform=$TARGET -t $SERVICE_IMPLEMENTATION_NAME .
PrintSuccess "2/4 - DONE"
echo ""

echo "---------------------------------------------"

echo ""
PrintSuccess "3/4 - Removing the container if it exists..."
docker stop $SERVICE_IMPLEMENTATION_NAME >& /dev/null
docker rm $SERVICE_IMPLEMENTATION_NAME >& /dev/null
PrintSuccess "3/4 - DONE"
echo ""

echo "---------------------------------------------"

echo ""
if [ "$IS_REMOTE" = "no" ]; then
  PrintSuccess "4/4 - Creating container $SERVICE_IMPLEMENTATION_NAME in local mode"
  docker create -p $PORT_MAPPING -e HOST=$HOST -e SERVER_PORT=$SERVICE_PORT -e EUREKA_IP_PORT=$EUREKA_IP_PORT -e API_GATEWAY_IP_PORT=$API_GATEWAY_IP_PORT -e MYSQL_IP_PORT=$MYSQL_IP_PORT -i -t --name "${SERVICE_IMPLEMENTATION_NAME}_${SERVICE_PORT}" $SERVICE_IMPLEMENTATION_NAME
else
  PrintSuccess "4/4 - Creating container $SERVICE_IMPLEMENTATION_NAME in remote mode"
  docker create -p $PORT_MAPPING -e HOST=$HOST -e SERVER_PORT=$SERVICE_PORT -i -t --name "${SERVICE_IMPLEMENTATION_NAME}_${SERVICE_PORT}" $SERVICE_IMPLEMENTATION_NAME
fi
PrintSuccess "4/4 - DONE"
echo ""

echo
PrintSuccess "Container $SERVICE_IMPLEMENTATION_NAME exposing port ${SERVICE_PORT} created successfully."
echo
