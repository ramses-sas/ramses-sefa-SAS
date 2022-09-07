package it.polimi.saefa.knowledge.persistence.domain.adaptation;

public class MaxResponseTime extends AdaptationParameter {
    private double maxThreshold;

    @Override
    public boolean isSatisfied() {
        return super.getValue()<= maxThreshold;
    }

    public MaxResponseTime(String json) {
        super(json);
    }

    @Override
    public void parseFromJson(String json) {

    }
}

