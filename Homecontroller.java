package csc478project.restaurantapp;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/")
    public String index() {
        return "forward:/restaurant.html";
    }

    @GetMapping("/admin")
    public String admin() {
        return "forward:/admin.html";
    }

    @GetMapping("/chef")
    public String chef() {
        return "forward:/chef.html";
    }

    @GetMapping("/host")
    public String host() {
        return "forward:/host.html";
    }

    @GetMapping("/manager")
    public String manager() {
        return "forward:/manager.html";
    }

    @GetMapping("/menu-view")
    public String menu() {
        return "forward:/menu.html";
    }

    @GetMapping("/orders-view")
    public String orders() {
        return "forward:/orders.html";
    }

    @GetMapping("/owner")
    public String owner() {
        return "forward:/owner.html";
    }

    @GetMapping("/tables-view")
    public String tables() {
        return "forward:/tables.html";
    }

    @GetMapping("/waiter")
    public String waiter() {
        return "forward:/waiter.html";
    }
}
