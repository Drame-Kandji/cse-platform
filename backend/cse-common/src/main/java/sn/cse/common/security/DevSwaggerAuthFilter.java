package sn.cse.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Filtre d'authentification de développement pour faciliter les tests directs via Swagger UI.
 * Actif uniquement si {@code cse.security.dev-bypass=true} (par défaut en dev) et en l'absence d'en-tête Bearer JWT.
 */
@Component
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class DevSwaggerAuthFilter extends OncePerRequestFilter {

    @Value("${cse.security.dev-bypass:true}")
    private boolean devBypass;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");
        if (devBypass
                && SecurityContextHolder.getContext().getAuthentication() == null
                && (authHeader == null || !authHeader.startsWith("Bearer "))) {
            var authorities = List.of(
                    new SimpleGrantedAuthority("ROLE_" + Roles.ADMIN_SYSTEME),
                    new SimpleGrantedAuthority("ROLE_" + Roles.CHEF_CHANTIER),
                    new SimpleGrantedAuthority("ROLE_" + Roles.RESPONSABLE_MATERIEL),
                    new SimpleGrantedAuthority("ROLE_" + Roles.RESPONSABLE_ACHATS),
                    new SimpleGrantedAuthority("ROLE_" + Roles.DAF),
                    new SimpleGrantedAuthority("ROLE_" + Roles.DIRECTION_GENERALE),
                    new SimpleGrantedAuthority("ROLE_" + Roles.FOURNISSEUR)
            );
            var auth = new UsernamePasswordAuthenticationToken("swagger-dev-user", null, authorities);
            SecurityContextHolder.getContext().setAuthentication(auth);
        }
        filterChain.doFilter(request, response);
    }
}

