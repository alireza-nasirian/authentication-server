package com.example.authserver.config;

import com.example.authserver.security.JwtAuthenticationFilter;
import com.example.authserver.service.CustomUserDetailsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity()
public class SecurityConfig {

    private final CustomUserDetailsService customUserDetailsService;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(CustomUserDetailsService customUserDetailsService,
                          JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.customUserDetailsService = customUserDetailsService;
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        // FIX: use no-arg ctor and set the services explicitly
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(customUserDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        // Use PathPattern-based request matchers
        PathPatternRequestMatcher.Builder paths = PathPatternRequestMatcher.withDefaults();

        http
                // JWT APIs are typically stateless; CORS enabled
                .cors(c -> c.configurationSource(corsConfigurationSource()))
                // For token-based APIs, disabling CSRF is common
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // H2 console needs frames from same origin
                .headers(h -> h.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin))

                .authorizeHttpRequests(auth -> auth
                        // --- Public endpoints ---
                        .requestMatchers(
                                paths.matcher("/api/auth/**"),
                                paths.matcher("/swagger-ui/**"),
                                paths.matcher("/swagger-ui.html"),
                                paths.matcher("/v3/api-docs/**"),
                                paths.matcher("/v3/api-docs.yaml"),
                                paths.matcher("/swagger-resources/**"),
                                paths.matcher("/webjars/**"),
                                paths.matcher("/h2-console/**"),
                                paths.matcher("/actuator/**"),
                                paths.matcher("/favicon.ico")
                        ).permitAll()

                        // examples of fine-grained method+path (optional)
                        .requestMatchers(paths.matcher(HttpMethod.GET, "/api/users")).permitAll()
                        .requestMatchers(paths.matcher(HttpMethod.GET, "/api/users/{id}")).permitAll()

                        // --- Protected endpoints ---
                        .requestMatchers(paths.matcher("/api/users/me")).authenticated()
                        .anyRequest().authenticated()
                );

        // JWT filter before UsernamePasswordAuthenticationFilter
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cfg = new CorsConfiguration();
        // NOTE: If you set allowCredentials(true), avoid "*" at runtime. Prefer explicit origins.
        cfg.setAllowedOriginPatterns(List.of("*"));
        cfg.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        cfg.setAllowedHeaders(List.of("*"));
        cfg.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cfg);
        return source;
    }
}
