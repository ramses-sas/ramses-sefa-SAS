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
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Collection;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ScheduledFuture;

@Slf4j
@Component
@Data
public class RequestsGenerator {
    @Value("${SCHEDULING_PERIOD}")
    private long schedulingPeriod;
    @Value("${ADAPT}")
    private int adapt;
    @Value("${TRIAL_DURATION_MINUTES}")
    private long trialDurationMinutes;

    @Autowired
    private RequestGeneratorService requestGeneratorService;
    @Autowired
    private AdaptationController adaptationController;
    private ThreadPoolTaskScheduler taskScheduler;
    private ScheduledFuture<?> simulationRoutine;

    public RequestsGenerator(ThreadPoolTaskScheduler taskScheduler) {
        this.taskScheduler = taskScheduler;
    }

    @PostConstruct
    public void init() {
        startSimulation();

        // Start Monitor and enable adaptation if ADAPT env var is != 0 after 10 seconds
        TimerTask startManagingTask = new TimerTask() {
            public void run() {
                log.info("Starting Monitor Routine");
                adaptationController.startMonitorRoutine();
                if (adapt != 0) {
                    log.info("Enabling adaptation");
                    adaptationController.changeAdaptationStatus(true);
                } else {
                    log.info("Disabling adaptation");
                    adaptationController.changeAdaptationStatus(false);
                }
            }
        };
        Timer startManagingTimer = new Timer("StartManagingTimer");
        startManagingTimer.schedule(startManagingTask, 1000*10);

        // Stop simulation after TRIAL_DURATION_MINUTES minutes
        TimerTask stopSimulationTask = new TimerTask() {
            public void run() {
                log.info("Stopping simulation");
                stopSimulation();
                log.info("Stopping Monitor Routine");
                adaptationController.stopMonitorRoutine();
                log.info("Disabling adaptation");
                adaptationController.changeAdaptationStatus(false);
                System.exit(0);
            }
        };
        Timer stopSimulationTimer = new Timer("StopSimulationTimer");
        stopSimulationTimer.schedule(stopSimulationTask, 1000*60*trialDurationMinutes);
    }

    class SimulationRoutine implements Runnable {
        @Override
        public void run() {
            try {
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
                        log.info("Order confirmed, but requires cash payment");
                        confirmedOrder = requestGeneratorService.confirmCashPayment(cartId, "Via REST Client", "Roma", 1, "12345", "1234567890", new Date());
                    }
                    if (confirmedOrder == null) throw new RuntimeException("Impossible to confirm order [2]");
                    if (!confirmedOrder.isConfirmed() && confirmedOrder.getIsTakeAway()) {
                        log.info("Order confirmed, but requires take away");
                        confirmedOrder = requestGeneratorService.handleTakeAway(cartId, true);
                    }
                    if (confirmedOrder == null) throw new RuntimeException("Impossible to confirm order [3]");
                }
                if (!confirmedOrder.isConfirmed()) throw new RuntimeException("Order not confirmed");
                log.info("Order confirmed!");
            } catch (Exception e) {
                //log.error(e.getMessage());
            }
        }
    }

    public void startSimulation() {
        if (simulationRoutine == null || simulationRoutine.isCancelled()) {
            log.info("simulationRoutine starting");
            simulationRoutine = taskScheduler.scheduleAtFixedRate(new SimulationRoutine(), schedulingPeriod);
        } else {
            log.info("simulationRoutine already running");
        }
    }

    public void stopSimulation() {
        if (simulationRoutine.cancel(false)) {
            log.info("simulationRoutine stopping");
            simulationRoutine = null;
        } else {
            log.error("Error stopping simulationRoutine");
        }
    }

}
