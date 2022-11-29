package it.polimi.sefa.orderingservice.externalInterfaces;

import it.polimi.sefa.orderingservice.config.LoadBalancerConfig;
import org.springframework.cloud.loadbalancer.annotation.LoadBalancerClient;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import it.polimi.sefa.restaurantservice.restapi.common.GetMenuItemDetailsResponse;
import it.polimi.sefa.restaurantservice.restapi.common.NotifyRestaurantRequest;
import it.polimi.sefa.restaurantservice.restapi.common.NotifyRestaurantResponse;

@FeignClient(name = "RESTAURANT-SERVICE")
@LoadBalancerClient(name = "RESTAURANT-SERVICE", configuration = LoadBalancerConfig.class)
public interface RestaurantServiceClient {

    @GetMapping("/rest/customer/restaurants/{restaurantId}/item/{itemId}")
    GetMenuItemDetailsResponse getMenuItemDetails(@PathVariable Long restaurantId, @PathVariable String itemId);

    @PostMapping("/rest/customer/restaurants/{restaurantId}/notify")
    public NotifyRestaurantResponse notifyRestaurant(@PathVariable Long restaurantId, @RequestBody NotifyRestaurantRequest request);


}

