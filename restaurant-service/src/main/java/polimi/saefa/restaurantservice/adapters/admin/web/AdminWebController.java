package polimi.saefa.restaurantservice.adapters.admin.web;

import org.springframework.stereotype.Controller;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping; 
import org.springframework.ui.Model;
import polimi.saefa.restaurantservice.domain.*;

import javax.servlet.http.HttpServletRequest;

import java.util.*;
import java.util.logging.Logger;

@Controller
@RequestMapping(path="/admin/web")
public class AdminWebController {
	private final Logger logger = Logger.getLogger(AdminWebController.class.toString());

	@Autowired 
	private RestaurantService restaurantService;

	/* Mostra client home page */
	@GetMapping("")
	public String index() {
		return "admin/index";
	}
	
	/* Trova il ristorante con restaurantId. */ 
	@GetMapping("/restaurants/{restaurantId}")
	public String getRestaurant(Model model, @PathVariable Long restaurantId) {
		Restaurant restaurant = restaurantService.getRestaurant(restaurantId);
		model.addAttribute("restaurant", restaurant);
		return "admin/get-restaurant";
	}

	/* Trova il menu del ristorante con restaurantId. */ 
	@GetMapping("/restaurants/{restaurantId}/menu")
	public String getRestaurantMenu(Model model, @PathVariable Long restaurantId) {
		Restaurant restaurant = restaurantService.getRestaurant(restaurantId);
		RestaurantMenu menu = restaurantService.getRestaurantMenu(restaurantId);
		model.addAttribute("restaurant", restaurant);
		model.addAttribute("menu", menu);
		return "admin/get-restaurant-menu";
	}

	/* Trova tutti i ristoranti. */ 
	@GetMapping("/restaurants")
	public String getRestaurants(Model model) {
		Collection<Restaurant> restaurants = restaurantService.getAllRestaurants();
		model.addAttribute("restaurants", restaurants);
		return "admin/get-restaurants";
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
		Restaurant restaurant = restaurantService.createRestaurant(form.getName(), form.getLocation());
		model.addAttribute("restaurant", restaurant);
		return "admin/get-restaurant";
	}	

	/* Crea o modifica il menu di un ristorante (form). */ 
	@PostMapping(value="/restaurants/{restaurantId}/menu", params={"edit"})
	public String getEditRestaurantMenuForm(Model model, @PathVariable Long restaurantId) {
		Restaurant restaurant = restaurantService.getRestaurant(restaurantId);
		RestaurantMenu menu = restaurantService.getRestaurantMenu(restaurantId);
		List<MenuItem> menuItems = menu.getMenuItems();
		if (menuItems==null) {
			menuItems = new ArrayList<>();
			// menu.setMenuItems(menuItems); 
		}		
		model.addAttribute("restaurant", restaurant);
		model.addAttribute("form", new EditRestaurantMenuForm(menuItems));
		return "admin/edit-restaurant-menu-form";
	}

	/* Crea o modifica il menu di un ristorante. */ 
	@PostMapping("/restaurants/{restaurantId}/menu")
	public String addRestaurantMenu(Model model, @PathVariable Long restaurantId, @ModelAttribute("form") EditRestaurantMenuForm form) {
		List<MenuItem> menuItems = form.getMenuItems(); 
		if (menuItems==null) {
			menuItems = new ArrayList<>();
		}
		Restaurant restaurant = restaurantService.createOrUpdateRestaurantMenu(restaurantId, menuItems);
		model.addAttribute("restaurant", restaurant);
		return "admin/get-restaurant";
	}	


	@PostMapping(value="/restaurants/{restaurantId}/menu", params={"addMenuItem"})
	public String addMenuItem(Model model, @PathVariable Long restaurantId, @ModelAttribute("form") EditRestaurantMenuForm form) {
		List<MenuItem> menuItems = form.getMenuItems(); 
		if (menuItems==null) {
			menuItems = new ArrayList<>();
			form.setMenuItems(menuItems);
		}
		menuItems.add(new MenuItem());
		Restaurant restaurant = restaurantService.getRestaurant(restaurantId);
		model.addAttribute("restaurant", restaurant);
		model.addAttribute("form", form);
		return "admin/edit-restaurant-menu-form";
	}
	
	@PostMapping(value="/restaurants/{restaurantId}/menu", params={"removeMenuItem"})
	public String removeMenuItem(Model model, @PathVariable Long restaurantId, 
				@ModelAttribute("form") EditRestaurantMenuForm form, HttpServletRequest req) {
		int index = Integer.valueOf(req.getParameter("removeMenuItem")).intValue();
		form.getMenuItems().remove(index);
		Restaurant restaurant = restaurantService.getRestaurant(restaurantId);
		model.addAttribute("restaurant", restaurant);
		model.addAttribute("form", form);
		return "admin/edit-restaurant-menu-form";
	}

}
