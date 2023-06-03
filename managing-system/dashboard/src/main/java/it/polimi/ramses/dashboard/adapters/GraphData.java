package it.polimi.ramses.dashboard.adapters;

import it.polimi.ramses.knowledge.domain.adaptation.values.QoSHistory;
import lombok.Data;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

@Data
public class GraphData {
    private String xAxisName;
    private String yAxisName;
    private Double threshold;
    // List of points for series 1. [y1]
    private List<QoSHistory.Value> pointsBefore = new ArrayList<>();
    // List of points for series 2. [y2]
    private List<QoSHistory.Value> pointsAfter = new ArrayList<>();
    // List of points for series + threshold. [[x, y1, y2, threshold]]
    private List<List<Object>> points;

    private final SimpleDateFormat sdf;

    public GraphData(String xAxisName, String yAxisName, Double threshold) {
        this.xAxisName = xAxisName;
        this.yAxisName = yAxisName;
        this.threshold = threshold;
        sdf = new SimpleDateFormat("HH:mm:ss");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    public boolean isEmpty() {
        return points == null || points.isEmpty();
    }

    public void addPointBefore(QoSHistory.Value y1) {
        pointsBefore.add(y1);
    }

    public void addPointAfter(QoSHistory.Value y2) {
        pointsAfter.add(y2);
    }

    public void generateAggregatedPoints() {
        points = new ArrayList<>();
        int pointsBeforeSize = pointsBefore.size();
        for (int i = 0; i < pointsBefore.size() + pointsAfter.size(); i++) {
            List<Object> point = new ArrayList<>();
            QoSHistory.Value pointToAdd;
            if (i < pointsBeforeSize) {
                pointToAdd = pointsBefore.get(i);
                point.add(sdf.format(pointToAdd.getTimestamp())+(pointToAdd.invalidatesThisAndPreviousValues() ? "\nINV" : "")); // x
                point.add(pointToAdd.getDoubleValue()); // y1
                if (i == pointsBeforeSize-1) {
                    point.add(pointToAdd.getDoubleValue()); // y2
                } else
                    point.add(null); // y2
            } else {
                pointToAdd = pointsAfter.get(i-pointsBeforeSize);
                point.add(sdf.format(pointToAdd.getTimestamp())+(pointToAdd.invalidatesThisAndPreviousValues() ? "\nINV" : "")); // x
                point.add(null); // y1
                point.add(pointToAdd.getDoubleValue()); // y2
            }
            point.add(threshold);
            points.add(point);
        }
    }

}
