package it.polimi.saefa.restclient;

import it.polimi.saefa.orderingservice.restapi.AddItemToCartResponse;
import it.polimi.saefa.orderingservice.restapi.CartItemElement;
import it.polimi.saefa.orderingservice.restapi.ConfirmOrderResponse;
import it.polimi.saefa.orderingservice.restapi.GetCartResponse;
import it.polimi.saefa.restaurantservice.restapi.common.*;
import it.polimi.saefa.restclient.domain.customer.CustomerWebService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Date;
import java.util.logging.Logger;

@Component
public class RequestsGenerator {
    private final Logger logger = Logger.getLogger(this.getClass().getName());

    @Autowired
    CustomerWebService customerWebService;

    public void simulateOrder() {
        Collection<GetRestaurantResponse> restaurants = customerWebService.getAllRestaurants();
        assert restaurants.size() >= 1;
        GetRestaurantResponse restaurant = restaurants.iterator().next();
        long restaurantId = restaurant.getId();
        GetRestaurantMenuResponse menu = customerWebService.getRestaurantMenu(restaurantId);
        assert menu.getMenuItems().size() >= 1;
        MenuItemElement menuItem = menu.getMenuItems().iterator().next();
        long cartId = customerWebService.createCart(restaurantId).getId();
        AddItemToCartResponse cart = customerWebService.addItemToCart(cartId, restaurantId, menuItem.getId(), 2);
        assert  cart.getItems().size() == 1;
        CartItemElement returnedItem = cart.getItems().iterator().next();
        assert  returnedItem.getQuantity() == 2 &&
                returnedItem.getId().equals(menuItem.getId()) &&
                returnedItem.getName().equals(menuItem.getName()) &&
                cart.getTotalPrice() == menuItem.getPrice() * 2;
        ConfirmOrderResponse confirmedOrder = customerWebService.confirmOrder(cartId, "1111111111111111", 12, 2023, "023",
                "Via REST Client", "Roma", 1, "12345", "1234567890", new Date());
        if (!confirmedOrder.isConfirmed()) {
            if (confirmedOrder.getRequiresCashPayment()) {
                logger.info("Order confirmed, but requires cash payment");
                confirmedOrder = customerWebService.confirmCashPayment(cartId, "Via REST Client", "Roma", 1, "12345", "1234567890", new Date());
            }
            if (!confirmedOrder.isConfirmed() && confirmedOrder.getIsTakeAway()) {
                logger.info("Order confirmed, but requires take away");
                confirmedOrder = customerWebService.handleTakeAway(cartId, true);
            }
        }
        assert confirmedOrder.isConfirmed();
        logger.info("Order confirmed!");
    }
}
