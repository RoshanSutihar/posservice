package com.roshansutihar.posmachine.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // Enable OAuth2 Login for Thymeleaf pages
                .oauth2Login(oauth2 -> oauth2
                        .loginPage("/oauth2/authorization/keycloak")
                        .defaultSuccessUrl("/", true)
                )

                // Enable OAuth2 Resource Server for API endpoints
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt
                                .jwtAuthenticationConverter(jwtAuthenticationConverter())
                        )
                )

                // Configure authorization rules
                .authorizeHttpRequests(authz -> authz
                        // Public endpoints (no authentication required)
                        .requestMatchers(
                                "/css/**",
                                "/js/**",
                                "/images/**"
                        ).permitAll()

                        .requestMatchers("/").hasAnyRole("ADMIN")

                        .requestMatchers("/api/**").authenticated()

                        // Thymeleaf pages - different roles
                        .requestMatchers("/products/**").hasAnyRole( "ADMIN")
                        .requestMatchers("/reports/**").hasAnyRole( "ADMIN")
                        .requestMatchers("/store/**").hasAnyRole("ADMIN")

                        // POS page - accessible to cashiers and above
                        .requestMatchers("/complete-sale").hasAnyRole( "ADMIN")

                        // Deny everything else (require authentication)
                        .anyRequest().authenticated()
                )

                // Logout configuration
                .logout(logout -> logout
                        .logoutSuccessUrl("/")
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .deleteCookies("JSESSIONID")
                )

                // Exception handling
                .exceptionHandling(exceptions -> exceptions
                        .accessDeniedPage("/access-denied")
                );

        return http.build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter grantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
        grantedAuthoritiesConverter.setAuthorityPrefix("ROLE_");
        grantedAuthoritiesConverter.setAuthoritiesClaimName("roles");

        JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(grantedAuthoritiesConverter);

        return jwtAuthenticationConverter;
    }
}
