package com.bakban9.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Loads the CSRF token before the page starts streaming, so the token cookie can still be
 * written when a template asks for it halfway through rendering.
 */
public class CsrfEagerFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        Object token = req.getAttribute(CsrfToken.class.getName());
        if (token instanceof CsrfToken t) {
            t.getToken();
        }
        chain.doFilter(req, res);
    }
}
