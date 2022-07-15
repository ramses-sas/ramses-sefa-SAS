#!/bin/bash
############################################################
# Help                                                     #
############################################################
Help()
{
   # Display Help
   echo "Syntax: dockerBuild [-p|r|t|h|a]"
   echo "Options:"
   echo "h     Display help."
   echo "p     Select port to run on. Defaults to the port used by the service."
   echo "r     Use a random port to run on."
   echo "t     Specify target platform for the container. Defaults to linux/arm64/v8."
   echo "a     Change the default IP address set to 172.0.0.10. Notice that you can only change the last two digits (last 16 bits)"
   echo "e     Display help."
}

export HOST="$(curl https://ipinfo.io/ip)"

IP=""
SERVICE_NAME=`awk -v FS="spring.application.name=" 'NF>1{print $2}' ./src/main/resources/application.properties`
if [ "$SERVICE_NAME" = "" ]; then
    echo "UNKNOWN SERVICE NAME. Make sure that spring.application.name is set in application.properties"
    exit 1
fi

SERVICE_PORT=`awk -v FS="server.port=" 'NF>1{print $2}' ./src/main/resources/application.properties`
if [ "$SERVICE_PORT" = "" ]; then
    echo "UNKNOWN SERVER PORT. Make sure that server.port is set in application.properties"
    exit 1
fi

if [ "$SERVICE_NAME" = "eureka-registry-service" ]; then
  IP="--ip 172.0.0.10"
fi

PORT=${SERVICE_PORT}
PORT_OPTION="${PORT}:${SERVICE_PORT}"
TARGET="linux/arm64/v8"
while getopts "p:rht:a:" option; do
   case $option in
      h) # display Help
        Help
        exit;;
      p) # Port
        PORT="$OPTARG"
        PORT_OPTION="${OPTARG}:${SERVICE_PORT}";;
      r) # Random port
        PORT_OPTION="${SERVICE_PORT}"
        PORT="";;
      t) # Target
        TARGET="${OPTARG}";;
      a)#Change ip
        IP="--ip ${OPTARG}";;
     \?) # Wrong option
         echo "UNKNOWN OPTION $option"
         exit;;
   esac
done

../gradlew build
docker network create --subnet=172.0.0.0/16 saefaNetwork
docker build --platform=$TARGET -t $SERVICE_NAME .
docker stop $SERVICE_NAME >& /dev/null
docker rm $SERVICE_NAME >& /dev/null
docker create --network saefaNetwork $IP -p $PORT_OPTION -e HOST=$HOST -i -t --name $SERVICE_NAME $SERVICE_NAME
if [ "$PORT" = "" ]; then
    echo "Exposing service $SERVICE_NAME on a random port"
else
    echo "Exposing service $SERVICE_NAME on port $PORT"
fi