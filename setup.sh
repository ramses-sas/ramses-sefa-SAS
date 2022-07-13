for d in */; do
    if [ ! "$d" = "eureka-service-registry/" ] ; then
      echo "Starting $d"
      cd "$d" || return
      bash dockerBuild.sh
      bash dockerRun.sh
      cd ..
      fi
done
