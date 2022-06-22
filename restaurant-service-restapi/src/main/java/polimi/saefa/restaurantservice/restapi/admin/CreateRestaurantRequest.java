package polimi.saefa.restaurantservice.restapi.admin;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateRestaurantRequest {

	private String name; 
	private String location;	

}

