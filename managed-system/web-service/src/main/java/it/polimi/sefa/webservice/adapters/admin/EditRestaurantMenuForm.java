package it.polimi.sefa.webservice.adapters.admin;

import lombok.*;
import it.polimi.sefa.restaurantservice.restapi.common.MenuItemElement;

import java.util.*; 

@Data @NoArgsConstructor
public class EditRestaurantMenuForm {

	private List<MenuItemElement> menuItems;
	
	public EditRestaurantMenuForm(List<MenuItemElement> menuItems) {
		this.menuItems = menuItems; 
	}

}
