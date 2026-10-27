package pe.utp.rinconcito.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice(annotations=Controller.class)
public class CommonModel {
    @ModelAttribute("isAdmin") public boolean isAdmin(Authentication a){return a!=null && a.getAuthorities().stream().anyMatch(v->v.getAuthority().equals("ROLE_ADMIN"));}
}
