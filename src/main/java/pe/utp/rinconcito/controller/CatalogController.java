package pe.utp.rinconcito.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pe.utp.rinconcito.dao.ProductDao;
import pe.utp.rinconcito.service.CatalogService;
import java.math.BigDecimal;

@Controller
public class CatalogController {
    private final ProductDao products; private final CatalogService catalog;
    public CatalogController(ProductDao products,CatalogService catalog){this.products=products;this.catalog=catalog;}
    @GetMapping("/products") public String list(Model m){m.addAttribute("items",products.all());m.addAttribute("categories",products.categories());return "products";}
    @PostMapping("/products") public String create(@RequestParam String name,@RequestParam BigDecimal price,@RequestParam long categoryId,@RequestParam(defaultValue="0") long minimum,RedirectAttributes flash){
        try{long id=catalog.createProduct(name,price,categoryId,minimum);flash.addFlashAttribute("success","Producto registrado #"+id);}catch(IllegalArgumentException | org.springframework.dao.DataIntegrityViolationException e){flash.addFlashAttribute("error",e.getMessage());}
        return "redirect:/products";
    }
    @PostMapping("/products/{id}/update") public String update(@PathVariable long id,@RequestParam String name,@RequestParam BigDecimal price,RedirectAttributes flash){
        try{catalog.changeProduct(id,name,price);flash.addFlashAttribute("success","Producto actualizado");}catch(IllegalArgumentException | org.springframework.dao.DataIntegrityViolationException e){flash.addFlashAttribute("error",e.getMessage());}
        return "redirect:/products";
    }
    @PostMapping("/products/{id}/disable") public String disable(@PathVariable long id,RedirectAttributes flash){catalog.deactivate(id);flash.addFlashAttribute("success","Producto deshabilitado");return "redirect:/products";}
    @PostMapping("/categories") public String category(@RequestParam String name,RedirectAttributes flash){try{catalog.addCategory(name);flash.addFlashAttribute("success","Categoría registrada");}catch(IllegalArgumentException | org.springframework.dao.DataIntegrityViolationException e){flash.addFlashAttribute("error",e.getMessage());}return "redirect:/products";}
}
