package it.polimi.saefa.monitor.prometheus;

/*
import java.util.HashMap;
import java.util.List;
import java.util.Map;


//@Controller
public class MetricsRepository {
    // Map<InstanceId, Map<endpoint, List<{method, outcome, status, uri, count, totalDuration}>>>
    private final Map<String, InstanceMetrics> instancesMetrics;

    public MetricsRepository() {
        instancesMetrics = new HashMap<>();
    }

    public InstanceMetrics newInstanceMetrics(String instanceId) {
        InstanceMetrics instanceMetrics = new InstanceMetrics(instanceId);
        instancesMetrics.put(instanceId, instanceMetrics);
        return instanceMetrics;
    }

    public void addHttpMetrics(String instanceId, HttpRequestMetrics metrics) {
        instancesMetrics.get(instanceId).addHttpMetrics(metrics);
    }

    public List<HttpRequestMetrics> getHttpMetrics(String instanceId, String endpoint) {
        return instancesMetrics.get(instanceId).getHttpMetrics(endpoint);
    }

    public List<HttpRequestMetrics> getHttpMetrics(String instanceId, String endpoint, String method) {
        return instancesMetrics.get(instanceId).getHttpMetrics(endpoint, method);
    }

    public List<HttpRequestMetrics> getHttpMetrics(String instanceId, String endpoint, String method, String outcome) {
        return instancesMetrics.get(instanceId).getHttpMetrics(endpoint, method, outcome);
    }

    public void setCpuUsage(String instanceId, double cpuUsage) {
        instancesMetrics.get(instanceId).cpuUsage = cpuUsage;
    }

    public void setDiskTotalSpace(String instanceId, double diskTotalSpace) {
        instancesMetrics.get(instanceId).diskTotalSpace = diskTotalSpace;
    }

    public void setDiskFreeSpace(String instanceId, double diskFreeSpace) {
        instancesMetrics.get(instanceId).diskFreeSpace = diskFreeSpace;
    }

}

 */
