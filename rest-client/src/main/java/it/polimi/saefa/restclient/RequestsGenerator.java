package it.polimi.saefa.restclient;

import it.polimi.saefa.orderingservice.restapi.AddItemToCartResponse;
import it.polimi.saefa.orderingservice.restapi.CartItemElement;
import it.polimi.saefa.orderingservice.restapi.ConfirmOrderResponse;
import it.polimi.saefa.restaurantservice.restapi.common.*;
import it.polimi.saefa.restclient.domain.RequestGeneratorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Date;

@Slf4j
@Component
public class RequestsGenerator {

    @Autowired
    RequestGeneratorService requestGeneratorService;

    public void simulateOrder() {
        Collection<GetRestaurantResponse> restaurants = requestGeneratorService.getAllRestaurants();
        assert restaurants.size() >= 1;
        GetRestaurantResponse restaurant = restaurants.iterator().next();
        long restaurantId = restaurant.getId();
        GetRestaurantMenuResponse menu = requestGeneratorService.getRestaurantMenu(restaurantId);
        assert menu.getMenuItems().size() >= 1;
        MenuItemElement menuItem = menu.getMenuItems().iterator().next();
        long cartId = requestGeneratorService.createCart(restaurantId).getId();
        AddItemToCartResponse cart = requestGeneratorService.addItemToCart(cartId, restaurantId, menuItem.getId(), 2);
        assert  cart.getItems().size() == 1;
        CartItemElement returnedItem = cart.getItems().iterator().next();
        assert  returnedItem.getQuantity() == 2 &&
                returnedItem.getId().equals(menuItem.getId()) &&
                returnedItem.getName().equals(menuItem.getName()) &&
                cart.getTotalPrice() == menuItem.getPrice() * 2;
        ConfirmOrderResponse confirmedOrder = requestGeneratorService.confirmOrder(cartId, "1111111111111111", 12, 2023, "001",
                "Via REST Client", "Roma", 1, "12345", "1234567890", new Date());
        if (!confirmedOrder.isConfirmed()) {
            if (confirmedOrder.getRequiresCashPayment()) {
                log.info("Order confirmed, but requires cash payment");
                confirmedOrder = requestGeneratorService.confirmCashPayment(cartId, "Via REST Client", "Roma", 1, "12345", "1234567890", new Date());
            }
            if (!confirmedOrder.isConfirmed() && confirmedOrder.getIsTakeAway()) {
                log.info("Order confirmed, but requires take away");
                confirmedOrder = requestGeneratorService.handleTakeAway(cartId, true);
            }
        }
        assert confirmedOrder.isConfirmed();
        log.info("Order confirmed!");
    }
}
