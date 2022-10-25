
from mysql.connector import *
import os

services = ["RESTAURANT-SERVICE", "ORDERING-SERVICE", "PAYMENT-PROXY-SERVICE", "DELIVERY-PROXY-SERVICE"]
qoses = ["Availability", "AverageResponseTime"]


try:
    with connect(
        host="localhost",
        user="saefauser",
        password="SaefaPW0!",
        database="knowledge"
    ) as connection:
        print(connection)
        for service in services:
            for qos in qoses:
                print("\n",service, ": "+qos)
                select_query = "SELECT q.service_implementation_id, q.current_value, q.value, q.threshold, q.invalidates_this_and_previous, q.timestamp FROM qosvalue_entity q"
                select_query += " WHERE service_id = '"+service+"' AND qos = '"+qos+"' AND q.instance_id IS NULL LIMIT 20"
                with connection.cursor() as cursor:
                    timestamps = []
                    current_values = []
                    values = []
                    thresholds = []
                    invalidates = []
                    implementation_ids = []
                    cursor.execute(select_query)
                    result = cursor.fetchall()
                    print("(SERVICE IMPLEMENTATION ID, CURRENT VALUE, VALUE, THRESHOLD, INVALIDATES HISTORY, TIMESTAMP)")
                    for row in result:
                        print(row)
                        implementation_ids.append(row[0])
                        current_values.append(row[1])
                        values.append(row[2])
                        thresholds.append(row[3])
                        invalidates.append(row[4])
                        timestamps.append(row[5])
                    values_coordinates = [(i+1, values[i]) for i in range(len(values))]
                    thresholds_coordinates = [(i+1, thresholds[i]) for i in range(len(thresholds))]
                    current_values_coordinates = [(i+1, current_values[i] if current_values[i] is not None else 0.0) for i in range(len(current_values))]
                    invalidates_coordinates = [(i+1, values[i]) for i in range(len(invalidates)) if invalidates[i] == 1]

                    files_to_generate = [values_coordinates, thresholds_coordinates, current_values_coordinates, invalidates_coordinates]
                    for file_to_generate in files_to_generate:
                        var_name = [ k for k,v in locals().items() if v is file_to_generate][0]
                        os.makedirs(service, exist_ok = True)
                        with open(service+"/"+qos+"_"+var_name.replace("_coordinates","")+".txt", "w") as f:
                            for val in file_to_generate:
                                print(val[0], val[1])
                                f.writelines(str(val[0])+" "+str(val[1])+"\n")
            break
                    
except Error as e:
    print(e)