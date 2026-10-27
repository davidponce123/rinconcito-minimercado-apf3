package pe.utp.rinconcito.controller;

import org.apache.commons.lang3.StringUtils;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** RF08 y RF09: gestión básica de cuentas y proveedores por ADMIN. */
@Controller
public class AdminController {
    private final JdbcTemplate jdbc;private final pe.utp.rinconcito.service.AccountService accounts;
    public AdminController(JdbcTemplate jdbc,pe.utp.rinconcito.service.AccountService accounts){this.jdbc=jdbc;this.accounts=accounts;}
    @GetMapping("/users") public String users(Model m){
        m.addAttribute("items",jdbc.queryForList("SELECT u.id_usuario,u.nombre_usuario,u.estado,r.nombre AS rol FROM usuario u JOIN rol r USING(id_rol) ORDER BY u.id_usuario"));return "users";
    }
    @PostMapping("/users") public String create(@RequestParam String username,@RequestParam String password,@RequestParam String role,RedirectAttributes flash){
        try{accounts.create(username,password,role);flash.addFlashAttribute("success","Cuenta creada con rol y perfil consistentes");}
        catch(IllegalArgumentException | DataIntegrityViolationException e){flash.addFlashAttribute("error","No se pudo crear la cuenta; revisa nombre, clave, rol y duplicados");}
        return "redirect:/users";
    }
    @PostMapping("/users/{id}/disable") public String disable(@PathVariable long id,org.springframework.security.core.Authentication auth,RedirectAttributes flash){
        Long actor=jdbc.queryForObject("SELECT id_usuario FROM usuario WHERE nombre_usuario=?",Long.class,auth.getName());
        if(actor!=null && actor==id)flash.addFlashAttribute("error","No puedes deshabilitar tu propia cuenta");
        else{jdbc.update("UPDATE usuario SET estado=false WHERE id_usuario=?",id);flash.addFlashAttribute("success","Cuenta deshabilitada");}
        return "redirect:/users";
    }
    @GetMapping("/suppliers") public String suppliers(Model m){m.addAttribute("items",jdbc.queryForList("SELECT id_proveedor,nombre,estado FROM proveedor ORDER BY nombre"));return "suppliers";}
    @PostMapping("/suppliers/{id}/toggle") public String supplierToggle(@PathVariable long id,RedirectAttributes flash){jdbc.update("UPDATE proveedor SET estado=NOT estado WHERE id_proveedor=?",id);flash.addFlashAttribute("success","Estado actualizado");return "redirect:/suppliers";}
}
