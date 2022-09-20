package it.polimi.saefa.analyse.domain;

import java.util.LinkedList;
import java.util.List;

public class ServiceStatsWindow extends LinkedList<List<ServiceStats>> {
    private final int capacity;

    public ServiceStatsWindow(int capacity) {
        this.capacity = capacity;
    }

    @Override
    public boolean add(List<ServiceStats> e) {
        if (size() >= capacity)
            removeFirst();
        return super.add(e);
    }

}
