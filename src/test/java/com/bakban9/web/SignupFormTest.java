package com.bakban9.web;

import com.bakban9.support.IntegrationTest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SignupFormTest extends IntegrationTest {

    @Test
    void validSignupCreatesMemberWithConsentTime() throws Exception {
        mvc.perform(post("/signup").with(csrf())
                        .param("name", "มะลิ").param("email", "Mali@Example.com").param("phone", "081-234-5678")
                        .param("consent", "true").param("source", "signup"))
                .andExpect(redirectedUrl("/signup"))
                .andExpect(flash().attribute("signupResult", "created"));

        assertThat(count("select count(*) from members where email = 'mali@example.com' and phone = '081-234-5678'"
                + " and consent_marketing and consent_at is not null")).isEqualTo(1);
    }

    @Test
    void consentIsNotGivenUnlessTicked() throws Exception {
        mvc.perform(post("/signup").with(csrf()).param("name", "ต้น").param("email", "ton@example.com"))
                .andExpect(flash().attribute("signupResult", "created"));
        assertThat(count("select count(*) from members where not consent_marketing and consent_at is null")).isEqualTo(1);
    }

    @Test
    void duplicateEmailIsReportedAndNotStoredTwice() throws Exception {
        mvc.perform(post("/signup").with(csrf()).param("name", "A").param("email", "dup@example.com"));
        mvc.perform(post("/signup").with(csrf()).param("name", "B").param("email", "DUP@example.com"))
                .andExpect(flash().attribute("signupResult", "duplicate"));

        assertThat(count("select count(*) from members")).isEqualTo(1);
    }

    @Test
    void invalidEmailOrMissingNameIsRejected() throws Exception {
        mvc.perform(post("/signup").with(csrf()).param("name", "A").param("email", "not-an-email"))
                .andExpect(redirectedUrl("/signup"))
                .andExpect(flash().attributeExists("signupErrors"))
                .andExpect(flash().attribute("signupError", "ขอชื่อกับอีเมลที่ถูกต้องก่อนน้า"));
        mvc.perform(post("/signup").with(csrf()).param("name", "  ").param("email", "ok@example.com"))
                .andExpect(flash().attributeExists("signupErrors"));

        assertThat(count("select count(*) from members")).isZero();
    }

    @Test
    void invalidPhoneIsRejectedWhenPhoneFieldIsShown() throws Exception {
        mvc.perform(post("/signup").with(csrf()).param("name", "A").param("email", "a@example.com").param("phone", "call me"))
                .andExpect(flash().attributeExists("signupErrors"));
        assertThat(count("select count(*) from members")).isZero();
    }

    @Test
    void phoneIsIgnoredWhenAdminTurnedItOff() throws Exception {
        jdbc.update("update page_sections set content = jsonb_set(content, '{showPhone}', '\"false\"') where key = 'form'"
                + " and page_id = (select id from pages where slug = 'signup')");
        try {
            mvc.perform(get("/signup")).andExpect(content().string(org.hamcrest.Matchers.not(
                    org.hamcrest.Matchers.containsString("id=\"su-phone\""))));
            mvc.perform(post("/signup").with(csrf()).param("name", "A").param("email", "a@example.com").param("phone", "0812345678"))
                    .andExpect(flash().attribute("signupResult", "created"));
            assertThat(count("select count(*) from members where phone = ''")).isEqualTo(1);
        } finally {
            jdbc.update("update page_sections set content = jsonb_set(content, '{showPhone}', '\"true\"') where key = 'form'"
                    + " and page_id = (select id from pages where slug = 'signup')");
        }
    }

    @Test
    void homePageShortFormUsesTheSameMembersTable() throws Exception {
        mvc.perform(post("/signup").with(csrf()).param("name", "บี").param("email", "bee@example.com")
                        .param("source", "home").param("phone", "0812345678").param("consent", "true"))
                .andExpect(redirectedUrl("/#signup"))
                .andExpect(flash().attribute("signupResult", "created"));
        // the short form has only name + email: no phone, no marketing consent
        assertThat(count("select count(*) from members where email = 'bee@example.com' and phone = '' and not consent_marketing"))
                .isEqualTo(1);
    }

    @Test
    void honeypotSubmissionLooksSuccessfulButStoresNothing() throws Exception {
        mvc.perform(post("/signup").with(csrf()).param("name", "bot").param("email", "bot@example.com")
                        .param("company", "Spam Inc"))
                .andExpect(flash().attribute("signupResult", "created"));
        assertThat(count("select count(*) from members")).isZero();
    }

    @Test
    void submissionsAreRateLimitedPerIp() throws Exception {
        // test profile: 3 submissions per window
        for (int i = 0; i < 3; i++) {
            mvc.perform(post("/signup").with(csrf()).param("name", "N" + i).param("email", "n" + i + "@example.com")
                    .with(r -> { r.setRemoteAddr("10.9.9.9"); return r; }));
        }
        mvc.perform(post("/signup").with(csrf()).param("name", "N4").param("email", "n4@example.com")
                        .with(r -> { r.setRemoteAddr("10.9.9.9"); return r; }))
                .andExpect(flash().attributeExists("signupError"));
        assertThat(count("select count(*) from members")).isEqualTo(3);

        // another visitor is not affected
        mvc.perform(post("/signup").with(csrf()).param("name", "Other").param("email", "other@example.com")
                        .with(r -> { r.setRemoteAddr("10.1.1.1"); return r; }))
                .andExpect(flash().attribute("signupResult", "created"));
    }

    @Test
    void postWithoutCsrfTokenIsRefused() throws Exception {
        mvc.perform(post("/signup").param("name", "A").param("email", "a@example.com"))
                .andExpect(status().isForbidden());
        assertThat(count("select count(*) from members")).isZero();
    }

    @Test
    void thankYouMessageIsShownAfterRedirect() throws Exception {
        mvc.perform(get("/signup").flashAttr("signupResult", "created"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("ขอบคุณที่สมัครนะ!")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("ไปช้อปเลย")));
    }
}
