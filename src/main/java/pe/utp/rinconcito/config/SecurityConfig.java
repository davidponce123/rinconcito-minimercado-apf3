package pe.utp.rinconcito.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import java.util.List;

/** RF01: autenticación, autorización por rol, BCrypt y protección CSRF de formularios. */
@Configuration
public class SecurityConfig {
    @Bean public PasswordEncoder passwordEncoder(){return new BCryptPasswordEncoder(12);}
    @Bean public UserDetailsService userDetailsService(JdbcTemplate jdbc){
        return username->jdbc.query("SELECT u.nombre_usuario,u.password_hash,u.estado,r.nombre FROM usuario u JOIN rol r USING(id_rol) WHERE u.nombre_usuario=?",
                rs->{if(!rs.next())throw new org.springframework.security.core.userdetails.UsernameNotFoundException("Cuenta no encontrada");
                    return new User(rs.getString(1),rs.getString(2),rs.getBoolean(3),true,true,true,List.of(new SimpleGrantedAuthority("ROLE_"+rs.getString(4))));},username);
    }
    @Bean public SecurityFilterChain webSecurity(HttpSecurity http) throws Exception {
        return http.authorizeHttpRequests(a->a
                  .requestMatchers("/css/**","/js/**","/login","/error").permitAll()
                  .requestMatchers("/products/**","/categories/**","/receipts/**","/reports","/reports/**","/users/**","/suppliers/**","/slow-moving/**").hasRole("ADMIN")
                  .requestMatchers("/sales/**","/inventory/**","/dashboard").hasAnyRole("ADMIN","CAJERO")
                  .anyRequest().authenticated())
                .formLogin(f->f.loginPage("/login").defaultSuccessUrl("/dashboard",true).permitAll())
                .logout(l->l.logoutSuccessUrl("/login?logout").permitAll())
                .sessionManagement(s->s.sessionFixation(sf->sf.migrateSession()))
                .build();
    }
}
