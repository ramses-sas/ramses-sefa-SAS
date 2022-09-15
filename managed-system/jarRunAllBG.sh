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

RunJar() {
  java -jar ./build/libs/*-latest.jar &
}

cd "servers/eureka-registry-server/" || return
RunJar
cd ../..
cd "servers/config-server/" || return
RunJar
cd ../..
echo; echo
echo "Waiting for the config server to be ready..."
sleep 10



PrintSuccess "Starting all the services..."
echo


for d in */; do
  if [ "${d: -8}" = "service/" ] ; then
    echo; echo
    PrintSuccess "Starting $d"
    cd "$d" || return
    RunJar
    cd ..
    echo; echo
  else
    if [ "${d: -8}" = "proxies/" ]; then
      echo; echo
      PrintSuccess "Starting proxies..."
      echo
      cd "$d" || return
      for dd in */; do
        if [ "${dd: -10}" = "1-service/" ]; then
          echo; echo
          PrintSuccess "Starting $dd"
          cd "$dd" || return
          RunJar
          cd ..
          echo; echo
        fi
      done
      cd ..
    fi
  fi
done
