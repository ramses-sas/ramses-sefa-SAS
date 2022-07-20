#!/bin/bash
############################################################
# Help                                                     #
############################################################
Help()
{
   # Display Help
   echo "Syntax: dockerRun [-h|i]"
   echo "Options:"
   echo "h     Display help."
   echo "i     Run in interactive mode (i.e., attach STDIN, STDOUT, STDERR)."
   echo
}


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
         echo "UNKNOWN OPTION $option"
         exit;;
   esac
done

docker start $DEFOPT $SERVICE_NAME
echo "Container $SERVICE_NAME running."
