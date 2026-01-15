package com.roshansutihar.posmachine.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;

import java.util.*;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
                .oauth2Login(oauth2 -> oauth2
                        .loginPage("/oauth2/authorization/keycloak")
                        .defaultSuccessUrl("/", true)
                        .userInfoEndpoint(userInfo ->
                                userInfo.userAuthoritiesMapper(userAuthoritiesMapper())
                        )
                )

                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt
                                .jwtAuthenticationConverter(jwtAuthenticationConverter())
                        )
                )

                .authorizeHttpRequests(authz -> authz

                        // Static resources
                        .requestMatchers(
                                "/css/**",
                                "/js/**",
                                "/images/**",
                                "/webjars/**"
                        ).permitAll()

                        // Home page
                        .requestMatchers("/").hasRole("ADMIN")

                        // API endpoints (JWT)
                        .requestMatchers("/api/**").authenticated()

                        // Thymeleaf pages
                        .requestMatchers("/products/**").hasRole("ADMIN")
                        .requestMatchers("/reports/**").hasRole("ADMIN")
                        .requestMatchers("/store/**").hasRole("ADMIN")
                        .requestMatchers("/complete-sale").hasRole("ADMIN")

                        // Everything else
                        .anyRequest().authenticated()
                )

                .logout(logout -> logout
                        .logoutSuccessUrl("/")
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .deleteCookies("JSESSIONID")
                )

                .exceptionHandling(ex -> ex
                        .accessDeniedPage("/access-denied")
                );

        return http.build();
    }

    @Bean
    public GrantedAuthoritiesMapper userAuthoritiesMapper() {
        return (authorities) -> {
            Set<GrantedAuthority> mappedAuthorities = new HashSet<>();

            for (GrantedAuthority authority : authorities) {
                // Keep original authorities
                mappedAuthorities.add(authority);

                if (authority instanceof OidcUserAuthority oidcAuthority) {

                    Map<String, Object> claims = oidcAuthority.getIdToken().getClaims();

                    // Check realm_access in ID token claims
                    if (claims.containsKey("realm_access")) {
                        Object realmAccessObj = claims.get("realm_access");

                        if (realmAccessObj instanceof Map realmAccess) {
                            @SuppressWarnings("unchecked")
                            List<String> roles = (List<String>) realmAccess.get("roles");

                            if (roles != null) {
                                roles.forEach(role -> {
                                    String roleWithPrefix = "ROLE_" + role.toUpperCase();
                                    mappedAuthorities.add(
                                            new SimpleGrantedAuthority(roleWithPrefix)
                                    );
                                });
                            }
                        }
                    }
                }
            }

            return mappedAuthorities;
        };
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {

        JwtGrantedAuthoritiesConverter authoritiesConverter =
                new JwtGrantedAuthoritiesConverter();

        // We handle roles manually
        authoritiesConverter.setAuthorityPrefix("ROLE_");
        authoritiesConverter.setAuthoritiesClaimName("realm_access.roles");

        JwtAuthenticationConverter jwtConverter =
                new JwtAuthenticationConverter();

        jwtConverter.setJwtGrantedAuthoritiesConverter(jwt -> {
            Collection<GrantedAuthority> authorities = new ArrayList<>();

            Map<String, Object> realmAccess =
                    jwt.getClaim("realm_access");

            if (realmAccess != null) {
                List<String> roles =
                        (List<String>) realmAccess.get("roles");

                roles.forEach(role ->
                        authorities.add(
                                new SimpleGrantedAuthority(
                                        "ROLE_" + role.toUpperCase()
                                )
                        )
                );
            }

            return authorities;
        });

        return jwtConverter;
    }
}
