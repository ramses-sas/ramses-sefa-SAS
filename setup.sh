for d in */; do
    if [ "${d: -7}" == "service" ] && [! "$d" = "eureka-registry-service/"] ; then
      echo "Starting $d"
      cd "$d" || return
      bash dockerBuild.sh
      bash dockerRun.sh
      cd ..
      fi
done
