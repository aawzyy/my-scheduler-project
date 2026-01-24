package com.personal.scheduler_api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return (web) -> web.ignoring()
                .requestMatchers(new AntPathRequestMatcher("/api/webhooks/**"))
                .requestMatchers(new AntPathRequestMatcher("/api/share/**"))
                .requestMatchers(new AntPathRequestMatcher("/api/availability/token/**"))
                .requestMatchers(new AntPathRequestMatcher("/api/personal-blocks/**"))
                .requestMatchers(new AntPathRequestMatcher("/api/dashboard/status"));
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                
                // === [SOLUSI UTAMA] MATIKAN MEMORI MASA LALU ===
                // Ini mencegah Spring melempar balik ke localhost:8081
                .requestCache(cache -> cache.disable()) 
                
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(new AntPathRequestMatcher("/api/availability/check")).permitAll()
                        .requestMatchers(new AntPathRequestMatcher("/api/appointments", "POST")).permitAll()
                        .requestMatchers(new AntPathRequestMatcher("/api/auth/success")).permitAll()
                        .requestMatchers(new AntPathRequestMatcher("/api/mobile/auth/**")).permitAll() // <--- TAMBAH INI
                        .anyRequest().authenticated())
                .exceptionHandling(e -> e
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .oauth2Login(oauth2 -> oauth2
                        // === PAKSA MASUK KE CONTROLLER ===
                        // Gunakan Absolute URL Production
                        .defaultSuccessUrl("https://aawzyy.my.id/api/auth/success", true)
                );

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // === HAPUS SEMUA LOCALHOST DI SINI ===
        // Kita bikin Full Production Only
        configuration.setAllowedOrigins(List.of(
            "https://aawzyy.my.id",
            "https://www.aawzyy.my.id"
        ));
        
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}