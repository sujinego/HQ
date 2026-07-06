package com.lg.hq.hqserver.config;

import com.lg.hq.hqserver.service.UserDetailsServiceImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    private final UserDetailsServiceImpl userDetailsService;

    public SecurityConfig(UserDetailsServiceImpl userDetailsService) {
        this.userDetailsService = userDetailsService;
    }


    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
            .headers()
                .frameOptions().sameOrigin()
            .and()
            .csrf().disable()
            .authorizeRequests()
                .antMatchers("/login", "/hash", "/seed-products",
                    "/clean-test-products").permitAll()
                // ── Actuator ──────────────────────────────
                .antMatchers("/actuator/**").hasRole("ADMIN")

                // ── 주문 권한 분리 ──────────────────────────
                .antMatchers(HttpMethod.GET,    "/api/orders/**")
                    .hasAnyRole("ADMIN", "KR_USER", "US_USER")  // 조회: 전체
                .antMatchers(HttpMethod.POST,   "/api/orders/**")
                    .hasAnyRole("ADMIN", "KR_USER")             // 등록: KR만
                .antMatchers(HttpMethod.PUT,    "/api/orders/**")
                    .hasAnyRole("ADMIN", "KR_USER")             // 수정: KR만
                .antMatchers(HttpMethod.DELETE, "/api/orders/**")
                    .hasRole("ADMIN")                           // 삭제: ADMIN만

                // ── HR 권한 분리 ──────────────────────────
                .antMatchers(HttpMethod.GET,    "/api/hr/**")
                .hasAnyRole("ADMIN", "KR_USER", "US_USER")
                .antMatchers(HttpMethod.POST,   "/api/hr/**")
                .hasRole("ADMIN")
                .antMatchers(HttpMethod.PUT,    "/api/hr/**")
                .hasRole("ADMIN")
                .antMatchers(HttpMethod.DELETE, "/api/hr/**")
                .hasRole("ADMIN")
                // ── 배치 수동 실행: ADMIN만 ──────────────────
                .antMatchers(HttpMethod.POST, "/api/batch/run/**")
                    .hasRole("ADMIN")
                // ── 나머지 API: 로그인만 하면 접근 가능 ──────
                .antMatchers("/api/**").authenticated()
                .anyRequest().authenticated()

            .and()
            .formLogin()
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .defaultSuccessUrl("/", true)
                .failureUrl("/login?error=true")
                .permitAll()
            .and()
            .logout()
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login")
                .deleteCookies("JSESSIONID")
                .permitAll()
            .and()
                // ── 예외 처리 ──────────────────────────────────
                .exceptionHandling()
                // 미인증 (401): 로그인 안 한 상태에서 접근
                .authenticationEntryPoint((req, res, e) -> {
                    // 페이지 요청이면 로그인 페이지로
                    String accept = req.getHeader("Accept");
                    if (accept != null && accept.contains("text/html")) {
                        res.sendRedirect("/login");
                        return;
                    }

                    res.setStatus(401);
                    res.setContentType("application/json;charset=UTF-8");
                    res.getWriter().write(
                            "{\"success\":false,\"status\":401," +
                                    "\"message\":\"로그인이 필요합니다.\"}");
                })
                // 권한 없음 (403): 로그인은 했지만 권한 부족
                .accessDeniedHandler((req, res, e) -> {
                    res.setStatus(403);
                    res.setContentType("application/json;charset=UTF-8");
                    res.getWriter().write(
                            "{\"success\":false,\"status\":403," +
                                    "\"message\":\"접근 권한이 없습니다.\"}");
                })

            .and()
                .sessionManagement()
                .maximumSessions(1)           // 중복 로그인 방지
                .maxSessionsPreventsLogin(false); // 새 로그인이 이전 세션 만료
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.userDetailsService(userDetailsService)
                .passwordEncoder(passwordEncoder());
    }
}