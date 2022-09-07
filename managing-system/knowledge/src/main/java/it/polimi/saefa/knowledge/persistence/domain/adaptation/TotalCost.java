package it.polimi.saefa.knowledge.persistence.domain.adaptation;

public class TotalCost extends AdaptationParameter {

    private double maxThreshold;

    @Override
    public boolean isSatisfied() {
        return super.getValue()<= maxThreshold;
    }

    public TotalCost(String json) {
        super(json);
    }

    @Override
    public void parseFromJson(String json) {

    }
}
