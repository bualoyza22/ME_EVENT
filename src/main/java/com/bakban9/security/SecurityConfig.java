package com.bakban9.security;

import com.bakban9.config.AppProperties;
import com.bakban9.service.RateLimiter;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /** Fonts from Google Fonts, map iframe from Google Maps, no inline scripts. */
    static final String CSP = String.join("; ",
            "default-src 'self'",
            "script-src 'self'",
            "style-src 'self' https://fonts.googleapis.com",
            "font-src 'self' https://fonts.gstatic.com",
            "img-src 'self' data: https:",
            "frame-src https://www.google.com https://maps.google.com",
            "form-action 'self'",
            "frame-ancestors 'self'",
            "base-uri 'self'",
            "object-src 'none'");

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http, LoginAttemptService attempts, RateLimiter limiter,
                                    AppProperties props) throws Exception {
        SavedRequestAwareAuthenticationSuccessHandler success = new SavedRequestAwareAuthenticationSuccessHandler();
        success.setDefaultTargetUrl("/admin");
        success.setAlwaysUseDefaultTargetUrl(false);

        http
                .authorizeHttpRequests(a -> a
                        .requestMatchers("/admin/login", "/admin/invite/**").permitAll()
                        .requestMatchers("/admin/members/export", "/admin/members/*/delete",
                                "/admin/settings/admins/**", "/admin/settings/audit").hasRole("OWNER")
                        .requestMatchers("/admin", "/admin/**").hasAnyRole("OWNER", "EDITOR")
                        .anyRequest().permitAll())
                .formLogin(f -> f
                        .loginPage("/admin/login")
                        .loginProcessingUrl("/admin/login")
                        .usernameParameter("email")
                        .passwordParameter("password")
                        .successHandler((req, res, auth) -> {
                            if (auth.getPrincipal() instanceof AdminPrincipal p) {
                                attempts.recordSuccess(p.getId());
                            }
                            success.onAuthenticationSuccess(req, res, auth);
                        })
                        .failureHandler((req, res, ex) -> {
                            String email = req.getParameter("email");
                            if (ex instanceof LockedException) {
                                res.sendRedirect("/admin/login?locked");
                            } else if (attempts.recordFailure(email)) {
                                res.sendRedirect("/admin/login?locked");
                            } else {
                                res.sendRedirect("/admin/login?error");
                            }
                        })
                        .permitAll())
                .logout(l -> l
                        .logoutUrl("/admin/logout")
                        .logoutSuccessUrl("/admin/login?logout")
                        .deleteCookies("BAKBAN_SESSION"))
                .exceptionHandling(e -> e.accessDeniedHandler((req, res, ex) -> {
                    res.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    req.getRequestDispatcher("/admin/forbidden").forward(req, res);
                }))
                .csrf(c -> {
                    // cookie store: anonymous visitors get no server session just for the token
                    CookieCsrfTokenRepository repo = new CookieCsrfTokenRepository();
                    repo.setCookieName("BAKBAN_CSRF");
                    repo.setCookieCustomizer(ck -> ck.httpOnly(true).sameSite("Lax").secure(props.cookieSecure()));
                    c.csrfTokenRepository(repo);
                })
                .addFilterAfter(new CsrfEagerFilter(), CsrfFilter.class)
                .addFilterBefore(new LoginRateLimitFilter(limiter, props), UsernamePasswordAuthenticationFilter.class)
                .headers(h -> h
                        .contentSecurityPolicy(c -> c.policyDirectives(CSP))
                        .referrerPolicy(r -> r.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                        .frameOptions(fo -> fo.sameOrigin()))
                .sessionManagement(s -> s.sessionFixation(sf -> sf.changeSessionId()));
        return http.build();
    }
}
