package pl.mdomino.artapp.security;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.oauth2.client.oidc.web.logout.OidcClientInitiatedLogoutSuccessHandler;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.stereotype.Component;
import pl.mdomino.artapp.repo.UserRepo;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JvtAuthConverter jvtAuthConverter = new JvtAuthConverter();
    private final RequestFilter requestFilter;
    private final UserInitializationFilter userInitializationFilter;

    @Bean
    @Order(1)
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf()
                    .disable()
                .securityMatcher("/api/**")
                .authorizeHttpRequests()
                    .requestMatchers("/api/images/preview/**").permitAll()
                    .requestMatchers("/api/images/random").permitAll()
                    .anyRequest()
                        .authenticated();

        http
                .oauth2ResourceServer()
                    .jwt()
                        .jwtAuthenticationConverter(jvtAuthConverter);

        http
                .sessionManagement()
                    .sessionCreationPolicy(SessionCreationPolicy.STATELESS);

        http.addFilterAfter(requestFilter, org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter.class);


        return http.build();
    }

    interface AuthoritiesConverter extends Converter<Map<String, Object>, Collection<GrantedAuthority>> {}

    @Bean
    AuthoritiesConverter realmRolesAuthoritiesConverter() {
        return claims -> {
            var realmAccess = Optional.ofNullable((Map<String, Object>) claims.get("realm_access"));
            var roles = realmAccess.flatMap(map -> Optional.ofNullable((List<String>) map.get("roles")));
            return roles.map(List::stream)
                    .orElse(Stream.empty())
                    .map(SimpleGrantedAuthority::new)
                    .map(GrantedAuthority.class::cast)
                    .toList();
        };
    }

    @Bean
    GrantedAuthoritiesMapper authenticationConverter(
            Converter<Map<String, Object>, Collection<GrantedAuthority>> authoritiesConverter) {
        return (authorities) -> authorities.stream()
                .filter(authority -> authority instanceof OidcUserAuthority)
                .map(OidcUserAuthority.class::cast)
                .map(OidcUserAuthority::getIdToken)
                .map(OidcIdToken::getClaims)
                .map(authoritiesConverter::convert)
                .flatMap(roles -> roles.stream())
                .collect(Collectors.toSet());
    }

    @Bean
    SecurityFilterChain clientSecurityFilterChain(
            HttpSecurity http,
            ClientRegistrationRepository clientRegistrationRepository) throws Exception {
        http.oauth2Login(Customizer.withDefaults());
        http.logout((logout) -> {
            var logoutSuccessHandler =
                    new OidcClientInitiatedLogoutSuccessHandler(clientRegistrationRepository);
            logoutSuccessHandler.setPostLogoutRedirectUri("http://localhost:8080/");
            logout.logoutSuccessHandler(logoutSuccessHandler);
        });

        http.oauth2Login(oauth -> oauth
                .successHandler((request, response, authentication) -> {
                    response.sendRedirect("/");
                })
        );

        http.addFilterAfter(userInitializationFilter, org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class);

        http.authorizeHttpRequests(requests -> {
            requests.requestMatchers("/", "/favicon.ico").permitAll();
            requests.requestMatchers("/test").authenticated();
            requests.requestMatchers("/admin").hasAuthority("Admin");
            requests.anyRequest().authenticated();
        });

        return http.build();
    }
}
