package csc478project.restaurantapp;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {
    @GetMapping("/")
    public String home() {
        return "redirect:/restaurant.html";
    }

    @GetMapping({"/menu", "/owner", "/manager", "/admin", "/chef", "/waiter", "/host"})
    public String pageAlias(HttpServletRequest request) {
        return "redirect:" + request.getServletPath() + ".html";
    }
}
