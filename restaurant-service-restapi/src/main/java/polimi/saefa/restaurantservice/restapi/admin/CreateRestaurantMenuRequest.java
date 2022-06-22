package polimi.saefa.restaurantservice.restapi.admin;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.*; 

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateRestaurantMenuRequest {

	private Long restaurantId;	
	private Collection<MenuItemElement> menuItems;

}

