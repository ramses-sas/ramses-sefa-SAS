package polimi.saefa.restaurantservice.restapi.admin;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.*; 

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GetRestaurantsResponse {

	private Collection<GetRestaurantResponse> restaurants;
	
}

