package it.polimi.saefa.restclient;

import it.polimi.saefa.orderingservice.restapi.AddItemToCartResponse;
import it.polimi.saefa.orderingservice.restapi.CartItemElement;
import it.polimi.saefa.orderingservice.restapi.ConfirmOrderResponse;
import it.polimi.saefa.orderingservice.restapi.CreateCartResponse;
import it.polimi.saefa.restaurantservice.restapi.common.*;
import it.polimi.saefa.restclient.domain.AdaptationController;
import it.polimi.saefa.restclient.domain.RequestGeneratorService;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Collection;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

@Slf4j
@Component
@Data
@EnableAsync
public class RequestsGenerator {
    @Value("${ADAPT}")
    private int adapt;
    @Value("${TRIAL_DURATION_MINUTES}")
    private long trialDurationMinutes;
    @Value("${spring.task.execution.pool.core-size}")
    private int poolSize;

    @Autowired
    private RequestGeneratorService requestGeneratorService;
    @Autowired
    private AdaptationController adaptationController;

    @PostConstruct
    public void init() {
        log.info("Adapt? {}", adapt != 0);
        log.info("Trial duration: {} minutes", trialDurationMinutes);
        log.info("Thread pool size: {}", poolSize);

        // After 10 seconds start Monitor. Then enable adaptation if ADAPT env var is != 0
        TimerTask startManagingTask = new TimerTask() {
            public void run() {
                log.info("Starting Monitor Routine");
                try {
                    adaptationController.startMonitorRoutine();
                    if (adapt != 0) {
                        log.info("Enabling adaptation");
                        adaptationController.changeAdaptationStatus(true);
                    } else {
                        log.info("Disabling adaptation");
                        adaptationController.changeAdaptationStatus(false);
                    }
                } catch (Exception e) {
                    log.error("Error while starting Monitor Routine", e);
                    System.exit(1);
                }
            }
        };
        Timer startManagingTimer = new Timer("StartManagingTimer");
        startManagingTimer.schedule(startManagingTask, 1000*10);

        // Stop simulation after TRIAL_DURATION_MINUTES minutes
        TimerTask stopSimulationTask = new TimerTask() {
            public void run() {
                try {
                    log.info("Stopping Monitor Routine");
                    adaptationController.stopMonitorRoutine();
                    log.info("Disabling adaptation");
                    adaptationController.changeAdaptationStatus(false);
                } catch (Exception e) {
                    log.error("Error while stopping simulation", e);
                    System.exit(1);
                }
                System.exit(0);
            }
        };
        Timer stopSimulationTimer = new Timer("StopSimulationTimer");
        stopSimulationTimer.schedule(stopSimulationTask, 1000*60*trialDurationMinutes);
    }


    @Async
    @Scheduled(fixedDelay = 10)
    public void scheduleFixedRateTaskAsync() {
        try {
            Thread.sleep((long) (Math.random() * 500));
            log.debug("Starting simulation routine");
            Collection<GetRestaurantResponse> restaurants = requestGeneratorService.getAllRestaurants();
            if (restaurants == null || restaurants.size() < 1)
                throw new RuntimeException("No restaurants available");
            GetRestaurantResponse restaurant = restaurants.iterator().next();
            long restaurantId = restaurant.getId();
            GetRestaurantMenuResponse menu = requestGeneratorService.getRestaurantMenu(restaurantId);
            if (menu == null || menu.getMenuItems().size() < 1)
                throw new RuntimeException("No menu items available");
            MenuItemElement menuItem = menu.getMenuItems().iterator().next();
            CreateCartResponse cartCreated = requestGeneratorService.createCart(restaurantId);
            if (cartCreated == null) throw new RuntimeException("Cart creation failed");
            long cartId = cartCreated.getId();
            AddItemToCartResponse cart = requestGeneratorService.addItemToCart(cartId, restaurantId, menuItem.getId(), 2);
            if (cart == null || cart.getItems().size() != 1)
                throw new RuntimeException("Wrong number of items in cart");
            CartItemElement returnedItem = cart.getItems().iterator().next();
            if (returnedItem.getQuantity() != 2 ||
                    !returnedItem.getId().equals(menuItem.getId()) ||
                    !returnedItem.getName().equals(menuItem.getName()) ||
                    cart.getTotalPrice() != menuItem.getPrice() * 2)
                throw new RuntimeException("Inconsistent cart");
            ConfirmOrderResponse confirmedOrder = requestGeneratorService.confirmOrder(cartId, "1111111111111111", 12, 2023, "001",
                    "Via REST Client", "Roma", 1, "12345", "1234567890", new Date());
            if (confirmedOrder == null) throw new RuntimeException("Impossible to confirm order [1]");
            if (!confirmedOrder.isConfirmed()) {
                if (confirmedOrder.getRequiresCashPayment()) {
                    log.debug("Order confirmed, but requires cash payment");
                    confirmedOrder = requestGeneratorService.confirmCashPayment(cartId, "Via REST Client", "Roma", 1, "12345", "1234567890", new Date());
                }
                if (confirmedOrder == null) throw new RuntimeException("Impossible to confirm order [2]");
                if (!confirmedOrder.isConfirmed() && confirmedOrder.getIsTakeAway()) {
                    log.debug("Order confirmed, but requires take away");
                    confirmedOrder = requestGeneratorService.handleTakeAway(cartId, true);
                }
                if (confirmedOrder == null) throw new RuntimeException("Impossible to confirm order [3]");
            }
            if (!confirmedOrder.isConfirmed()) throw new RuntimeException("Order not confirmed");
            log.debug("Order confirmed!");
        } catch (Exception e) {
            //log.error(e.getMessage());
        }
    }
}
