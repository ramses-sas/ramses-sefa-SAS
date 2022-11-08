from mysql.connector import *
import os

FILEPATH = os.path.dirname(os.path.realpath(__file__))

services = ["RESTAURANT-SERVICE", "ORDERING-SERVICE", "PAYMENT-PROXY-SERVICE", "DELIVERY-PROXY-SERVICE"]
qoses = ["Availability", "AverageResponseTime"]

def compute_area(problematic_values_coordinates: list, threshold_of_problematic_values: list, timestamp_of_value_satisfying_threshold) -> float:
    area = 0.0
    n_values = len(problematic_values_coordinates)
    if n_values < 2:
        return area
    for i in range(n_values-1): # Non considero l'ultimo valore
        dx = problematic_values_coordinates[i+1][0] - problematic_values_coordinates[i][0]
        dy1 = abs(threshold_of_problematic_values[i] - problematic_values_coordinates[i][1])
        dy2 = abs(threshold_of_problematic_values[i+1] - problematic_values_coordinates[i+1][1])
        area += dx * (dy1 + dy2) / 2
    # Considero l'ultimo valore e immagino che sia costante fino al timestamp del valore che soddisfa il threshold
    dx = timestamp_of_value_satisfying_threshold.timestamp() - problematic_values_coordinates[n_values-1][0]
    dy = abs(threshold_of_problematic_values[n_values-1] - problematic_values_coordinates[n_values-1][1])
    area += dx * dy
    return area

try:
    with connect(
        host="localhost",
        user="saefauser",
        password="SaefaPW0!",
        database="knowledge"
    ) as connection:
        for service in services:
            for qos in qoses:
                print("\n"+service, ": "+qos)
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
                    min_timestamp = timestamps[0].timestamp()
                    values_coordinates = [((timestamps[i].timestamp() - min_timestamp)/60, values[i]) for i in range(len(values))]
                    thresholds_coordinates = [((timestamps[i].timestamp() - min_timestamp)/60, thresholds[i]) for i in range(len(thresholds))]
                    current_values_coordinates = [((timestamps[i].timestamp() - min_timestamp)/60, current_values[i]) for i in range(len(current_values)) if current_values[i] is not None]
                    invalidates_coordinates = [((timestamps[i].timestamp() - min_timestamp)/60, values[i]) for i in range(len(invalidates)) if invalidates[i] == 1]

                    total_area = 0.0
                    new_segment_found = False
                    problematic_values_coordinates = [] # Le coordinate dei valori che non rispettano la soglia
                    threshold_of_problematic_values = [] # Le soglie dei valori che non rispettano la soglia
                    # Così facendo perdiamo il pezzo di area che c'è quando passiamo da soglia rispettata a non e viceversa. Ma non fa niente visto che è una misura nostra
                    for i in range(len(values_coordinates)):
                        if qos == "Availability":
                            if values_coordinates[i][1] < thresholds_coordinates[i][1]:
                                new_segment_found = True
                                problematic_values_coordinates.append(values_coordinates[i])
                                threshold_of_problematic_values.append(thresholds_coordinates[i][1])
                            else:
                                if new_segment_found:
                                    total_area += compute_area(problematic_values_coordinates, threshold_of_problematic_values, timestamps[i])
                                    problematic_values_coordinates = []
                                    threshold_of_problematic_values = []
                                    new_segment_found = False
                        elif qos == "AverageResponseTime":
                            if values_coordinates[i][1] > thresholds_coordinates[i][1]:
                                new_segment_found = True
                                problematic_values_coordinates.append(values_coordinates[i])
                                threshold_of_problematic_values.append(thresholds_coordinates[i][1])
                            else:
                                if new_segment_found:
                                    total_area += compute_area(problematic_values_coordinates, threshold_of_problematic_values, timestamps[i])
                                    problematic_values_coordinates = []
                                    threshold_of_problematic_values = []
                                    new_segment_found = False
                    print("Total area for "+qos+": "+str(total_area))

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
                                adaptation_coordinates.append((timestamps[j].timestamp() - min_timestamp)/60)
                                break           
                    
                    # adaptation_coordinates = [(i+1) for i in range(len(values)) if timestamps[i] in option_timestamps and implementation_ids[i] in option_implementation_ids]

                    files_to_generate = [values_coordinates, thresholds_coordinates, current_values_coordinates, invalidates_coordinates]
                    os.makedirs(FILEPATH+"/"+service, exist_ok = True)
                    for file_to_generate in files_to_generate:
                        var_name = [ k for k,v in locals().items() if v is file_to_generate][0]
                        with open(FILEPATH+"/"+service+"/"+qos+"_"+var_name.replace("_coordinates","")+".txt", "w") as f1:
                            f1.writelines("i value\n")
                            for val in file_to_generate:
                                f1.writelines(f'{val[0]:.3f}'+" "+f'{val[1]:.3f}'+"\n")
                    with open(FILEPATH+"/"+service+"/"+qos+"_adaptation.txt", "w") as f2:
                        f2.writelines("i\n")
                        for val in adaptation_coordinates:
                            f2.writelines(f'{val:.3f}'+"\n")
                    with open(FILEPATH+"/"+service+"/"+qos+"_area.txt", "w") as f3:
                        f3.writelines("value\n"+f'{total_area:.5f}')
           
except Error as e:
    print(e)