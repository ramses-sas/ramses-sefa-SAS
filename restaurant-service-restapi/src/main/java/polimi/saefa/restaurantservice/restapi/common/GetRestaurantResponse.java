package polimi.saefa.restaurantservice.restapi.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GetRestaurantResponse {

	private Long id;
	
	private String name; 
	private String location;	
}

