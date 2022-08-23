package it.polimi.saefa.orderingservice.externalInterfaces;

import it.polimi.saefa.orderingservice.config.LoadBalancerConfig;
import org.springframework.cloud.loadbalancer.annotation.LoadBalancerClient;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import it.polimi.saefa.restaurantservice.restapi.common.GetMenuItemDetailsResponse;
import it.polimi.saefa.restaurantservice.restapi.common.NotifyRestaurantResponse;

@FeignClient(name = "RESTAURANT-SERVICE")
@LoadBalancerClient(name = "RESTAURANT-SERVICE", configuration = LoadBalancerConfig.class)
public interface RestaurantServiceClient {

    @GetMapping("/rest/customer/restaurants/{restaurantId}/item/{itemId}")
    GetMenuItemDetailsResponse getMenuItemDetails(@PathVariable Long restaurantId, @PathVariable String itemId);

    @GetMapping("/rest/customer/restaurants/{restaurantId}/notify/{orderNumber}")
    NotifyRestaurantResponse notifyRestaurant(@PathVariable Long restaurantId, @PathVariable Long orderNumber);

}

