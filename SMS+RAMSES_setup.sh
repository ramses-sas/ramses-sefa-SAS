#!/bin/bash
PrintSuccess() { echo -e "\033[0;32m$1\033[0m"; }
PrintWarn() { echo -e "\033[0;33m$1\033[0m"; }
PrintError() { echo -e "\033[0;31m$1\033[0m"; }

if [[("${GITHUB_OAUTH}" = "") || ("${GITHUB_REPOSITORY_URL}" = "")]]; then
  PrintError "Env var GITHUB_OAUTH and GITHUB_REPOSITORY_URL must be set!"
  exit 1
fi

usage() {
  echo "Usage: [-a <arch>] [-l]"
  echo "-a <arch>: Desired architecture. Supported values are 'arm64' and 'amd64'. Default is 'arm64'"
  echo "-l: start only the load generator"
  exit 1
}

loadgen() {
  PrintSuccess "Setting up Load Generator"
  docker pull sbi98/sefa-load-generator
  docker run -P --name sefa-load-generator -d --network ramses-sas-net sbi98/sefa-load-generator:$ARCH
  exit 0
}

LOADGEN=false
while getopts a:l option
do
  case "${option}" in
    a) ARCH=${OPTARG};;
    l) LOADGEN=true;;
    *) usage;;
  esac
done

if [[(${ARCH} != "arm64") && ( ${ARCH} != "amd64")]]; then
  PrintWarn "Desired architecture not specified or unknown. Supported values are 'arm64' and 'amd64'. Using 'arm64' as default option"
  ARCH="arm64"
else
   PrintSuccess "Running script with selceted architecture: ${ARCH}"
fi


##### Network #####
PrintSuccess "Creating new Docker network called 'ramses-sas-net'"
docker network create ramses-sas-net
echo

##### MYSQL #####
PrintSuccess "Setting up MySQL Server"
docker pull sbi98/mysql:$ARCH
docker run -P --name mysql -d --network ramses-sas-net sbi98/mysql:$ARCH
echo
sleep 2

##### SMS #####
echo; PrintSuccess "Setting up the Simple Managed System!"; echo 

PrintSuccess "Setting up Netflix Eureka Server"
docker pull sbi98/sms-eureka:$ARCH
docker run -P --name sms-eureka -d --network ramses-sas-net sbi98/sms-eureka:$ARCH
echo
sleep 2
PrintSuccess "Setting up Spring Config Server"
docker pull sbi98/sms-configserver:$ARCH
docker run -P --name sms-configserver -e GITHUB_REPOSITORY_URL=$GITHUB_REPOSITORY_URL -d --network ramses-sas-net sbi98/sms-configserver:$ARCH
echo
sleep 10

declare -a arr=("sms-randint-vendor-service" 
                "sms-randint-producer-service"
                "sms-api-gateway"
                )
for i in "${arr[@]}"
do
   PrintSuccess "Setting up $i"
   docker pull sbi98/$i:$ARCH
   docker run -P --name $i -d --network ramses-sas-net sbi98/$i:$ARCH
   echo
   sleep 1
done


##### PROBE AND ACTUATORS #####
echo; PrintSuccess "Setting up probe and actuators!"; echo 

declare -a pract=("sms-probe" "sms-instances-manager")
for i in "${pract[@]}"
do
   PrintSuccess "Pulling $i"
   docker pull sbi98/$i:$ARCH
   docker run -P --name $i -d --network ramses-sas-net sbi98/$i:$ARCH
   echo
   sleep 1
done

PrintSuccess "Pulling sms-config-manager"
docker pull sbi98/sms-config-manager:$ARCH
docker run -P --name sms-config-manager -e GITHUB_OAUTH=$GITHUB_OAUTH -e GITHUB_REPOSITORY_URL=$GITHUB_REPOSITORY_URL -d --network ramses-sas-net sbi98/sms-config-manager:$ARCH
echo
sleep 1

##### RAMSES #####
echo; PrintSuccess "Setting up the Managing System, RAMSES!"; echo 

PrintSuccess "Pulling ramses-knowledge"
docker pull sbi98/ramses-knowledge:$ARCH
docker run -P --name ramses-knowledge -e CONFIGURATION_PATH=/app/architecture_sla/sms -e PROBE_URL=http://sms-probe:58020 -d --network ramses-sas-net sbi98/ramses-knowledge:$ARCH
echo
sleep 1
declare -a ramsesarr=("ramses-analyse" "ramses-plan" "ramses-execute" "ramses-monitor" "ramses-dashboard")
for i in "${ramsesarr[@]}"
do
   PrintSuccess "Pulling $i"
   docker pull sbi98/$i:$ARCH
   docker run -P --name $i -d --network ramses-sas-net sbi98/$i:$ARCH
   echo
   sleep 1
done

echo; PrintSuccess "DONE!"; echo 
echo; PrintWarn "A load generator is also available on Docker Hub. The image is sbi98/sms-load-generator"; echo 