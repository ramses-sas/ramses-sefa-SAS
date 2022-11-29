package it.polimi.sefa.restaurantservice.restapi.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GetMenuItemDetailsResponse {
    private String id;
    private String name;
    private double price;
}
