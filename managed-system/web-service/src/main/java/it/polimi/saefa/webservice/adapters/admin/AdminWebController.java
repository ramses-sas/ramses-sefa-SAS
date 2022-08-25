package it.polimi.saefa.webservice.adapters.admin;

import org.springframework.stereotype.Controller;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping; 
import org.springframework.ui.Model;
import it.polimi.saefa.restaurantservice.restapi.admin.CreateRestaurantResponse;
import it.polimi.saefa.restaurantservice.restapi.common.GetRestaurantMenuResponse;
import it.polimi.saefa.restaurantservice.restapi.common.GetRestaurantResponse;
import it.polimi.saefa.restaurantservice.restapi.common.MenuItemElement;
import it.polimi.saefa.webservice.domain.admin.AdminWebService;

import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.logging.Logger;

@Controller
@RequestMapping(path="/admin")
public class AdminWebController {
	private final Logger logger = Logger.getLogger(AdminWebController.class.toString());
	@Autowired 
	private AdminWebService adminWebService;

	/* Mostra client home page */
	@GetMapping("")
	public String index() {
		return "admin/index";
	}

	/* Trova tutti i ristoranti. */
	@GetMapping("/restaurants")
	public String getRestaurants(Model model) {
		Collection<GetRestaurantResponse> restaurants = adminWebService.getAllRestaurants();
		model.addAttribute("restaurants", restaurants);
		return "admin/get-restaurants";
	}


	/* Trova il ristorante con restaurantId. */
	@GetMapping("/restaurants/{restaurantId}")
	public String getRestaurant(Model model, @PathVariable Long restaurantId) {
		GetRestaurantResponse restaurant = adminWebService.getRestaurant(restaurantId);
		model.addAttribute("restaurant", restaurant);
		return "admin/get-restaurant";
	}

	/* Trova il menu del ristorante con restaurantId. */
	@GetMapping("/restaurants/{restaurantId}/menu")
	public String getRestaurantMenu(Model model, @PathVariable Long restaurantId) {
		GetRestaurantResponse restaurant = adminWebService.getRestaurant(restaurantId);
		GetRestaurantMenuResponse menu = adminWebService.getRestaurantMenu(restaurantId);
		model.addAttribute("restaurant", restaurant);
		model.addAttribute("menu", menu);
		return "admin/get-restaurant-menu";
	}
	
	/* Crea un nuovo ristorante (form). */
	@GetMapping(value="/restaurants", params={"add"})
	public String getAddRestaurantForm(Model model) {
		model.addAttribute("form", new AddRestaurantForm());
		return "admin/add-restaurant-form";
	}
	
	/* Crea un nuovo ristorante. */
	@PostMapping("/restaurants")
	public String addRestaurant(Model model, @ModelAttribute("form") AddRestaurantForm form) {
		CreateRestaurantResponse restaurant = adminWebService.createRestaurant(form.getName(), form.getLocation());
		model.addAttribute("restaurant", restaurant);
		return "admin/get-restaurant";
	}

	/* Crea o modifica il menu di un ristorante (form). */
	@PostMapping(value="/restaurants/{restaurantId}/menu", params={"edit"})
	public String getEditRestaurantMenuForm(Model model, @PathVariable Long restaurantId) {
		GetRestaurantResponse restaurant = adminWebService.getRestaurant(restaurantId);
		GetRestaurantMenuResponse menu = adminWebService.getRestaurantMenu(restaurantId);
		Collection<MenuItemElement> menuItems = menu.getMenuItems();
		model.addAttribute("restaurant", restaurant);
		model.addAttribute("form", new EditRestaurantMenuForm(menuItems.stream().toList()));
		return "admin/edit-restaurant-menu-form";
	}

	/* Crea o modifica il menu di un ristorante. */
	@PostMapping("/restaurants/{restaurantId}/menu")
	public String addRestaurantMenu(Model model, @PathVariable Long restaurantId, @ModelAttribute("form") EditRestaurantMenuForm form) {
		List<MenuItemElement> menuItems = form.getMenuItems();
		if (menuItems==null) {
			menuItems = new ArrayList<>();
		}
		adminWebService.createOrUpdateRestaurantMenu(restaurantId, menuItems);
		GetRestaurantResponse restaurant = adminWebService.getRestaurant(restaurantId);
		model.addAttribute("restaurant", restaurant);
		return "admin/get-restaurant";
	}


	/* Aggiunge un nuovo menu item al menu di un ristorante (form). */
	@PostMapping(value="/restaurants/{restaurantId}/menu", params={"addMenuItem"})
	public String addMenuItem(Model model, @PathVariable Long restaurantId, @ModelAttribute("form") EditRestaurantMenuForm form) {
		List<MenuItemElement> menuItems = form.getMenuItems();
		if (menuItems==null) {
			menuItems = new ArrayList<>();
			form.setMenuItems(menuItems);
		}
		menuItems.add(new MenuItemElement());
		GetRestaurantResponse restaurant = adminWebService.getRestaurant(restaurantId);
		model.addAttribute("restaurant", restaurant);
		model.addAttribute("form", form);
		return "admin/edit-restaurant-menu-form";
	}

	/* Rimuove un menu item dal menu di un ristorante (form). */
	@PostMapping(value="/restaurants/{restaurantId}/menu", params={"removeMenuItem"})
	public String removeMenuItem(Model model, @PathVariable Long restaurantId, 
				@ModelAttribute("form") EditRestaurantMenuForm form, HttpServletRequest req) {
		int index = Integer.parseInt(req.getParameter("removeMenuItem"));
		form.getMenuItems().remove(index);
		GetRestaurantResponse restaurant = adminWebService.getRestaurant(restaurantId);
		model.addAttribute("restaurant", restaurant);
		model.addAttribute("form", form);
		return "admin/edit-restaurant-menu-form";
	}
}
