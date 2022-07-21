Help()
{
   # Display Help
   echo "Syntax: setup [-h|e]"
   echo "Run without options to run every microservice locally in a dedicated container using an external Eureka Service."
   echo "Options:"
   echo "h     Display help."
   echo "e     Run also the Eureka Service locally."
}

# EUREKA_IP_PORT=""
while getopts "he" option; do
   case $option in
      h) # display Help
        Help
        exit;;
      e) # Run the Eureka Service locally
        cd "eureka-registry-service/" || return
        EUREKA_IP_PORT="172.0.0.10:8761"
        bash ../scripts/dockerBuild.sh -e "$EUREKA_IP_PORT"
        bash ../scripts/dockerRun.sh
        cd ..;;
     \?) # Wrong option
         echo "UNKNOWN OPTION $option"
         exit;;
   esac
done

echo "Staring all the services..."
echo

COUNT=2
for d in */; do
  if [ "${d: -8}" = "service/" ] && [ "$d" != "eureka-registry-service/" ] ; then
    echo "Starting $d"
    cd "$d" || return
    if [ "$EUREKA_IP_PORT" = "" ] ; then # eureka-registry-service is not running locally
      bash ../scripts/dockerBuild.sh
    else # eureka-registry-service is running locally
      bash ../scripts/dockerBuild.sh -e "$EUREKA_IP_PORT" -a "172.0.0.${COUNT}"
      COUNT=$((COUNT+1))
    fi
    bash ../scripts/dockerRun.sh
    cd ..
  fi
done

echo
echo "SETUP DONE. All containers should be up and running."
echo "Press 'q' to exit and stop all containers."
echo "Press 'x' to exit and keep all containers running."
echo

while : ; do
  read -n 1 k <&1
  if [[ $k = q ]] ; then
    echo "Stopping all containers..."
    for d in */; do
      if [ "${d: -8}" = "service/" ]; then
        cd "$d" || return
        SERVICE_NAME=`awk -v FS="spring.application.name=" 'NF>1{print $2}' ./src/main/resources/application.properties`
        docker stop $SERVICE_NAME
        cd ..
      fi
    done
    echo "Exiting..."
    exit
  else
    if [[ $k = x ]] ; then
      echo "Exiting..."
      exit
    else 
      echo "Press 'q' to exit and stop all containers."
      echo "Press 'x' to exit and keep all containers running."
      echo
    fi
  fi
done

