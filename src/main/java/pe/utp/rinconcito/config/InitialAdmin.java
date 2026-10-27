package pe.utp.rinconcito.config;

import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

/** Alta segura del primer administrador mediante variables de entorno, sin claves fijas. */
@Configuration
public class InitialAdmin {
  @Bean
public ApplicationRunner bootstrap(
        JdbcTemplate jdbc,
        PasswordEncoder encoder,
        Environment env) {
        return args->{
            jdbc.update("INSERT INTO rol(nombre) VALUES ('ADMIN'),('CAJERO') ON CONFLICT (nombre) DO NOTHING");
           String login = env.getProperty("ADMIN_USER");
String password = env.getProperty("ADMIN_PASSWORD");
            if(login==null || password==null)return;
            if(login.isBlank() || password.length()<12)throw new IllegalStateException("Credencial inicial insegura: ADMIN_PASSWORD debe tener al menos 12 caracteres");
            jdbc.update("INSERT INTO usuario(nombre_usuario,password_hash,id_rol) SELECT ?,?,id_rol FROM rol WHERE nombre='ADMIN' ON CONFLICT(nombre_usuario) DO NOTHING",login,encoder.encode(password));
            jdbc.update("INSERT INTO administrador(id_usuario,nivel_acceso) SELECT id_usuario,'TOTAL' FROM usuario WHERE nombre_usuario=? ON CONFLICT DO NOTHING",login);
        };
    }
}
