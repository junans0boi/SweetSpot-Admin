package com.hollywood.sweetspotadmin.config;

import com.hollywood.sweetspotadmin.global.security.JwtAuthenticationFilter;
import com.hollywood.sweetspotadmin.user.model.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // ✅ CSRF 비활성화 확인
                .csrf(AbstractHttpConfigurer::disable)

                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // .authorizeHttpRequests(auth -> auth

                //         // 1. 로그인, 토큰 재발급 등 인증 API는 모두 허용 (권한 필요 없음)
                //         .requestMatchers("/api/admin/auth/**") // ⬅️ (가정) 로그인 API 경로
                //         .permitAll()

                //         // 2. 분석 API는 임시로 모두 허용
                //         .requestMatchers("/api/admin/analysis/**")
                //         .permitAll()

                //         // 3. 그 외의 모든 /api/admin/** 경로는 "ADMIN" 역할 필요
                //         .requestMatchers("/api/admin/**")
                //         .hasRole(Role.ROLE_ADMIN.name().replace("ROLE_", ""))

                //         // 4. 위에서 정의하지 않은 나머지 모든 요청은 거부
                //         .anyRequest().denyAll())
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())

                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}