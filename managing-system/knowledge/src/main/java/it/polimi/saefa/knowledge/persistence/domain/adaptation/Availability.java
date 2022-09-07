package it.polimi.saefa.knowledge.persistence.domain.adaptation;

public class Availability extends AdaptationParameter{
    private double minThreshold;

    @Override
    public boolean isSatisfied() {
        return super.getValue()>=minThreshold;
    }

    public Availability(String json) {
        super(json);
    }

    @Override
    public void parseFromJson(String json) {

    }
}
