package polimi.saefa.webservice.adapters.admin;

import lombok.*;
import polimi.saefa.restaurantservice.restapi.common.MenuItemElement;

import java.util.*; 

@Data @NoArgsConstructor
public class EditRestaurantMenuForm {

	private List<MenuItemElement> menuItems;
	
	public EditRestaurantMenuForm(List<MenuItemElement> menuItems) {
		this.menuItems = menuItems; 
	}

}
