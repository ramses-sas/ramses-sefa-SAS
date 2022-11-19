from mysql.connector import *
import os

FILEPATH = os.path.dirname(os.path.realpath(__file__))

services = ["RESTAURANT-SERVICE", "ORDERING-SERVICE", "PAYMENT-PROXY-SERVICE", "DELIVERY-PROXY-SERVICE"]
qoses = ["Availability", "AverageResponseTime"]


try:
    with connect(
        host="localhost",
        user="sofauser",
        password="SaefaPW0!",
        database="knowledge"
    ) as connection:
        for service in services:
            for qos in qoses:
                print("\n",service, ": "+qos)
                select_query = "SELECT q.service_implementation_id, q.current_value, q.value, q.threshold, q.invalidates_this_and_previous, q.timestamp FROM qosvalue_entity q"
                select_query += " WHERE service_id = '"+service+"' AND qos = '"+qos+"' AND q.instance_id IS NULL ORDER BY q.timestamp ASC"
                timestamps = []
                current_values = []
                values = []
                thresholds = []
                invalidates = []
                implementation_ids = []
                with connection.cursor() as cursor:
                    cursor.execute(select_query)
                    result = cursor.fetchall()
                    # print("(Implementation ID, Current Value, Value, Threshold, Invalidates, Timestamp)")
                    for row in result:
                        # print(row)
                        implementation_ids.append(row[0])
                        current_values.append(row[1])
                        values.append(row[2])
                        thresholds.append(row[3])
                        invalidates.append(row[4])
                        timestamps.append(row[5])
                    values_coordinates = [(i+1, values[i]) for i in range(len(values))]
                    thresholds_coordinates = [(i+1, thresholds[i]) for i in range(len(thresholds))]
                    current_values_coordinates = [(i+1, current_values[i]) for i in range(len(current_values)) if current_values[i] is not None]
                    invalidates_coordinates = [(i+1, values[i]) for i in range(len(invalidates)) if invalidates[i] == 1]

                    adapt_query = "SELECT a.discriminator, a.service_implementation_id, a.timestamp FROM adaptation_option a"
                    adapt_query += " WHERE a.service_id = '"+service+"' ORDER BY a.timestamp ASC"
                    option_applied = []
                    option_timestamps = []
                    option_implementation_ids = []
                    with connection.cursor() as cursor2:
                        cursor2.execute(adapt_query)
                        result = cursor2.fetchall()
                        # print("(Discriminator, Implementation ID, Timestamp)")
                        for row in result:
                            # print(row)
                            option_applied.append(row[0])
                            option_implementation_ids.append(row[1])
                            option_timestamps.append(row[2])
                    
                    # In realtà basta farlo solo per un QoS
                    adaptation_coordinates = []
                    for i in range(len(option_timestamps)):
                        for j in range(len(timestamps)-1):
                            if option_timestamps[i] >= timestamps[j] and option_timestamps[i] < timestamps[j+1] and option_implementation_ids[i] == implementation_ids[j]:
                                adaptation_coordinates.append(j+1)
                                break           
                    
                    # adaptation_coordinates = [(i+1) for i in range(len(values)) if timestamps[i] in option_timestamps and implementation_ids[i] in option_implementation_ids]

                    files_to_generate = [values_coordinates, thresholds_coordinates, current_values_coordinates, invalidates_coordinates]
                    for file_to_generate in files_to_generate:
                        var_name = [ k for k,v in locals().items() if v is file_to_generate][0]
                        os.makedirs(FILEPATH+"/"+service, exist_ok = True)
                        with open(FILEPATH+"/"+service+"/"+qos+"_"+var_name.replace("_coordinates","")+".txt", "w") as f:
                            f.writelines("i value\n")
                            #print(var_name)
                            for val in file_to_generate:
                                #print(val[0], val[1])
                                f.writelines(str(val[0])+" "+str(val[1])+"\n")
                    with open(FILEPATH+"/"+service+"/"+qos+"_adaptation.txt", "w") as f:
                        f.writelines("i\n")
                        #print("adaptation_coordinates")
                        for val in adaptation_coordinates:
                            #print(val)
                            f.writelines(str(val)+"\n")
           
except Error as e:
    print(e)