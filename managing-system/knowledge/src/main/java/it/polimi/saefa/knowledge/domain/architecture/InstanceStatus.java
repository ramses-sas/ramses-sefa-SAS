package it.polimi.saefa.knowledge.domain.architecture;

/*public final class InstanceStatus {
    public static final String ACTIVE = "ACTIVE";
    public static final String FAILED = "FAILED";
    public static final String SHUTDOWN = "SHUTDOWN";

    private InstanceStatus() {};
}
 */

public enum InstanceStatus {
    ACTIVE,
    UNREACHABLE,
    FAILED,
    SHUTDOWN
}
