package com.trinity.hermes.usermanagement.config;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

  @Value("${app.frontend-url:http://localhost:3000}")
  private String frontendUrl;

  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration config = new CorsConfiguration();
    config.setAllowedOrigins(List.of(frontendUrl));
    config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
    config.setAllowedHeaders(List.of("*"));
    config.setAllowCredentials(true);
    config.setMaxAge(3600L);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", config);
    return source;
  }

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

    http.cors(Customizer.withDefaults());

    // For now ignored CSRF only for API endpoints.
    http.csrf(
            csrf ->
                csrf.ignoringRequestMatchers(
                    "/api/**",
                    "/notification/**",
                    "/v3/api-docs/**",
                    "/api-docs/**",
                    "/swagger-ui/**",
                    "/swagger/**"))
        .exceptionHandling(
            ex ->
                ex.accessDeniedHandler(
                    (request, response, accessDeniedException) -> {
                      response.setStatus(HttpStatus.FORBIDDEN.value());
                      response.setContentType("application/json");
                      response
                          .getWriter()
                          .write("{\"message\": \"You do not have access to this resource\"}");
                    }))
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers(
                        "/v3/api-docs/**", "/swagger/**", "/swagger-ui/**", "/api-docs/**")
                    .permitAll()
                    .requestMatchers("/api/public/**")
                    .permitAll()
                    .requestMatchers("/api/auth/**")
                    .permitAll()
                    .requestMatchers("/api/v1/disruptions", "/api/v1/disruptions/**")
                    .permitAll()
                    .requestMatchers("/api/v1/dashboard/train", "/api/v1/dashboard/train/**")
                    .hasAnyRole("City_Manager", "Train_Admin", "Train_Provider")
                    .requestMatchers("/api/v1/dashboard/cycle", "/api/v1/dashboard/cycle/**")
                    .hasAnyRole("City_Manager", "Cycle_Admin", "Cycle_Provider")
                    .requestMatchers("/api/v1/dashboard/**")
                    .authenticated()
                    .requestMatchers("/api/v1/recommendation-engine/**")
                    .hasRole("City_Manager")
                    .requestMatchers("/api/v1/bus/**")
                    .hasAnyRole("City_Manager", "Bus_Admin", "Bus_Provider")
                    .requestMatchers("/api/v1/train/**")
                    .hasAnyRole("City_Manager", "Train_Admin", "Train_Provider")
                    .requestMatchers("/api/v1/tram/**")
                    .hasAnyRole("City_Manager", "Tram_Admin", "Tram_Provider")
                    .requestMatchers("/api/v1/ev/**")
                    .authenticated()
                    .requestMatchers("/api/v1/events/**", "/api/v1/pedestrians/**")
                    .authenticated()
                    .requestMatchers("/error")
                    .permitAll()
                    .requestMatchers("/api/notification/v1", "/api/notification/v1/**")
                    .permitAll()
                    .requestMatchers(HttpMethod.POST, "/api/usermanagement/register")
                    .permitAll()
                    .requestMatchers(HttpMethod.DELETE, "/api/usermanagement/delete")
                    .authenticated()
                    .requestMatchers(HttpMethod.GET, "/api/usermanagement/users")
                    .authenticated()
                    .requestMatchers("/api/usermanagement/profile")
                    .authenticated()
                    .requestMatchers("/api/usermanagement/password")
                    .authenticated()
                    .requestMatchers("/api/v1/car/**")
                    .hasRole("City_Manager")
                    // MV management: internal API accessed via port-forward only, no auth required
                    .requestMatchers("/api/v1/mv", "/api/v1/mv/**")
                    .permitAll()
                    .requestMatchers("/api/trains")
                    .hasRole("City_Manager")
                    .requestMatchers("/api/buses")
                    .hasAnyRole("City_Manager", "Bus_Provider")
                    .anyRequest()
                    .authenticated())
        .oauth2ResourceServer(
            oauth2 ->
                oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())))
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

    return http.build();
  }

  @Bean
  public Converter<Jwt, AbstractAuthenticationToken> jwtAuthenticationConverter() {
    JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
    converter.setJwtGrantedAuthoritiesConverter(jwtGrantedAuthoritiesConverter());
    return converter;
  }

  @Bean
  public Converter<Jwt, Collection<GrantedAuthority>> jwtGrantedAuthoritiesConverter() {
    JwtGrantedAuthoritiesConverter defaultConverter = new JwtGrantedAuthoritiesConverter();

    return jwt -> {
      Collection<GrantedAuthority> defaultAuthorities = defaultConverter.convert(jwt);
      Collection<GrantedAuthority> realmRoles = extractRealmRoles(jwt);

      return Stream.concat(defaultAuthorities.stream(), realmRoles.stream())
          .collect(Collectors.toList());
    };
  }

  private Collection<GrantedAuthority> extractRealmRoles(Jwt jwt) {
    Map<String, Object> realmAccess = jwt.getClaim("realm_access");

    if (realmAccess == null || realmAccess.get("roles") == null) {
      return List.of();
    }

    @SuppressWarnings("unchecked")
    List<String> roles = (List<String>) realmAccess.get("roles");

    return roles.stream()
        .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
        .collect(Collectors.toList());
  }
}
