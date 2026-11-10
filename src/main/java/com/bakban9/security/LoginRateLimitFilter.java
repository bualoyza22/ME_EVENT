package com.bakban9.security;

import com.bakban9.config.AppProperties;
import com.bakban9.service.RateLimiter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import org.springframework.web.filter.OncePerRequestFilter;

/** Limits login POSTs per IP, so many accounts cannot be guessed from one address. */
public class LoginRateLimitFilter extends OncePerRequestFilter {

    private final RateLimiter limiter;
    private final AppProperties props;

    public LoginRateLimitFilter(RateLimiter limiter, AppProperties props) {
        this.limiter = limiter;
        this.props = props;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        if ("POST".equals(req.getMethod()) && (req.getContextPath() + "/admin/login").equals(req.getRequestURI())) {
            AppProperties.RateLimit rl = props.rateLimit();
            if (!limiter.tryAcquire("login:" + req.getRemoteAddr(), rl.loginMax(), Duration.ofMinutes(rl.loginWindowMinutes()))) {
                res.sendRedirect(req.getContextPath() + "/admin/login?throttled");
                return;
            }
        }
        chain.doFilter(req, res);
    }
}
