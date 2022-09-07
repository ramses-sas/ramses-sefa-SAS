package it.polimi.saefa.knowledge.persistence.domain.architecture;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class ServiceImplementation {
    private Service service;
    private String implementationId;
    private double costPerInstance;
    private double costPerRequest; // tipo scatto alla risposta
    private double costPerSecond; //cost per second a richiesta (equivale a una sorta di costo per processing time)
    private double costPerBoot; //costo per avvio di un'istanza
    private double score; //valutazione di quanto è preferibile questa implementazione rispetto ad altre

    public ServiceImplementation(String implementationId, double costPerInstance, double costPerRequest, double costPerSecond, double costPerBoot, double score) {
        this.implementationId = implementationId;
        this.costPerInstance = costPerInstance;
        this.costPerRequest = costPerRequest;
        this.costPerSecond = costPerSecond;
        this.costPerBoot = costPerBoot;
        this.score = score;
    }
}
