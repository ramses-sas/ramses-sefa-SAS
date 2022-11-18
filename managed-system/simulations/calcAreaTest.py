from mysql.connector import *
import os
from datetime import datetime

FILEPATH = os.path.dirname(os.path.realpath(__file__))

def compute_area(problematic_values_coordinates: list, threshold_of_problematic_values: list, x_coord_of_value_satis_thresh) -> float:
    area = 0.0
    n_values = len(problematic_values_coordinates)
    if n_values == 0:
        return area
    if n_values == 1:
        dx = x_coord_of_value_satis_thresh - problematic_values_coordinates[n_values-1][0]
        dy = abs(threshold_of_problematic_values[0] - problematic_values_coordinates[0][1])
        #print("Nval1 dx: ", dx, "dy: ", dy)
        return dx * dy/2
    for i in range(n_values-1): # Non considero l'ultimo valore
        dx = problematic_values_coordinates[i+1][0] - problematic_values_coordinates[i][0]
        dy1 = abs(threshold_of_problematic_values[i] - problematic_values_coordinates[i][1])
        dy2 = abs(threshold_of_problematic_values[i+1] - problematic_values_coordinates[i+1][1])
        area += dx * (dy1 + dy2) / 2
        #print("dx: ", dx, "dy1: ", dy1, "dy2: ", dy2, "area: ", area)
    # Considero l'ultimo valore e immagino che sia lineare fino al timestamp del valore che soddisfa il threshold
    dx = x_coord_of_value_satis_thresh - problematic_values_coordinates[n_values-1][0]
    dy = abs(threshold_of_problematic_values[n_values-1] - problematic_values_coordinates[n_values-1][1])
    #print("FinalVal dx: ", dx, "dy: ", dy)
    area += dx * dy/2
    return area

try:
    timestamps = []
    current_values = []
    values = []
    thresholds = []
    invalidates = []
    implementation_ids = []
    for i in range(10):
        timestamps.append(datetime(2022, 1, 1, 0, i, 0))
        values.append(8)
        thresholds.append(10)
    timestamps.append(datetime(2022, 1, 1, 0, 10, 0))
    values.append(12)
    thresholds.append(10)
    total_area = 0.0
    min_timestamp = timestamps[0].timestamp()
    new_segment_found = False
    problematic_values_coordinates = [] # Le coordinate dei valori che non rispettano la soglia
    threshold_of_problematic_values = [] # Le soglie dei valori che non rispettano la soglia
    # Così facendo perdiamo il pezzo di area che c'è quando passiamo da soglia rispettata a non e viceversa. Ma non fa niente visto che è una misura nostra
    for i in range(len(values)):
        if values[i] < thresholds[i]:
            new_segment_found = True
            print("Timestamp: ", timestamps[i])
            print("Timestamp.timestamp: ", timestamps[i].timestamp())
            print("Timestamp.timestamp - min_timestamp / 60: ", (timestamps[i].timestamp() - min_timestamp)/60)
            problematic_values_coordinates.append(((timestamps[i].timestamp() - min_timestamp)/60, values[i]))
            threshold_of_problematic_values.append(thresholds[i])
        else:
            if new_segment_found:
                total_area += compute_area(problematic_values_coordinates, threshold_of_problematic_values, (timestamps[i].timestamp() - min_timestamp)/60)
                problematic_values_coordinates = []
                threshold_of_problematic_values = []
                new_segment_found = False
    if new_segment_found:
        total_area += compute_area(problematic_values_coordinates, threshold_of_problematic_values, (timestamps[i].timestamp() - min_timestamp)/60)
        problematic_values_coordinates = []
        threshold_of_problematic_values = []
        new_segment_found = False
    print("Total area: "+str(total_area))
except Error as e:
    print(e)