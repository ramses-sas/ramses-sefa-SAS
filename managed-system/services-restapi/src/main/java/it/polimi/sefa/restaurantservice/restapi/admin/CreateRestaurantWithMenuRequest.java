package it.polimi.sefa.restaurantservice.restapi.admin;

import it.polimi.sefa.restaurantservice.restapi.common.MenuItemElement;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.*; 

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateRestaurantWithMenuRequest {

	private String name; 
	private String location;	
	
	private Collection<MenuItemElement> menuItems;

}

