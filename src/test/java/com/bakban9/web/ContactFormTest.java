package com.bakban9.web;

import com.bakban9.support.IntegrationTest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;

class ContactFormTest extends IntegrationTest {

    @Test
    void validMessageGoesToInboxAndNotifiesShop() throws Exception {
        mvc.perform(post("/contact").with(csrf()).param("name", "ฝน").param("email", "fon@example.com")
                        .param("subject", "สอบถามสภาพ").param("message", "ใบสีฟ้ายังอยู่ไหม"))
                .andExpect(redirectedUrl("/contact"))
                .andExpect(flash().attribute("contactSent", true));

        assertThat(count("select count(*) from messages where status = 'open' and subject = 'สอบถามสภาพ'")).isEqualTo(1);
        // shop email from the seeded Footer settings
        verify(mail).send(eq("BAKBAN99@gmail.com"), eq("fon@example.com"), contains("สอบถามสภาพ"), anyString());
    }

    @Test
    void nameEmailAndMessageAreRequiredButSubjectIsNot() throws Exception {
        mvc.perform(post("/contact").with(csrf()).param("name", "ฝน").param("email", "fon@example.com").param("message", " "))
                .andExpect(flash().attributeExists("contactErrors"));
        mvc.perform(post("/contact").with(csrf()).param("name", "").param("email", "fon@example.com").param("message", "hi"))
                .andExpect(flash().attributeExists("contactErrors"));
        mvc.perform(post("/contact").with(csrf()).param("name", "ฝน").param("email", "fon@").param("message", "hi"))
                .andExpect(flash().attributeExists("contactErrors"));
        assertThat(count("select count(*) from messages")).isZero();

        mvc.perform(post("/contact").with(csrf()).param("name", "ฝน").param("email", "fon@example.com").param("message", "hi"))
                .andExpect(flash().attribute("contactSent", true));
        assertThat(count("select count(*) from messages")).isEqualTo(1);
    }

    @Test
    void adminCanCloseTheForm() throws Exception {
        jdbc.update("update page_sections set is_visible = false where key = 'form'"
                + " and page_id = (select id from pages where slug = 'contact')");

        mvc.perform(get("/contact")).andExpect(content().string(not(containsString("ส่งข้อความถึงเรา"))));
        mvc.perform(post("/contact").with(csrf()).param("name", "ฝน").param("email", "fon@example.com").param("message", "hi"))
                .andExpect(flash().attributeExists("contactError"));
        assertThat(count("select count(*) from messages")).isZero();
        verify(mail, never()).send(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void honeypotIsDropped() throws Exception {
        mvc.perform(post("/contact").with(csrf()).param("name", "bot").param("email", "b@example.com")
                .param("message", "buy now").param("company", "x"));
        assertThat(count("select count(*) from messages")).isZero();
    }

    @Test
    void previewButtonPrefillsTheSubject() throws Exception {
        mvc.perform(get("/contact").param("product", "6"))
                .andExpect(content().string(containsString("สนใจ: กระเป๋าใบเล็กสีฟ้าพาสเทล")));
    }

    @Test
    void emptyShopValuesAreNotRenderedAsPlaceholders() throws Exception {
        // tel, socials and map are empty in the seed: nothing fake on the page
        mvc.perform(get("/contact"))
                .andExpect(content().string(not(containsString("tel:"))))
                .andExpect(content().string(not(containsString("Follow us"))))
                .andExpect(content().string(not(containsString("<iframe"))))
                .andExpect(content().string(not(containsString("90-000-0000"))));
    }
}
