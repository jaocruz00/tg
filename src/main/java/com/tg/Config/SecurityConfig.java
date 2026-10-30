package com.tg.Config;
import com.tg.Repositories.SessaoRepository;
import com.tg.Service.AutenticacaoService;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import org.springframework.context.annotation.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.*;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;
@Configuration
public class SecurityConfig {
 @Bean org.springframework.security.core.userdetails.UserDetailsService userDetailsService() {
   return name -> { throw new org.springframework.security.core.userdetails.UsernameNotFoundException(name); };
 }
 @Bean PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(12); }
 @Bean SecurityFilterChain security(HttpSecurity http, SessaoRepository sessoes) throws Exception {
   var bearer=new OncePerRequestFilter() {
     @Override protected void doFilterInternal(HttpServletRequest req,HttpServletResponse res,FilterChain chain) throws ServletException,IOException {
       String h=req.getHeader("Authorization");
       if(h!=null && h.startsWith("Bearer ") && h.length()<=256) {
         var s=sessoes.findByTokenHash(AutenticacaoService.hash(h.substring(7)));
         if(s.isPresent() && s.get().getExpiraEm().isAfter(Instant.now()) && s.get().getUsuario().isAtivo()) {
           var u=s.get().getUsuario();
           var context=SecurityContextHolder.createEmptyContext();
           context.setAuthentication(new UsernamePasswordAuthenticationToken(u.getEmail(),null,List.of(new SimpleGrantedAuthority("ROLE_"+u.getPerfil()))));
           SecurityContextHolder.setContext(context);
         }
       }
       chain.doFilter(req,res);
     }
   };
   return http.csrf(c -> c.disable())
     .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
     .authorizeHttpRequests(a -> a.requestMatchers(org.springframework.http.HttpMethod.POST,"/api/auth/login").permitAll()
       .requestMatchers("/api/admin/**").hasRole("ADMIN")
       .requestMatchers("/api/**").hasAnyRole("ADMIN","ADVOGADO").anyRequest().denyAll())
     .exceptionHandling(e -> e.authenticationEntryPoint((q,s,x)->s.sendError(401)).accessDeniedHandler((q,s,x)->s.sendError(403)))
     .addFilterBefore(bearer,UsernamePasswordAuthenticationFilter.class).build();
 }
}
