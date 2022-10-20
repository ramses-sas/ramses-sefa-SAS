
package it.polimi.saefa.analyse.domain;

import it.polimi.saefa.knowledge.domain.adaptation.options.AdaptationOption;
import lombok.Getter;
import lombok.Setter;

import java.util.LinkedList;
import java.util.List;

public class AnalysisWindow extends LinkedList<List<AdaptationOption>> {
    @Getter @Setter
    private int capacity;

    public AnalysisWindow(int capacity) {
        this.capacity = capacity;
    }

    @Override
    public boolean add(List<AdaptationOption> e) {
        if (size() >= capacity)
            removeFirst();
        return super.add(e);
    }

    public boolean isFull() {
        return size() == capacity;
    }

}
