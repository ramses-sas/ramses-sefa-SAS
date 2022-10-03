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
   echo "     The addresses of the server microservices and of the API gateway are defined in the application.properties file of the config repository"
   echo "     It does not run the Eureka Server, the MySQL Server and the Config Server"
   echo "Options:"
   echo "h     Display help."
   echo "r     Run the microservice in the remote mode."
   echo "         It uses the public IP address of the machine, obtained through an external service. "
   echo "         It usually requires a port forwarding for the microservice ports."
   echo "         The addresses of the Eureka Server, of the MySQL Server and of the API Gateway are retrieved from the config repository"
}

SCRIPTS_PATH="$( cd "scripts" && pwd && cd .. )"
HOST=`ifconfig | grep '\<inet\>' | cut -d ' ' -f2 | grep -v '127.0.0.1'`
IS_REMOTE="no"


while getopts "hl" option; do
   case $option in
      h) # display Help
        Help
        exit;;
      r)
        HOST="$(curl https://ipinfo.io/ip)"
        IS_REMOTE="yes";;
     \?) # Wrong option
        PrintError "UNKNOWN OPTION";;
   esac
done

BuildNRun() {
  if [ "$IS_REMOTE" = "no" ] ; then
    bash "$SCRIPTS_PATH/dockerBuild.sh"; bash "$SCRIPTS_PATH/dockerRun.sh"
  else
    bash "$SCRIPTS_PATH/dockerBuild.sh" -r; bash "$SCRIPTS_PATH/dockerRun.sh"
  fi
}

if [ "$IS_REMOTE" = "no" ] ; then
  cd "servers/eureka-registry-server/" || return
  bash "$SCRIPTS_PATH/dockerBuild.sh"; bash "$SCRIPTS_PATH/dockerRun.sh"
  cd ../..
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
    echo; echo; echo; echo
    PrintSuccess "Starting $d"
    cd "$d" || return
    BuildNRun
    cd ..
    echo; echo; echo; echo
  else
    if [ "${d: -8}" = "proxies/" ]; then
      echo; echo; echo; echo
      PrintSuccess "Starting proxies..."
      echo
      cd "$d" || return
      for dd in */; do
        echo; echo; echo; echo
        PrintSuccess "Starting $dd"
        cd "$dd" || return
        BuildNRun
        cd ..
        echo; echo; echo; echo
      done
      cd ..
    fi
  fi
done

echo
PrintSuccess "SETUP DONE. All containers should be up and running."
echo "Press 'q' to exit and stop all containers."
echo "Press 'x' to exit and keep all containers running."
echo

DockerStop() {
  SERVICE_IMPLEMENTATION_NAME=`awk -v FS="IMPLEMENTATION_NAME=" 'NF>1{print $2}' ./src/main/resources/application.properties`
  if [ "$SERVICE_IMPLEMENTATION_NAME" = "" ]; then
    PrintError "UNKNOWN SERVICE IMPLEMENTATION NAME. Make sure that IMPLEMENTATION_NAME is set in application.properties. Using spring.application.name property"
    SERVICE_IMPLEMENTATION_NAME=`awk -v FS="spring.application.name=" 'NF>1{print $2}' ./src/main/resources/application.properties`
  fi
  docker stop $SERVICE_IMPLEMENTATION_NAME
}

while : ; do
  read -n 1 k <&1
  if [[ $k = q ]] ; then
    echo
    PrintSuccess "Stopping all containers..."
    for d in */; do
      if [ "${d: -8}" = "service/" ] ; then
        cd "$d" || return
        DockerStop
        cd ..
      else
        if [ "${d: -8}" = "proxies/" ] ; then
          cd "$d" || return
          for dd in */; do
            cd "$dd" || return
            DockerStop
            cd ..
          done
          cd ..
        fi
      fi
    done
    if [ "$IS_REMOTE" = "no" ] ; then
      cd "servers/eureka-registry-server/" || return
      DockerStop
      cd ../..
      cd "servers/config-server/" || return
      DockerStop
      cd ../..
    fi
    echo
    PrintSuccess "Exiting..."
    exit
  else
    if [[ $k = x ]] ; then
      echo
      PrintSuccess "Exiting..."
      exit
    else 
      echo
      echo "Press 'q' to exit and stop all containers."
      echo "Press 'x' to exit and keep all containers running."
      echo
    fi
  fi
done

