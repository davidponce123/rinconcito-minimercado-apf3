package pe.utp.rinconcito.controller;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pe.utp.rinconcito.dao.ProductDao;
import pe.utp.rinconcito.domain.SaleLine;
import pe.utp.rinconcito.domain.ReceiptLine;
import pe.utp.rinconcito.service.SalesService;
import pe.utp.rinconcito.service.ReceivingService;
import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Controladores MVC; delegan reglas y transacciones a los servicios. */
@Controller
public class OperationsController {
    private final ProductDao products; private final JdbcTemplate jdbc;
    private final SalesService sales; private final ReceivingService receiving;
    public OperationsController(ProductDao products,JdbcTemplate jdbc,SalesService sales,ReceivingService receiving){this.products=products;this.jdbc=jdbc;this.sales=sales;this.receiving=receiving;}
    @GetMapping("/sales") public String sales(Model model){
        model.addAttribute("items",jdbc.queryForList("SELECT id_venta,fecha_pago,tipo_pago,total FROM v_venta_total WHERE fecha_pago IS NOT NULL ORDER BY id_venta DESC LIMIT 100"));
        return "sales";
    }
    @GetMapping("/sales/new") public String salesForm(Model m){m.addAttribute("products",products.all().stream().filter(p->p.active()).toList());return "sale-form";}
    @PostMapping("/sales") public String sell(@RequestParam List<Long> productId,@RequestParam List<Integer> quantity,@RequestParam String payment,Principal p,RedirectAttributes flash){
        try{
            if(productId.size()!=quantity.size())throw new IllegalArgumentException("Líneas de venta incompletas");
            List<SaleLine> lines=new ArrayList<>();
            for(int i=0;i<productId.size();i++)if(productId.get(i)!=null && quantity.get(i)!=null)lines.add(new SaleLine(productId.get(i),quantity.get(i)));
            long id=sales.confirm(lines,payment,p.getName());flash.addFlashAttribute("success","Venta #"+id+" confirmada, inventario actualizado");
            return "redirect:/sales";
        }catch(IllegalArgumentException | ArithmeticException | org.springframework.dao.DataIntegrityViolationException e){flash.addFlashAttribute("error",e.getMessage());return "redirect:/sales/new";}
    }
    @GetMapping("/receipts") public String receipts(Model m){m.addAttribute("items",jdbc.queryForList("SELECT i.id_ingreso,i.guia,p.nombre AS proveedor,i.fecha,i.estado FROM ingreso_mercaderia i JOIN proveedor p USING(id_proveedor) ORDER BY id_ingreso DESC LIMIT 100"));return "receipts";}
    @GetMapping("/receipts/new") public String receiptForm(Model m){
        m.addAttribute("products",products.all().stream().filter(p->p.active()).toList());
        m.addAttribute("suppliers",jdbc.queryForList("SELECT id_proveedor,nombre FROM proveedor WHERE estado=true ORDER BY nombre"));return "receipt-form";
    }
    @PostMapping("/receipts") public String receive(@RequestParam long supplierId,@RequestParam String guide,@RequestParam List<Long> productId,@RequestParam List<Integer> quantity,@RequestParam List<BigDecimal> cost,@RequestParam(required=false) List<String> expiry,Principal p,RedirectAttributes flash){
        try{
            if(productId.size()!=quantity.size()||productId.size()!=cost.size())throw new IllegalArgumentException("Líneas de recepción incompletas");
            if(expiry!=null && !expiry.isEmpty() && expiry.size()!=productId.size())throw new IllegalArgumentException("Los vencimientos deben corresponder a cada lote");
            List<ReceiptLine> lines=new ArrayList<>();
            for(int i=0;i<productId.size();i++){
                if(productId.get(i)==null || quantity.get(i)==null || cost.get(i)==null)throw new IllegalArgumentException("Completa producto, cantidad y costo de cada lote");
                String raw=expiry!=null && i<expiry.size()?expiry.get(i):"";
                lines.add(new ReceiptLine(productId.get(i),quantity.get(i),cost.get(i),raw==null||raw.isBlank()?null:LocalDate.parse(raw)));
            }
            long id=receiving.register(supplierId,guide,lines,p.getName());flash.addFlashAttribute("success","Recepción #"+id+" registrada, existencias actualizadas");return "redirect:/receipts";
        }catch(org.springframework.dao.DuplicateKeyException e){
            flash.addFlashAttribute("error","La guía ya está registrada para este proveedor");return "redirect:/receipts/new";
        }catch(org.springframework.dao.DataIntegrityViolationException e){
            flash.addFlashAttribute("error","No se pudo registrar la recepción; revisa los datos de los lotes");return "redirect:/receipts/new";
        }catch(IllegalArgumentException | java.time.format.DateTimeParseException | ArithmeticException e){flash.addFlashAttribute("error",e.getMessage());return "redirect:/receipts/new";}
    }
}
