#!/bin/bash
############################################################
# Help                                                     #
############################################################
Help()
{
   # Display Help
   echo "Syntax: dockerRun [-i|h]"
   echo "Options:"
   echo "i     Run in interactive mode."
   echo "h     Display help."
   echo
}

############################################################
# Main program                                             #
############################################################
SERVICE_NAME=`awk -v FS="spring.application.name=" 'NF>1{print $2}' ./src/main/resources/application.properties`
if [ "$SERVICE_NAME" = "" ]; then
    echo "UNKNOWN SERVICE NAME. Make sure that spring.application.name is set in application.properties"
    exit 1
fi

DEFOPT=""

while getopts ":ih:" option; do
   case $option in
      h) # display Help
         Help
         exit;;
      i) # Interactive
         DEFOPT="-a -i";;
     \?) # Run in BG
         echo "Container will run in background";;
   esac
done

docker start $DEFOPT $SERVICE_NAME