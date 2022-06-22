package polimi.saefa.restaurantservice.domain;

import javax.persistence.*; 

import lombok.*; 

import java.util.*;

@Embeddable
@Data @NoArgsConstructor
public class RestaurantMenu {

	@ElementCollection
	private List<MenuItem> menuItems;

	public RestaurantMenu(List<MenuItem> menuItems) {
		this(); 
		this.menuItems = menuItems; 
	}
	
}
