package polimi.saefa.restaurantservice.adapters.admin.web;

import lombok.*;
import polimi.saefa.restaurantservice.domain.MenuItem;

import java.util.*; 

@Data @NoArgsConstructor
public class EditRestaurantMenuForm {

	private List<MenuItem> menuItems;
	
	public EditRestaurantMenuForm(List<MenuItem> menuItems) {
		this.menuItems = menuItems; 
	}

}
