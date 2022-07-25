package polimi.saefa.orderingservice.domain;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import polimi.saefa.restaurantservice.restapi.common.GetMenuItemPriceResponse;
import polimi.saefa.restaurantservice.restapi.common.NotifyRestaurantResponse;

@FeignClient(name = "restaurant-service")
public interface RestaurantServiceClient {

    @GetMapping("/rest/customer/restaurants/{restaurantId}/item/{itemId}")
    GetMenuItemPriceResponse getMenuItemPrice(@PathVariable Long restaurantId, @PathVariable String itemId);

    @GetMapping("/rest/customer/restaurants/{restaurantId}/notify/{orderNumber}")
    NotifyRestaurantResponse notifyRestaurant(@PathVariable Long restaurantId, @PathVariable Long orderNumber);

}

