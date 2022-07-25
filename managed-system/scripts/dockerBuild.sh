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

############################################################
# Help                                                     #
############################################################
Help()
{
   # Display Help
   echo "Syntax: dockerBuild [-p|t|h|a|e]"
   echo "Options:"
   echo "h     Display help."
   echo "p     Specify port to be exposed by the container. Defaults to the port used by the service."
   echo "t     Specify target platform for the container. Defaults to linux/arm64/v8."
   echo "a     Specify the public IP address of the machine hosting the container instance. "
   echo "      Defaults to the public IP address of the host, obtained through an external service. "
   echo "      The host must be reachable from that address at the service port."
   echo "i     Specify the IP address to assign to the container hosting the microservice. "
   echo "      If not specified, a free IP address will be used. "
   echo "      The IP must belong to the 172.0.0.0/16 subnet."
   echo "e     Specify the IP address and the port of the Eureka Service, in the form IP:PORT. "
   echo "      Defaults to the address specified in the application.properties."
}

ROOT_PATH="$( cd "$( dirname "${BASH_SOURCE[0]}" )/.." && pwd && cd .. )"

# Get the service name from the application.properties file
SERVICE_NAME=`awk -v FS="spring.application.name=" 'NF>1{print $2}' ./src/main/resources/application.properties`
if [ "$SERVICE_NAME" = "" ]; then
  PrintError "UNKNOWN SERVICE NAME. Make sure that spring.application.name is set in application.properties"
  exit 1
fi

# Get the port of the service from the application.properties file
SERVICE_PORT=`awk -v FS="server.port=" 'NF>1{print $2}' ./src/main/resources/application.properties`
if [ "$SERVICE_PORT" = "" ]; then
  PrintError "UNKNOWN SERVER PORT. Make sure that server.port is set in application.properties"
  exit 1
fi

# Get the Eureka server IP and port from the application.properties file
EUREKA_IP_PORT=`awk -v FS="EUREKA_IP_PORT=" 'NF>1{print $2}' ./src/main/resources/application.properties`
if [ "$EUREKA_IP_PORT" = "" ]; then
  PrintError "UNKNOWN EUREKA IP AND PORT. Make sure that EUREKA_IP_PORT is set in application.properties"
  exit 1
fi


PORT_MAPPING="${SERVICE_PORT}:${SERVICE_PORT}"
TARGET="linux/arm64/v8"
HOST="$(curl https://ipinfo.io/ip)"
IP=""

while getopts "hi:p:t:a:e:" option; do
   case $option in
      h) # display Help
        Help
        exit;;
      p) # Port
        PORT_MAPPING="${OPTARG}:${SERVICE_PORT}";;
      t) # Target
        TARGET="${OPTARG}";;
      a) # Specify public IP address of the machine hosting the container
        HOST="${OPTARG}";;
      i) # Set IP address of the container
        IP="--ip ${OPTARG}";;
      e) # Set IP address and port of the Eureka service
        EUREKA_IP_PORT="${OPTARG}";;
     \?) # Wrong option
         PrintError "UNKNOWN OPTION $option"
         exit 1;;
   esac
done

echo ""
PrintSuccess "1/5 - Building the service using gradle provided by the project..."
"$ROOT_PATH/gradlew" build
PrintSuccess "1/5 - DONE"
echo ""

echo "---------------------------------------------"

echo ""
PrintSuccess "2/5 - Creating a network for the container..."
docker network create --subnet=172.0.0.0/16 saefaNetwork
PrintSuccess "2/5 - DONE"
echo ""

echo "---------------------------------------------"

echo ""
PrintSuccess "3/5 Building the container image with name $SERVICE_NAME for the platform $TARGET"
docker build --platform=$TARGET -t $SERVICE_NAME .
PrintSuccess "3/5 - DONE"
echo ""

echo "---------------------------------------------"

echo ""
PrintSuccess "4/5 - Removing the container if it exists..."
docker stop $SERVICE_NAME >& /dev/null
docker rm $SERVICE_NAME >& /dev/null
PrintSuccess "4/5 - DONE"
echo ""

echo "---------------------------------------------"

echo ""
PrintSuccess "5/5 - Creating container $SERVICE_NAME"
docker create --network saefaNetwork $IP -p $PORT_MAPPING -e HOST=$HOST -e EUREKA_IP_PORT=$EUREKA_IP_PORT -i -t --name $SERVICE_NAME $SERVICE_NAME
PrintSuccess "5/5 - DONE"
echo ""

echo
PrintSuccess "Container $SERVICE_NAME exposing port ${PORT_MAPPING%:*} created successfully."
echo
