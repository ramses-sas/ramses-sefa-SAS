#!/bin/bash
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
   echo "a     Specify an IP address for the microservice instance. "
   echo "      Defaults to the public IP address of the host, obtained through an external service. "
   echo "      The host must be reachable from that address."
   echo "e     Specify the IP address and the port of the Eureka Service, in the form IP:PORT. "
   echo "      Defaults to the address specified in the application.properties."
}

# Get the service name from the application.properties file
SERVICE_NAME=`awk -v FS="spring.application.name=" 'NF>1{print $2}' ./src/main/resources/application.properties`
if [ "$SERVICE_NAME" = "" ]; then
    echo "UNKNOWN SERVICE NAME. Make sure that spring.application.name is set in application.properties"
    exit 1
fi

# Get the port of the service from the application.properties file
SERVICE_PORT=`awk -v FS="server.port=" 'NF>1{print $2}' ./src/main/resources/application.properties`
if [ "$SERVICE_PORT" = "" ]; then
    echo "UNKNOWN SERVER PORT. Make sure that server.port is set in application.properties"
    exit 1
fi

EUREKA_IP_PORT=""
IP=""
if [ "$SERVICE_NAME" != "eureka-registry-service" ]; then
  # If it is not the Eureka service, get the IP address and port of the Eureka service from the application.properties file
  EUREKA_IP_PORT=`awk -v FS="EUREKA_IP_PORT=" 'NF>1{print $2}' ./src/main/resources/application.properties`
  if [ "$EUREKA_IP_PORT" = "" ]; then
      echo "UNKNOWN EUREKA IP AND PORT. Make sure that EUREKA_IP_PORT is set in application.properties"
      exit 1
  fi
else
  # If it is the Eureka service, fix the IP address to the default value
  IP="--ip 172.0.0.10"
fi

PORT_MAPPING="${SERVICE_PORT}:${SERVICE_PORT}"
HOST="$(curl https://ipinfo.io/ip)"
TARGET="linux/arm64/v8"

while getopts "hp:t:a:e:" option; do
   case $option in
      h) # display Help
        Help
        exit;;
      p) # Port
        PORT_MAPPING="${OPTARG}:${SERVICE_PORT}";;
      t) # Target
        TARGET="${OPTARG}";;
      a) # Set IP address of the service instance
        HOST="${OPTARG}";;
      e) # Set IP address of the Eureka service
        EUREKA_IP_PORT="${OPTARG}";;
     \?) # Wrong option
         echo "UNKNOWN OPTION $option"
         exit;;
   esac
done

echo ""
echo "1/5 - Building the service using gradle provided by the project..."
../gradlew build
echo "1/5 - DONE"
echo ""

echo "---------------------------------------------"

echo ""
echo "2/5 - Creating a network for the container..."
docker network create --subnet=172.0.0.0/16 saefaNetwork
echo "2/5 - DONE"
echo ""

echo "---------------------------------------------"

echo ""
echo "3/5 Build the container image with name $SERVICE_NAME for the platform $TARGET"
docker build --platform=$TARGET -t $SERVICE_NAME .
echo "3/5 - DONE"
echo ""

echo "---------------------------------------------"

echo ""
echo "4/5 - Removing the container if it exists..."
docker stop $SERVICE_NAME >& /dev/null
docker rm $SERVICE_NAME >& /dev/null
echo "4/5 - DONE"
echo ""

echo "---------------------------------------------"

echo ""
echo "5/5 - Creating a container named $SERVICE_NAME from the image created before, on the network created before, forwarding port $PORT_MAPPING"
docker create --network saefaNetwork $IP -p $PORT_MAPPING -e HOST=$HOST -e EUREKA_IP_PORT=$EUREKA_IP_PORT -i -t --name $SERVICE_NAME $SERVICE_NAME
echo "5/5 - DONE"
echo ""

echo "Container $SERVICE_NAME exposing port ${PORT_MAPPING%:*} created successfully."