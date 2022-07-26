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
   echo "Syntax: setup [-h|e]"
   echo "Run without options to run every microservice locally in a dedicated container using an external Eureka Service."
   echo "Options:"
   echo "h     Display help."
   echo "e     Run also the Eureka Service locally."
}

SCRIPTS_PATH="$( cd "scripts" && pwd && cd .. )"

# EUREKA_IP_PORT=""
while getopts "he" option; do
   case $option in
      h) # display Help
        Help
        exit;;
      e) # Run the Eureka Service locally
        cd "eureka-registry-service/" || return
        EUREKA_IP_PORT="172.0.0.2:58082"
        bash "$SCRIPTS_PATH/dockerBuild.sh" -e "$EUREKA_IP_PORT" -i "172.0.0.2"
        bash "$SCRIPTS_PATH/dockerRun.sh"
        cd ..;;
     \?) # Wrong option
        PrintError "UNKNOWN OPTION $option"
        exit;;
   esac
done

PrintSuccess "Starting all the services..."
echo

COUNT=3

BuildNRun() {
  if [ "$EUREKA_IP_PORT" = "" ] ; then # eureka-registry-service is not running locally
    bash "$SCRIPTS_PATH/dockerBuild.sh" -e "52.208.38.53:58082"
  else # eureka-registry-service is running locally
    bash "$SCRIPTS_PATH/dockerBuild.sh" -e "$EUREKA_IP_PORT" -a "172.0.0.${COUNT}" -i "172.0.0.${COUNT}"
    COUNT=$(($COUNT+1))
  fi
  bash "$SCRIPTS_PATH/dockerRun.sh"
}


for d in */; do
  if [ "${d: -8}" = "service/" ] && [ "$d" != "eureka-registry-service/" ] ; then
    if [ "$d" = "delivery-proxy-service/" ] || [ "$d" = "payment-proxy-service/" ] ; then
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
    else
      echo; echo; echo; echo
      PrintSuccess "Starting $d"
      cd "$d" || return
      BuildNRun
      cd ..
      echo; echo; echo; echo
    fi
  fi
done

echo
PrintSuccess "SETUP DONE. All containers should be up and running."
echo "Press 'q' to exit and stop all containers."
echo "Press 'x' to exit and keep all containers running."
echo

DockerStop() {
  docker stop `awk -v FS="spring.application.name=" 'NF>1{print $2}' ./src/main/resources/application.properties`
}

while : ; do
  read -n 1 k <&1
  if [[ $k = q ]] ; then
    echo
    PrintSuccess "Stopping all containers..."
    for d in */; do
      if [ "${d: -8}" = "service/" ] ; then
        if [ "$d" = "delivery-proxy-service/" ] || [ "$d" = "payment-proxy-service/" ] ; then
          cd "$d" || return
          for dd in */; do
            cd "$dd" || return
            DockerStop
            cd ..
          done
          cd ..
        else
          cd "$d" || return
          DockerStop
          cd ..
        fi
      fi
    done
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
