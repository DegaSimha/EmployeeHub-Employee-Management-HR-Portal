package com.employeehub.config;

import com.employeehub.security.JwtAuthenticationFilter;
import com.employeehub.exception.ApiError;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.*;
import java.util.List;
import java.time.LocalDateTime;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    @Bean PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthenticationFilter jwtFilter,
                                            ObjectMapper objectMapper) throws Exception {
        http.csrf(csrf -> csrf.disable())
            .cors(Customizer.withDefaults())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(exceptions -> exceptions
                    .authenticationEntryPoint((request, response, ex) -> {
                        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        response.setContentType("application/json");
                        response.setCharacterEncoding("UTF-8");
                        objectMapper.writeValue(response.getWriter(),
                                new ApiError(LocalDateTime.now(), 401, "Authentication required.", request.getRequestURI()));
                    })
                    .accessDeniedHandler((request, response, ex) -> {
                        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                        response.setContentType("application/json");
                        response.setCharacterEncoding("UTF-8");
                        objectMapper.writeValue(response.getWriter(),
                                new ApiError(LocalDateTime.now(), 403, "Access denied.", request.getRequestURI()));
                    }))
            .authorizeHttpRequests(auth -> auth
                    .requestMatchers("/api/auth/login", "/", "/index.html", "/login.html", "/css/**", "/js/**", "/favicon.ico").permitAll()
                    .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/departments").authenticated()
                    .requestMatchers("/api/departments/**").hasRole("ADMIN")
                    .requestMatchers(HttpMethod.GET, "/api/holidays", "/api/holidays/**").authenticated()
                    .requestMatchers("/api/holidays/**").hasRole("ADMIN")
                    .requestMatchers("/api/employees/**").hasRole("ADMIN")
                    .requestMatchers(HttpMethod.POST, "/api/attendance/mine/check-in", "/api/attendance/mine/check-out").hasRole("EMPLOYEE")
                    .requestMatchers(HttpMethod.GET, "/api/attendance/mine", "/api/leaves/mine", "/api/profile").hasAnyRole("ADMIN", "EMPLOYEE")
                    .requestMatchers(HttpMethod.POST, "/api/leaves").hasRole("EMPLOYEE")
                    .requestMatchers(HttpMethod.DELETE, "/api/leaves/mine/**").hasRole("EMPLOYEE")
                    .requestMatchers("/api/leaves/**").hasRole("ADMIN")
                    .requestMatchers("/api/attendance/**").hasRole("ADMIN")
                    .requestMatchers("/api/dashboard/**", "/api/reports/**").hasRole("ADMIN")
                    .requestMatchers("/api/profile/**").authenticated()
                    .requestMatchers("/api/**").authenticated()
                    .anyRequest().permitAll())
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(@Value("${app.cors.allowed-origin}") String allowedOrigin) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(allowedOrigin));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        configuration.setAllowCredentials(false);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }
}
