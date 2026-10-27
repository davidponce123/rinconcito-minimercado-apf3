package pe.utp.rinconcito.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import pe.utp.rinconcito.dao.InventoryDao;

@Controller
public class InventoryController {
    private final InventoryDao inventory;
    public InventoryController(InventoryDao inventory){this.inventory=inventory;}
    @GetMapping("/inventory") public String all(Model m){m.addAttribute("items",inventory.stock());return "inventory";}
}
