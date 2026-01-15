package com.roshansutihar.posmachine.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.oidc.web.logout.OidcClientInitiatedLogoutSuccessHandler;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;

import java.util.*;
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final ClientRegistrationRepository clientRegistrationRepository;

    @Value("${LOGOUT_URL}")
    private String logoutUrl;

    public SecurityConfig(ClientRegistrationRepository clientRegistrationRepository) {
        this.clientRegistrationRepository = clientRegistrationRepository;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers("/api/**", "/complete-sale")
                )

                .oauth2Login(oauth2 -> oauth2
                        .loginPage("/oauth2/authorization/keycloak")
                        .defaultSuccessUrl("/", true)
                        .userInfoEndpoint(userInfo ->
                                userInfo.oidcUserService(oidcUserService())
                        )
                )

                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt
                                .jwtAuthenticationConverter(jwtAuthenticationConverter())
                        )
                )

                // =========================
                // AUTHORIZATION RULES
                // =========================
                .authorizeHttpRequests(auth -> auth

                        // Static assets
                        .requestMatchers(
                                "/css/**",
                                "/js/**",
                                "/images/**",
                                "/webjars/**"
                        ).permitAll()


                        .requestMatchers("/api/payments/**").permitAll()

                        .requestMatchers("/api/config/**").permitAll()

                        // UI pages (ADMIN only)
                        .requestMatchers("/").hasRole("ADMIN")
                        .requestMatchers("/products/**").hasRole("ADMIN")
                        .requestMatchers("/reports/**").hasRole("ADMIN")
                        .requestMatchers("/store-info/**").hasRole("ADMIN")
                        .requestMatchers("/complete-sale").hasRole("ADMIN")

                        // Everything else requires login
                        .anyRequest().authenticated()
                )

                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessHandler(oidcLogoutSuccessHandler())
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .deleteCookies("JSESSIONID")
                )

                // =========================
                // ACCESS DENIED
                // =========================
                .exceptionHandling(ex -> ex
                        .accessDeniedPage("/access-denied")
                );

        return http.build();
    }

    @Bean
    public OAuth2UserService<OidcUserRequest, OidcUser> oidcUserService() {

        OidcUserService delegate = new OidcUserService();

        return userRequest -> {
            OidcUser oidcUser = delegate.loadUser(userRequest);

            Set<GrantedAuthority> authorities = new HashSet<>();

            Map<String, Object> realmAccess =
                    oidcUser.getIdToken().getClaim("realm_access");

            if (realmAccess != null) {
                @SuppressWarnings("unchecked")
                List<String> roles =
                        (List<String>) realmAccess.get("roles");

                if (roles != null) {
                    roles.forEach(role ->
                            authorities.add(
                                    new SimpleGrantedAuthority(
                                            "ROLE_" + role.toUpperCase()
                                    )
                            )
                    );
                }
            }

            return new DefaultOidcUser(
                    authorities,
                    oidcUser.getIdToken(),
                    oidcUser.getUserInfo(),
                    "preferred_username"
            );
        };
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {

        JwtAuthenticationConverter converter =
                new JwtAuthenticationConverter();

        converter.setJwtGrantedAuthoritiesConverter(jwt -> {

            Collection<GrantedAuthority> authorities = new ArrayList<>();

            Map<String, Object> realmAccess =
                    jwt.getClaim("realm_access");

            if (realmAccess != null) {
                @SuppressWarnings("unchecked")
                List<String> roles =
                        (List<String>) realmAccess.get("roles");

                if (roles != null) {
                    roles.forEach(role ->
                            authorities.add(
                                    new SimpleGrantedAuthority(
                                            "ROLE_" + role.toUpperCase()
                                    )
                            )
                    );
                }
            }

            return authorities;
        });

        return converter;
    }

    private LogoutSuccessHandler oidcLogoutSuccessHandler() {

        OidcClientInitiatedLogoutSuccessHandler handler =
                new OidcClientInitiatedLogoutSuccessHandler(
                        clientRegistrationRepository
                );

        handler.setPostLogoutRedirectUri(logoutUrl);
        return handler;
    }
}
