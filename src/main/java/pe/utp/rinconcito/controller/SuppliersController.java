package pe.utp.rinconcito.controller;

import org.apache.commons.lang3.StringUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Alta básica de proveedores usada en recepción; gestión completa RF09 queda pendiente. */
@Controller
public class SuppliersController {
    private final JdbcTemplate jdbc;
    public SuppliersController(JdbcTemplate jdbc){this.jdbc=jdbc;}
    @PostMapping("/receipts/suppliers") public String create(@RequestParam String name,RedirectAttributes flash){
        if(StringUtils.isBlank(name) || name.trim().length()>80)flash.addFlashAttribute("error","Nombre de proveedor inválido");
        else{jdbc.update("INSERT INTO proveedor(nombre) VALUES (?)",name.trim());flash.addFlashAttribute("success","Proveedor registrado");}
        return "redirect:/receipts/new";
    }
}
