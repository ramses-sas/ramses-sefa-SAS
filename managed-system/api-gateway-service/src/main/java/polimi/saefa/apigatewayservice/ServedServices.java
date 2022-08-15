package polimi.saefa.apigatewayservice;

public enum ServedServices {
    // Un enum case per servizio che necessita di load balancing da parte del gateway
    // Il nome dell'enum deve essere uguale al nome del servizio, con un _ invece di un -
    RESTAURANT_SERVICE,
    ORDERING_SERVICE;

    public String getServiceId() { return this.name().replace("_", "-"); }
    public String toLoadBalancerUri() {
        return "lb://" + this.getServiceId();
    }
}
