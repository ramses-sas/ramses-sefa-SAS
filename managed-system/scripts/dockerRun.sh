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
   echo "Syntax: dockerRun [-h|i]"
   echo "Options:"
   echo "h     Display help."
   echo "i     Run in interactive mode (i.e., attach STDIN, STDOUT, STDERR)."
   echo
}


SERVICE_IMPLEMENTATION_NAME=`awk -v FS="IMPLEMENTATION_NAME=" 'NF>1{print $2}' ./src/main/resources/application.properties`
if [ "$SERVICE_IMPLEMENTATION_NAME" = "" ]; then
  PrintError "UNKNOWN SERVICE IMPLEMENTATION NAME. Make sure that IMPLEMENTATION_NAME is set in application.properties. Using spring.application.name property"
  SERVICE_IMPLEMENTATION_NAME=`awk -v FS="spring.application.name=" 'NF>1{print $2}' ./src/main/resources/application.properties`
  if [ "$SERVICE_IMPLEMENTATION_NAME" = "" ]; then
    PrintError "UNKNOWN SERVICE IMPLEMENTATION NAME. Make sure that IMPLEMENTATION_NAME is set in application.properties"
    exit 1
  fi
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
         PrintError "UNKNOWN OPTION $option"
         exit;;
   esac
done

docker start $DEFOPT $SERVICE_IMPLEMENTATION_NAME
PrintSuccess "Container $SERVICE_IMPLEMENTATION_NAME running."
