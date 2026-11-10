package com.bakban9.support;

import com.bakban9.domain.Admin;
import com.bakban9.domain.AdminRepository;
import com.bakban9.security.AdminPrincipal;
import com.bakban9.service.AdminAccountService;
import com.bakban9.service.MailService;
import com.bakban9.service.RateLimiter;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.flyway.autoconfigure.FlywayMigrationStrategy;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;

/**
 * Runs against a real PostgreSQL database (TEST_DATABASE_URL, default
 * jdbc:postgresql://localhost:5432/bakban9_test). The schema is rebuilt and the shop data
 * seeded once per test run; each test resets the rows it may change.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(IntegrationTest.FreshDatabase.class)
public abstract class IntegrationTest {

    public static final String OWNER_EMAIL = "owner@test.local";
    public static final String EDITOR_EMAIL = "editor@test.local";
    public static final String PASSWORD = "correct-horse-1";

    @TestConfiguration
    static class FreshDatabase {
        @Bean
        FlywayMigrationStrategy cleanMigrate() {
            return (Flyway flyway) -> {
                flyway.clean();
                flyway.migrate();
            };
        }
    }

    @Autowired protected MockMvc mvc;
    @Autowired protected JdbcTemplate jdbc;
    @Autowired protected RateLimiter rateLimiter;
    @Autowired protected AdminRepository admins;
    @Autowired protected AdminAccountService accounts;

    @MockitoBean protected MailService mail;

    @BeforeEach
    void resetData() {
        jdbc.update("delete from members");
        jdbc.update("delete from messages");
        jdbc.update("delete from product_images where product_id > 9");
        jdbc.update("delete from products where id > 9");
        jdbc.update("update products set is_visible = true, deleted_at = null, price_thb = null");
        jdbc.update("update page_sections set is_visible = true");
        jdbc.update("update page_sections set content = jsonb_set(content, '{perPage}', '\"6\"') where key = 'products'");
        jdbc.update("update pages set is_published = true");
        jdbc.update("delete from admin_invites");
        rateLimiter.reset();
        ensureAdmin(OWNER_EMAIL, Admin.OWNER);
        ensureAdmin(EDITOR_EMAIL, Admin.EDITOR);
        jdbc.update("update admins set failed_attempts = 0, locked_until = null, is_active = true");
    }

    private void ensureAdmin(String email, String role) {
        if (admins.findByEmailIgnoreCase(email).isEmpty()) {
            accounts.createAdmin(role, email, PASSWORD, role);
        }
    }

    protected RequestPostProcessor asOwner() {
        return as(OWNER_EMAIL);
    }

    protected RequestPostProcessor asEditor() {
        return as(EDITOR_EMAIL);
    }

    private RequestPostProcessor as(String email) {
        AdminPrincipal p = new AdminPrincipal(admins.findByEmailIgnoreCase(email).orElseThrow());
        return authentication(new UsernamePasswordAuthenticationToken(p, null, p.getAuthorities()));
    }

    protected long count(String sql, Object... args) {
        Long n = jdbc.queryForObject(sql, Long.class, args);
        return n == null ? 0 : n;
    }
}
