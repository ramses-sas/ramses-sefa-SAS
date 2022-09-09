package it.polimi.saefa.dashboard.adapters;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class GraphData {
    private String xAxisName;
    private String yAxisName;
    private List<List<Object>> points = new ArrayList<>();

    public GraphData(String xAxisName, String yAxisName) {
        this.xAxisName = xAxisName;
        this.yAxisName = yAxisName;
    }

    public void addPoint(Object x, Object y) {
        List<Object> point = new ArrayList<>();
        point.add(x);
        point.add(y);
        points.add(point);
    }

}


/*
    private List<String> xAxisValues = new ArrayList<>();
    private List<Double> yAxisValues = new ArrayList<>();

    public void addXAxisValue(String value) {
        xAxisValues.add(value);
    }

    public void addYAxisValue(Double value) {
        yAxisValues.add(value);
    }

    public List<GraphPoint> getPoints() {
        List<GraphPoint> points = new ArrayList<>();
        for (int i = 0; i < xAxisValues.size(); i++) {
            points.add(new GraphPoint(xAxisValues.get(i), yAxisValues.get(i)));
        }
        return points;
    }

    public List<List<Object>> getXYArray2() {
        List<List<Object>> points = new ArrayList<>();
        for (int i = 0; i < xAxisValues.size(); i++) {
            List<Object> xy = new ArrayList<>();
            xy.add(xAxisValues.get(i));
            xy.add(yAxisValues.get(i));
            points.add(xy);
        }
        return points;
    }

    public List<double[]> getXYArray() {
        List<double[]> points = new ArrayList<>();
        for (int i = 0; i < xAxisValues.size(); i++) {
            double[] xy = new double[2];
            xy[0] = Double.parseDouble(xAxisValues.get(i));
            xy[1] = yAxisValues.get(i);
            points.add(xy);
        }
        return points;
    }

    static class GraphPoint {
        private String x;
        private Double y;

        public GraphPoint(String x, Double y) {
            this.x = x;
            this.y = y;
        }

        public String getX() {
            return x;
        }

        public Double getY() {
            return y;
        }
    }
     */
