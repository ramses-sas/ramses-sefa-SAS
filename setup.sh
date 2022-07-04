for d in */; do
    if [ ! "$d" = "API-Gateway-service/" ] ; then
      echo "Starting $d"
      cd "$d" || return
      bash dockerBuild.sh
      bash dockerRun.sh
      cd ..
      fi
done
echo "Waiting for services to start"
sleep 5
cd API-Gateway-service || exit
echo "Starting API-Gateway-service"
bash dockerBuild.sh
bash dockerRun.sh