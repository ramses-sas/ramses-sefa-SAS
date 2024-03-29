package it.polimi.sefa.restaurantservice.restapi.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.*; 

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GetRestaurantWithMenuResponse {

	private Long restaurantId;
	
	private String name; 
	private String location;	
	
	private Collection<MenuItemElement> menuItems;
}

