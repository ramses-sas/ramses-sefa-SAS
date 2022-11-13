from datetime import timedelta
import os
FILEPATH = os.path.dirname(os.path.realpath(__file__))
service = "PAYMENT-PROXY-SERVICE"
initial_n_of_instances = 1


# timedelta(hour, minute, second, microsecond)
start_time = timedelta(hours = 22, minutes = 15, seconds = 19) # l'istanza 58090 era già accesa
end_time = timedelta(hours = 22, minutes = 25, seconds = 9)

start_instance_events = [ # UNA NUOVA ISTANZA PUO' RICEVERE RICHIESTE (E' SU EUREKA)
    # (timestampt di EUREKA) - Registering application PAYMENT-PROXY-SERVICE with eureka with status UP
    timedelta(hours = 22, minutes = 16, seconds = 5), # l'istanza 58090 è stata riavviata dal simulatore
]
stop_instance_events = [ # when the instance deregisters from Eureka
    # (timestampt di EUREKA) - Completed shut down of DiscoveryClient
    timedelta(hours = 22, minutes = 15, seconds = 53), #  l'istanza 58090 è stata spenta dal simulatore (failure injection)
]
total_external_events = []
total_external_events.extend(start_instance_events)
total_external_events.extend(stop_instance_events)
total_external_events.sort()
print(total_external_events)

# DAL POV DEL MANAGING
add_instance_events = [ # when the instance is again in the list of instances of the service
]
shutdown_instance_events = [ # when the STOP INSTANCE option is applied by the actuator
]
total_managing_events = []
total_managing_events.extend(add_instance_events)
total_managing_events.extend(shutdown_instance_events)
total_managing_events.sort()
print(total_managing_events)

os.makedirs(FILEPATH+"/"+service, exist_ok = True)
with open(FILEPATH+"/"+service+"/real_n_of_instances.txt", "w") as f1:
    f1.writelines("i value\n")
    f1.writelines("0 "+str(initial_n_of_instances)+"\n")
    current_n_of_instances = initial_n_of_instances
    for event in total_external_events:
        if event in start_instance_events:
            current_n_of_instances += 1
        else:
            current_n_of_instances -= 1
        #print((event-start_time).total_seconds()/60+" "+current_n_of_instances+"\n")
        f1.writelines(str((event-start_time).total_seconds())+" "+str(current_n_of_instances)+"\n")
    f1.writelines(str((end_time-start_time).total_seconds())+" "+str(current_n_of_instances)+"\n")
with open(FILEPATH+"/"+service+"/managing_n_of_instances.txt", "w") as f1:
    f1.writelines("i value\n")
    f1.writelines("0 "+str(initial_n_of_instances)+"\n")
    current_n_of_instances = initial_n_of_instances
    for event in total_managing_events:
        if event in add_instance_events:
            current_n_of_instances += 1
        else:
            current_n_of_instances -= 1
        #print((event-start_time).total_seconds()/60+" "+current_n_of_instances+"\n")
        f1.writelines(str((event-start_time).total_seconds())+" "+str(current_n_of_instances)+"\n")
    f1.writelines(str((end_time-start_time).total_seconds())+" "+str(current_n_of_instances)+"\n")

#SELECT * FROM knowledge.instance_metrics_snapshot ims1 where service_id = "PAYMENT-PROXY-SERVICE" 
#AND timestamp >= IFNULL(
#	(SELECT timestamp t from knowledge.instance_metrics_snapshot ims2 where ims2.status = "UNREACHABLE" and ims2.instance_id = ims1.instance_id ORDER BY TIMESTAMP DESC LIMIT 1),
#    (SELECT timestamp t from knowledge.instance_metrics_snapshot ims2 WHERE ims2.instance_id = ims1.instance_id ORDER BY TIMESTAMP ASC LIMIT 1)
#);