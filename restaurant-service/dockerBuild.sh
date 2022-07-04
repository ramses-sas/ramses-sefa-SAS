#!/bin/bash
############################################################
# Help                                                     #
############################################################
Help()
{
   # Display Help
   echo "Syntax: dockerBuild [-p|r|t|h]"
   echo "Options:"
   echo "p     Select port to run on. Defaults to 8080."
   echo "r     Use a random port to run on."
   echo "t     Specify target platform for the container. Defaults to linux/arm64/v8."
   echo "h     Display help."
   echo
}

SERVICE_NAME=`awk -v FS="spring.application.name=" 'NF>1{print $2}' ./src/main/resources/application.properties`
if [ "$SERVICE_NAME" = "" ]; then
    SERVICE_NAME="unknown-service"
    echo "UNKNOWN SERVICE_NAME"
fi

SERVICE_PORT=`awk -v FS="server.port=" 'NF>1{print $2}' ./src/main/resources/application.properties`
if [ "$SERVICE_PORT" = "" ]; then
    SERVICE_PORT="8081"
fi

PORT="8080"
PORT_OPTION="${PORT}:${SERVICE_PORT}"
TARGET="linux/arm64/v8"
while getopts "p:rht:" option; do
   case $option in
      h) # display Help
        Help
        exit;;
      p) # Port
        PORT_OPTION="${OPTARG}:${SERVICE_PORT}";;
      r) # Random port
        PORT_OPTION="${SERVICE_PORT}"
        PORT="";;
      t) # Target
        TARGET="${OPTARG}";;
     \?) # Wrong option
         echo "UNKNOWN OPTION $option"
         exit;;
   esac
done

../gradlew build
docker build --platform=$TARGET -t $SERVICE_NAME .
docker stop $SERVICE_NAME
docker rm $SERVICE_NAME
docker create -p $PORT_OPTION -i -t --name $SERVICE_NAME $SERVICE_NAME
if [ "$PORT" = "" ]; then
    echo "Exposing service $SERVICE_NAME on a random port"
else
    echo "Exposing service $SERVICE_NAME on port $PORT"
fi