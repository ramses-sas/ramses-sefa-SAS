package it.polimi.ramses.execute.externalInterfaces;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChangePropertyRequest {
    List<PropertyToChange> propertiesToChange;
}
