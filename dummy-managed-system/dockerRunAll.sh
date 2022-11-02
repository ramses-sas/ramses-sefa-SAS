#!/bin/bash
RED='\033[0;31m'
GREEN='\033[0;32m'
NC='\033[0m' # No Color

PrintError() {
  echo -e "${RED}$1${NC}"
  exit -1
}

PrintSuccess() {
  echo -e "${GREEN}$1${NC}"
}

docker ps >/dev/null 2>&1 || PrintError "Cannot connect to Docker daemon"

Help()
{
   # Display Help
   echo "Syntax: dockerRunAll [-h|r]"
   echo "Run without options to run every microservice in local mode."
   echo "     Only the microservices hosted on the same LAN can communicate with each other"
   echo "Options:"
   echo "h     Display help."
}

SCRIPTS_PATH="$( cd "scripts" && pwd && cd .. )"
HOST=`ifconfig | grep '\<inet\>' | cut -d ' ' -f2 | grep -v '127.0.0.1'`

while getopts "hl" option; do
   case $option in
      h) # display Help
        Help
        exit;;
     \?) # Wrong option
        PrintError "UNKNOWN OPTION";;
   esac
done

BuildNRun() {
  bash "$SCRIPTS_PATH/dockerBuild.sh"; bash "$SCRIPTS_PATH/dockerRun.sh"
}

BuildOnly() {
  bash "$SCRIPTS_PATH/dockerBuild.sh"
}

cd "../libs/config-parser/" || return
./gradlew clean; ./gradlew build
cd "../load-balancer/"
./gradlew clean; ./gradlew build
cd "../../dummy-managed-system/"

if [ "$IS_REMOTE" = "no" ] ; then
  cd "servers/eureka-registry-server/" || return
  bash "$SCRIPTS_PATH/dockerBuild.sh"; bash "$SCRIPTS_PATH/dockerRun.sh"
  cd ../..
  sleep 2
  cd "servers/config-server/" || return
  bash "$SCRIPTS_PATH/dockerBuild.sh"; bash "$SCRIPTS_PATH/dockerRun.sh"
  cd ../..
  echo; echo
  echo "Waiting for the config server to be ready..."
  sleep 10
fi



PrintSuccess "Starting all the services..."
echo


for d in */; do
  if [ "${d: -8}" = "service/" ] ; then
    echo; echo; echo
    PrintSuccess "Starting $d"
    cd "$d" || return
    BuildNRun
    cd ..
    echo; echo; echo
  fi
done

echo
PrintSuccess "SETUP DONE. All containers should be up and running."
