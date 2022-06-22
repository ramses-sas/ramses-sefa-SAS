package polimi.saefa.restaurantservice.restapi.admin;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GetRestaurantResponse {

	private Long restaurantId;
	
	private String name; 
	private String location;	
}

