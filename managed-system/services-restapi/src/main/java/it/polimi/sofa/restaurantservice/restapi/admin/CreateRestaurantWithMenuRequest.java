package it.polimi.sofa.restaurantservice.restapi.admin;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import it.polimi.sofa.restaurantservice.restapi.common.MenuItemElement;

import java.util.*; 

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateRestaurantWithMenuRequest {

	private String name; 
	private String location;	
	
	private Collection<MenuItemElement> menuItems;

}

