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
   echo "- uses the private IP address of the machine. It means that only the microservices hosted on the same LAN can access it"
   echo "- makes the container expose the same port used by the microservice"
   echo "- creates a linux/arm64/v8 container."
   echo
   echo "Options:"
   echo "h     Display help."
   echo "p     Specify port to be exposed by the container. Defaults to the port used by the service."
   echo "t     Specify target platform for the container. Defaults to linux/arm64/v8."
   echo "a     Specify the IP address of the machine hosting the container instance. "
   echo "      The other microservices must be able to reach it at that IP address using the service port."
   echo "r     Use the public IP address of the machine, obtained through an external service. "
   echo "      It usually requires a port forwarding for the microservice ports."
   #echo "i     Specify the IP address to assign to the container hosting the microservice. "
   #echo "      If not specified, a free IP address will be used. "
   #echo "      The IP must belong to the 172.0.0.0/16 subnet."
   echo "e     Specify the IP address and the port of the Eureka Service, in the form IP:PORT. "
   echo "      Defaults to the address specified in the application.properties if the -r option is used."
   echo "      Otherwise, defaults to the private IP address of the hosting machine."
   echo "s     Specify the IP address of the MySQL server. "
   echo "      Defaults to the address specified in the application.properties if the -r option is used."
   echo "      Otherwise, defaults to the private IP address of the hosting machine."
   echo
   echo "Use only one between -a and -r. When using -a or -r, the -e and -s options are mandatory."
}

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

# Get the Eureka IP and port from the application.properties file
EUREKA_IP_PORT=`awk -v FS="EUREKA_IP_PORT=" 'NF>1{print $2}' ./src/main/resources/application.properties`
if [ "$EUREKA_IP_PORT" = "" ]; then
  PrintError "UNKNOWN EUREKA IP AND PORT. Make sure that EUREKA_IP_PORT is set in application.properties"
  exit 1
fi

ROOT_PATH="$( cd "$( dirname "${BASH_SOURCE[0]}" )/.." && pwd && cd .. )"
PORT_MAPPING="${SERVICE_PORT}:${SERVICE_PORT}"
TARGET="linux/arm64/v8"

# Use private IP address of the machine by default
HOST=`ifconfig | grep '\<inet\>' | cut -d ' ' -f2 | grep -v '127.0.0.1'`
EUREKA_IP_PORT="${HOST}:${EUREKA_IP_PORT##*:}"
MYSQL_SERVER="${HOST}"

while getopts "hrp:t:a:e:s:" option; do
   case $option in
      h) # display Help
        Help
        exit;;
      p) # Port
        PORT_MAPPING="${OPTARG}:${SERVICE_PORT}";;
      t) # Target
        TARGET="${OPTARG}";;
      a) # Specify IP address of the machine hosting the container
        HOST="${OPTARG}";;
      r) # Use public IP address of the machine
        HOST="$(curl https://ipinfo.io/ip)";;
      #i) # Set IP address of the container
      #  IP="--ip ${OPTARG}";;
      e) # Set IP address and port of the Eureka service
        EUREKA_IP_PORT="${OPTARG}";;
      s) # Set IP address and port of the MySQL server hosting the databases
        MYSQL_SERVER="${OPTARG}";;
     \?) # Wrong option
        PrintError "UNKNOWN OPTION $option"
        exit 1;;
   esac
done

echo ""
PrintSuccess "1/4 - Building the service using gradle provided by the project..."
"$ROOT_PATH/gradlew" build
PrintSuccess "1/4 - DONE"
echo ""

echo "---------------------------------------------"

echo ""
PrintSuccess "2/4 Building the container image with name $SERVICE_NAME for the platform $TARGET"
docker build --platform=$TARGET -t $SERVICE_NAME .
PrintSuccess "2/4 - DONE"
echo ""

echo "---------------------------------------------"

echo ""
PrintSuccess "3/4 - Removing the container if it exists..."
docker stop $SERVICE_NAME >& /dev/null
docker rm $SERVICE_NAME >& /dev/null
PrintSuccess "3/4 - DONE"
echo ""

echo "---------------------------------------------"

echo ""
PrintSuccess "4/4 - Creating container $SERVICE_NAME"
docker create -p $PORT_MAPPING -e HOST=$HOST -e EUREKA_IP_PORT=$EUREKA_IP_PORT -e MYSQL_SERVER=$MYSQL_SERVER -i -t --name $SERVICE_NAME $SERVICE_NAME
PrintSuccess "4/4 - DONE"
echo ""

echo
PrintSuccess "Container $SERVICE_NAME exposing port ${PORT_MAPPING%:*} created successfully."
echo
