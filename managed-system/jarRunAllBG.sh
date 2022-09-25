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

export EUREKA_IP_PORT=localhost:58082 
export MYSQL_IP_PORT=localhost:3306


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

KILL_COMMAND="kill"
RunJar() {
  java -jar ./build/libs/*-latest.jar > /dev/null 2>&1 &
  KILL_COMMAND="$KILL_COMMAND $!"
}

cd "servers/eureka-registry-server/" || return
PrintSuccess "Starting Eureka Server..."
RunJar
cd ../..
cd "servers/config-server/" || return
PrintSuccess "Starting Config Server..."
RunJar
cd ../..
echo; echo
echo "Waiting for the config server to be ready..."
sleep 10



PrintSuccess "Starting all the services..."
echo


for d in */; do
  if [ "${d: -8}" = "service/" ] ; then
    PrintSuccess "Starting $d"
    cd "$d" || return
    RunJar
    cd ..
  else
    if [ "${d: -8}" = "proxies/" ]; then
      cd "$d" || return
      for dd in */; do
        if [ "${dd: -10}" = "1-service/" ]; then
          PrintSuccess "Starting $dd"
          cd "$dd" || return
          RunJar
          cd ..
        fi
      done
      cd ..
    fi
  fi
done

PrintSuccess "All services started"
echo "$KILL_COMMAND; rm killAll.sh" > killAll.sh
