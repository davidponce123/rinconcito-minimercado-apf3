package pe.utp.rinconcito.service;

import org.apache.commons.lang3.StringUtils;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Set;

/** Alta de usuario y perfil en una transacción; el controlador gestiona errores fuera de ella. */
@Service
public class AccountService {
    private final JdbcTemplate jdbc;
    private final PasswordEncoder encoder;
    public AccountService(JdbcTemplate jdbc,PasswordEncoder encoder){this.jdbc=jdbc;this.encoder=encoder;}
    @Transactional
    public long create(String username,String password,String role){
        if(StringUtils.isBlank(username)||username.trim().length()>40||StringUtils.isBlank(password)||password.length()<12||!Set.of("ADMIN","CAJERO").contains(role))
            throw new IllegalArgumentException("Nombre, clave de 12 caracteres y rol son obligatorios");
        Long roleId=jdbc.queryForObject("SELECT id_rol FROM rol WHERE nombre=?",Long.class,role);
        Long id=jdbc.queryForObject("INSERT INTO usuario(nombre_usuario,password_hash,id_rol) VALUES (?,?,?) RETURNING id_usuario",Long.class,username.trim(),encoder.encode(password),roleId);
        if("ADMIN".equals(role))jdbc.update("INSERT INTO administrador(id_usuario,nivel_acceso) VALUES (?,'TOTAL')",id);
        else jdbc.update("INSERT INTO cajero(id_usuario,codigo_cajero) VALUES (?,?)",id,"CJ-"+id);
        return id;
    }
}
