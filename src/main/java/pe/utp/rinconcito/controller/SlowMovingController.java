package pe.utp.rinconcito.controller;

import org.apache.commons.lang3.StringUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.security.Principal;

/** RF10: rotación basada en última salida o, si no existe, primera entrada registrada. */
@Controller
public class SlowMovingController {
    private final JdbcTemplate jdbc;
    public SlowMovingController(JdbcTemplate jdbc){this.jdbc=jdbc;}
    @GetMapping("/slow-moving") public String list(Model m){
        m.addAttribute("items",jdbc.queryForList("""
          SELECT p.id_producto,p.nombre,s.stock_actual,
                 (CURRENT_DATE-COALESCE(MAX(CASE WHEN x.tipo='SALIDA' THEN x.fecha::date END),
                                           MIN(CASE WHEN x.tipo='ENTRADA' THEN x.fecha::date END))) AS dias,
                 ROUND(s.stock_actual*p.precio,2) AS valor_estimado
          FROM producto p JOIN v_stock s USING(id_producto)
          JOIN movimiento_inventario x USING(id_producto)
          WHERE p.estado=true AND s.stock_actual>0
          GROUP BY p.id_producto,p.nombre,s.stock_actual,p.precio
          HAVING (CURRENT_DATE-COALESCE(MAX(CASE WHEN x.tipo='SALIDA' THEN x.fecha::date END),
                                          MIN(CASE WHEN x.tipo='ENTRADA' THEN x.fecha::date END)))>30
          ORDER BY dias DESC,p.nombre
          """));return "slow-moving";
    }
    @PostMapping("/slow-moving/{id}/decision") public String decision(@PathVariable long id,@RequestParam String decision,Principal p,RedirectAttributes flash){
        if(StringUtils.isBlank(decision)||decision.length()>40){flash.addFlashAttribute("error","Acción inválida");return "redirect:/slow-moving";}
        jdbc.update("INSERT INTO accion_baja_rotacion(id_producto,decision,id_usuario) SELECT ?,?,id_usuario FROM usuario WHERE nombre_usuario=?",id,decision.trim(),p.getName());
        flash.addFlashAttribute("success","Acción registrada en el historial");return "redirect:/slow-moving";
    }
}
