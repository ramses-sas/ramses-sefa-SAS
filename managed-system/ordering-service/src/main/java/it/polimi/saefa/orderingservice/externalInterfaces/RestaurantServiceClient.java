package it.polimi.saefa.orderingservice.externalInterfaces;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import it.polimi.saefa.restaurantservice.restapi.common.GetMenuItemDetailsResponse;
import it.polimi.saefa.restaurantservice.restapi.common.NotifyRestaurantResponse;

@FeignClient(name = "restaurant-service")
public interface RestaurantServiceClient {

    @GetMapping("/rest/customer/restaurants/{restaurantId}/item/{itemId}")
    GetMenuItemDetailsResponse getMenuItemDetails(@PathVariable Long restaurantId, @PathVariable String itemId);

    @GetMapping("/rest/customer/restaurants/{restaurantId}/notify/{orderNumber}")
    NotifyRestaurantResponse notifyRestaurant(@PathVariable Long restaurantId, @PathVariable Long orderNumber);

}

