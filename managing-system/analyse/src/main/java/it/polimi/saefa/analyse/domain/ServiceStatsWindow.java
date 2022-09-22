/*
package it.polimi.saefa.analyse.domain;

import lombok.Getter;
import lombok.Setter;

import java.util.LinkedList;
import java.util.List;

public class ServiceStatsWindow extends LinkedList<List<ServiceStats>> {
    @Getter @Setter
    private int capacity;

    public ServiceStatsWindow(int capacity) {
        this.capacity = capacity;
    }

    @Override
    public boolean add(List<ServiceStats> e) {
        if (size() >= capacity)
            removeFirst();
        return super.add(e);
    }

    public boolean isFull() {
        return size() == capacity;
    }

}
*/
