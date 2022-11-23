package it.polimi.sofa.webservice.adapters.admin;

import lombok.*;
import it.polimi.sofa.restaurantservice.restapi.common.MenuItemElement;

import java.util.*; 

@Data @NoArgsConstructor
public class EditRestaurantMenuForm {

	private List<MenuItemElement> menuItems;
	
	public EditRestaurantMenuForm(List<MenuItemElement> menuItems) {
		this.menuItems = menuItems; 
	}

}
