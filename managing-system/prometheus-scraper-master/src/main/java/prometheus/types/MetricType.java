package prometheus.types;

public enum MetricType {
    COUNTER, GAUGE, SUMMARY, HISTOGRAM;

    public Class<? extends Metric> getMetricClass() {
        return switch (this) {
            case COUNTER -> Counter.class;
            case GAUGE -> Gauge.class;
            case SUMMARY -> Summary.class;
            case HISTOGRAM -> Histogram.class;
        };
    }
}
