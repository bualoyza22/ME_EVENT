package com.bakban9.web;

import com.bakban9.support.IntegrationTest;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdminPermissionsTest extends IntegrationTest {

    // ---- anonymous ----

    @Test
    void everyAdminScreenRequiresLogin() throws Exception {
        for (String url : new String[] {"/admin", "/admin/pages", "/admin/products", "/admin/products/1", "/admin/members",
                "/admin/messages", "/admin/media", "/admin/footer", "/admin/settings", "/admin/members/export"}) {
            mvc.perform(get(url)).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/admin/login"));
        }
    }

    @Test
    void anonymousCannotChangeData() throws Exception {
        mvc.perform(post("/admin/products/1/delete").with(csrf())).andExpect(status().is3xxRedirection());
        assertThat(count("select count(*) from products where id = 1 and deleted_at is null")).isEqualTo(1);
    }

    @Test
    void loginPageIsPublic() throws Exception {
        mvc.perform(get("/admin/login")).andExpect(status().isOk())
                .andExpect(content().string(containsString("เข้าสู่ระบบผู้ดูแล")));
    }

    // ---- login and lockout ----

    @Test
    void correctPasswordLogsInAndRecordsLastLogin() throws Exception {
        mvc.perform(formLogin("/admin/login").userParameter("email").user(OWNER_EMAIL).password(PASSWORD))
                .andExpect(redirectedUrl("/admin"));
        assertThat(count("select count(*) from admins where email = ? and last_login_at is not null", OWNER_EMAIL)).isEqualTo(1);
    }

    @Test
    void wrongPasswordShowsGenericError() throws Exception {
        mvc.perform(formLogin("/admin/login").userParameter("email").user(OWNER_EMAIL).password("nope"))
                .andExpect(redirectedUrl("/admin/login?error"));
        mvc.perform(formLogin("/admin/login").userParameter("email").user("nobody@test.local").password("nope"))
                .andExpect(redirectedUrl("/admin/login?error"));
    }

    @Test
    void fiveWrongPasswordsLockTheAccount() throws Exception {
        for (int i = 0; i < 4; i++) {
            mvc.perform(formLogin("/admin/login").userParameter("email").user(EDITOR_EMAIL).password("wrong" + i))
                    .andExpect(redirectedUrl("/admin/login?error"));
        }
        mvc.perform(formLogin("/admin/login").userParameter("email").user(EDITOR_EMAIL).password("wrong5"))
                .andExpect(redirectedUrl("/admin/login?locked"));
        // even the right password is refused while locked
        mvc.perform(formLogin("/admin/login").userParameter("email").user(EDITOR_EMAIL).password(PASSWORD))
                .andExpect(redirectedUrl("/admin/login?locked"));
        assertThat(count("select count(*) from admins where email = ? and locked_until > now()", EDITOR_EMAIL)).isEqualTo(1);
    }

    @Test
    void deactivatedAdminCannotLogIn() throws Exception {
        jdbc.update("update admins set is_active = false where email = ?", EDITOR_EMAIL);
        mvc.perform(formLogin("/admin/login").userParameter("email").user(EDITOR_EMAIL).password(PASSWORD))
                .andExpect(redirectedUrl("/admin/login?error"));
    }

    @Test
    void loginAttemptsAreRateLimitedPerIp() throws Exception {
        for (int i = 0; i < 20; i++) {
            mvc.perform(post("/admin/login").with(csrf()).param("email", "x" + i + "@test.local").param("password", "x")
                    .with(r -> { r.setRemoteAddr("10.7.7.7"); return r; }));
        }
        mvc.perform(post("/admin/login").with(csrf()).param("email", OWNER_EMAIL).param("password", PASSWORD)
                        .with(r -> { r.setRemoteAddr("10.7.7.7"); return r; }))
                .andExpect(redirectedUrl("/admin/login?throttled"));
    }

    // ---- owner vs editor ----

    @Test
    void editorCanEditContentAndProducts() throws Exception {
        mvc.perform(get("/admin").with(asEditor())).andExpect(status().isOk());
        mvc.perform(get("/admin/pages").with(asEditor())).andExpect(status().isOk());
        mvc.perform(post("/admin/products/1/visibility").with(asEditor()).with(csrf()).param("visible", "false"))
                .andExpect(status().is3xxRedirection());
        assertThat(count("select count(*) from products where id = 1 and not is_visible")).isEqualTo(1);
        mvc.perform(post("/admin/pages/home/sections/hero").with(asEditor()).with(csrf())
                        .param("f_headline", "พาดหัวใหม่").param("mediaIds", "3"))
                .andExpect(status().is3xxRedirection());
        mvc.perform(get("/")).andExpect(content().string(containsString("พาดหัวใหม่")));
    }

    @Test
    void editorCannotDownloadMembersOrManageAdmins() throws Exception {
        mvc.perform(get("/admin/members/export").with(asEditor())).andExpect(status().isForbidden());
        mvc.perform(post("/admin/settings/admins/invite").with(asEditor()).with(csrf()).param("email", "new@test.local"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/admin/members/1/delete").with(asEditor()).with(csrf())).andExpect(status().isForbidden());
        assertThat(count("select count(*) from admin_invites")).isZero();
        // the screens do not offer what the editor cannot do
        mvc.perform(get("/admin/members").with(asEditor()))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("ดาวน์โหลด CSV</a>"))));
    }

    @Test
    void ownerDownloadsMembersAsExcelFriendlyCsv() throws Exception {
        mvc.perform(post("/signup").with(csrf()).param("name", "สมชาย").param("email", "somchai@example.com"));
        byte[] body = mvc.perform(get("/admin/members/export").with(asOwner()))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", containsString("text/csv")))
                .andExpect(header().string("Content-Disposition", containsString("attachment")))
                .andReturn().getResponse().getContentAsByteArray();
        assertThat(body[0]).isEqualTo((byte) 0xEF);
        assertThat(body[1]).isEqualTo((byte) 0xBB);
        assertThat(body[2]).isEqualTo((byte) 0xBF);
        String csv = new String(body, 3, body.length - 3, StandardCharsets.UTF_8);
        assertThat(csv).startsWith("\"ชื่อ\",\"อีเมล\"").contains("\"สมชาย\",\"somchai@example.com\"");
    }

    @Test
    void ownerCanInviteAnEditor() throws Exception {
        mvc.perform(post("/admin/settings/admins/invite").with(asOwner()).with(csrf()).param("email", "new@test.local"))
                .andExpect(redirectedUrl("/admin/settings"));
        assertThat(count("select count(*) from admin_invites where email = 'new@test.local' and role = 'editor'")).isEqualTo(1);
    }

    @Test
    void adminPostsRequireCsrf() throws Exception {
        mvc.perform(post("/admin/products/1/delete").with(asOwner())).andExpect(status().isForbidden());
        assertThat(count("select count(*) from products where id = 1 and deleted_at is null")).isEqualTo(1);
    }

    @Test
    void deleteIsSoftAndHidesTheProductAtOnce() throws Exception {
        mvc.perform(post("/admin/products/1/delete").with(asOwner()).with(csrf())).andExpect(status().is3xxRedirection());
        assertThat(count("select count(*) from products where id = 1 and deleted_at is not null")).isEqualTo(1);
        mvc.perform(get("/")).andExpect(content().string(org.hamcrest.Matchers.not(containsString("กระเป๋าถือหนังสีแดงเบอร์กันดี"))));
    }

    @Test
    void everyChangeIsWrittenToTheAuditLog() throws Exception {
        long before = count("select count(*) from audit_logs where admin_email = ?", EDITOR_EMAIL);
        mvc.perform(post("/admin/products/2/visibility").with(asEditor()).with(csrf()).param("visible", "false"));
        assertThat(count("select count(*) from audit_logs where admin_email = ? and action = 'product.hide' and entity_id = 2",
                EDITOR_EMAIL)).isEqualTo(1);
        assertThat(count("select count(*) from audit_logs where admin_email = ?", EDITOR_EMAIL)).isEqualTo(before + 1);
    }

    @Test
    void responsesCarrySecurityHeaders() throws Exception {
        mvc.perform(get("/"))
                .andExpect(header().string("Content-Security-Policy", containsString("script-src 'self'")))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"));
    }
}
