package it.polimi.saefa.knowledge.persistence.domain.adaptation;

public class AverageResponseTime extends AdaptationParameter{

    private double maxThreshold;

    @Override
    public boolean isSatisfied() {
        return super.getValue()<= maxThreshold;
    }

    public AverageResponseTime(String json) {
        super(json);
    }

    @Override
    public void parseFromJson(String json) {

    }
}
