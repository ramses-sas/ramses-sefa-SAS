package polimi.saefa.orderingservice.externalInterfaces;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import polimi.saefa.restaurantservice.restapi.common.GetMenuItemDetailsResponse;
import polimi.saefa.restaurantservice.restapi.common.NotifyRestaurantRequest;
import polimi.saefa.restaurantservice.restapi.common.NotifyRestaurantResponse;

@FeignClient(name = "restaurant-service")
public interface RestaurantServiceClient {

    @GetMapping("/rest/customer/restaurants/{restaurantId}/item/{itemId}")
    GetMenuItemDetailsResponse getMenuItemDetails(@PathVariable Long restaurantId, @PathVariable String itemId);

    @PostMapping("/rest/customer/restaurants/{restaurantId}/notify")
    public NotifyRestaurantResponse notifyRestaurant(@PathVariable Long restaurantId, @RequestBody NotifyRestaurantRequest request);


}

