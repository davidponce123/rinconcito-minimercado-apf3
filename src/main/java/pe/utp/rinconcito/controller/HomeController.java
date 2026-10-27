package pe.utp.rinconcito.controller;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import pe.utp.rinconcito.dao.InventoryDao;

@Controller
public class HomeController {
    private final JdbcTemplate jdbc; private final InventoryDao stock;
    public HomeController(JdbcTemplate jdbc,InventoryDao stock){this.jdbc=jdbc;this.stock=stock;}
    @GetMapping("/") public String root(){return "redirect:/dashboard";}
    @GetMapping("/login") public String login(){return "login";}
    @GetMapping("/dashboard") public String dashboard(Model model){
        model.addAttribute("products",jdbc.queryForObject("SELECT COUNT(*) FROM producto WHERE estado=true",Integer.class));
        model.addAttribute("sales",jdbc.queryForObject("SELECT COUNT(*) FROM venta WHERE fecha_pago::date=CURRENT_DATE",Integer.class));
        model.addAttribute("low",stock.stock().stream().filter(s->s.needsReorder()).limit(8).toList());
        return "dashboard";
    }
}
