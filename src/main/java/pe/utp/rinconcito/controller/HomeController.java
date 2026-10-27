
package pe.utp.rinconcito.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import pe.utp.rinconcito.service.DashboardService;

@Controller
public class HomeController {

    private final DashboardService dashboardService;

    public HomeController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/")
    public String root() {
        return "redirect:/dashboard";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {

        model.addAttribute(
            "products",
            dashboardService.activeProducts()
        );

        model.addAttribute(
            "sales",
            dashboardService.todaySales()
        );

        model.addAttribute(
            "low",
            dashboardService.lowStock()
        );

        return "dashboard";
    }
}
