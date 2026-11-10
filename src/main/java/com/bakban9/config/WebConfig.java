package com.bakban9.config;

import com.bakban9.storage.LocalStorageService;
import com.bakban9.storage.StorageService;
import jakarta.servlet.Filter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final StorageService storage;
    private final AppProperties props;

    public WebConfig(StorageService storage, AppProperties props) {
        this.storage = storage;
        this.props = props;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        if (storage instanceof LocalStorageService local) {
            String base = props.storage().publicBaseUrl();
            registry.addResourceHandler(base + "/**")
                    .addResourceLocations(local.root().toUri().toString() + "/")
                    .setCacheControl(CacheControl.maxAge(Duration.ofDays(365)).cachePublic().immutable());
        }
    }

    /** Uploaded files may never run scripts, even when opened directly (SVG). */
    @Bean
    FilterRegistrationBean<Filter> mediaHeadersFilter() {
        String prefix = props.storage().publicBaseUrl() + "/";
        FilterRegistrationBean<Filter> reg = new FilterRegistrationBean<>((req, res, chain) -> {
            HttpServletRequest r = (HttpServletRequest) req;
            if (r.getRequestURI().startsWith(prefix)) {
                HttpServletResponse w = (HttpServletResponse) res;
                w.setHeader("Content-Security-Policy", "default-src 'none'; img-src 'self' data:; style-src 'unsafe-inline'; sandbox");
                w.setHeader("X-Content-Type-Options", "nosniff");
            }
            chain.doFilter(req, res);
        });
        reg.setOrder(Integer.MIN_VALUE + 100);
        return reg;
    }
}
