SERVICE_NAME=`awk -v FS="spring.application.name=" 'NF>1{print $2}' ./src/main/resources/application.properties`
if [ "$SERVICE_NAME" = "" ]; then
    SERVICE_NAME="unknown-service"
    echo "UNKNOWN SERVICE_NAME"
fi

SERVICE_PORT=`awk -v FS="server.port=" 'NF>1{print $2}' ./src/main/resources/application.properties`
if [ "$SERVICE_PORT" = "" ]; then
    SERVICE_PORT="8081"
fi

PORT="8080"
PORT_OPTION="${PORT}:${SERVICE_PORT}"
while getopts "p:r" option; do
   case $option in
      p) # Port
        PORT_OPTION="${OPTARG}:${SERVICE_PORT}";;
      r) # Random port
        PORT_OPTION="${SERVICE_PORT}"
        PORT="";;
     \?) # Wrong option
         echo "UNKNOWN OPTION";;
   esac
done

../gradlew build
docker build -t $SERVICE_NAME .
docker stop $SERVICE_NAME
docker rm $SERVICE_NAME
docker create -p $PORT_OPTION -i -t --name $SERVICE_NAME $SERVICE_NAME
if [ "$PORT" = "" ]; then
    echo "Exposing service $SERVICE_NAME on a random port"
else
    echo "Exposing service $SERVICE_NAME on port $PORT"
fi