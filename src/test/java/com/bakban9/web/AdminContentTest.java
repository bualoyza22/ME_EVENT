package com.bakban9.web;

import com.bakban9.support.IntegrationTest;
import java.io.ByteArrayOutputStream;
import java.awt.image.BufferedImage;
import java.nio.charset.StandardCharsets;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Admin edits reach the public site right away; bad input is refused with a status message. */
class AdminContentTest extends IntegrationTest {

    @Test
    void priceMustBeANumber() throws Exception {
        mvc.perform(multipart("/admin/products/1").with(asEditor()).with(csrf())
                        .param("name", "กระเป๋าถือหนังสีแดงเบอร์กันดี").param("price", "ประมาณพัน").param("imageIds", "3"))
                .andExpect(flash().attribute("statusKind", "error"))
                .andExpect(flash().attribute("status", containsString("ราคาต้องเป็นตัวเลข")));
        assertThat(count("select count(*) from products where id = 1 and price_thb is null")).isEqualTo(1);
    }

    @Test
    void savedPriceAppearsOnTheShop() throws Exception {
        long imageId = count("select media_id from product_images where product_id = 1 and position = 0");
        mvc.perform(multipart("/admin/products/1").with(asEditor()).with(csrf())
                        .param("name", "กระเป๋าถือหนังสีแดงเบอร์กันดี").param("price", "1,290")
                        .param("condition", "95%").param("visible", "true").param("_visible", "on")
                        .param("imageIds", String.valueOf(imageId)))
                .andExpect(flash().attribute("statusKind", "ok"));
        assertThat(count("select price_thb from products where id = 1")).isEqualTo(1290);
        mvc.perform(get("/")).andExpect(content().string(containsString("1,290 บาท")));
    }

    @Test
    void uncheckingVisibleHidesTheProduct() throws Exception {
        mvc.perform(multipart("/admin/products/1").with(asEditor()).with(csrf())
                .param("name", "กระเป๋าถือหนังสีแดงเบอร์กันดี").param("_visible", "on"));
        assertThat(count("select count(*) from products where id = 1 and not is_visible")).isEqualTo(1);
    }

    @Test
    void newProductWithUploadedImages() throws Exception {
        MockMultipartFile img = new MockMultipartFile("newImages", "side.png", "image/png", png(1200, 800));
        mvc.perform(multipart("/admin/products").file(img).with(asEditor()).with(csrf())
                        .param("name", "กระเป๋าใหม่").param("brand", "Chanel").param("visible", "true").param("_visible", "on"))
                .andExpect(status().is3xxRedirection());
        assertThat(count("select count(*) from products p join product_images pi on pi.product_id = p.id "
                + "where p.name = 'กระเป๋าใหม่' and pi.position = 0")).isEqualTo(1);
        // resized variants were made for a 1200px wide image
        assertThat(count("select count(*) from media where filename = 'side.png' and variant_urls ->> '400' is not null and variant_urls ->> '1000' is not null"))
                .isEqualTo(1);
        mvc.perform(get("/").param("brand", "chanel")).andExpect(content().string(containsString("กระเป๋าใหม่")));
    }

    @Test
    void uploadRejectsFilesThatAreNotImages() throws Exception {
        MockMultipartFile fake = new MockMultipartFile("files", "evil.png", "image/png",
                "<html><script>alert(1)</script></html>".getBytes(StandardCharsets.UTF_8));
        mvc.perform(multipart("/admin/media").file(fake).with(asEditor()).with(csrf()).param("kind", "other"))
                .andExpect(flash().attribute("statusKind", "error"));
        assertThat(count("select count(*) from media where filename = 'evil.png'")).isZero();
    }

    @Test
    void svgUploadIsSanitized() throws Exception {
        String svg = "<svg xmlns=\"http://www.w3.org/2000/svg\" onload=\"alert(1)\"><script>alert(2)</script>"
                + "<circle r=\"4\"/></svg>";
        MockMultipartFile f = new MockMultipartFile("files", "icon.svg", "image/svg+xml", svg.getBytes(StandardCharsets.UTF_8));
        mvc.perform(multipart("/admin/media").file(f).with(asEditor()).with(csrf()).param("kind", "other"))
                .andExpect(flash().attribute("statusKind", "ok"));
        String url = jdbc.queryForObject("select url from media where filename = 'icon.svg' order by id desc limit 1", String.class);
        String stored = mvc.perform(get(url)).andReturn().getResponse().getContentAsString();
        assertThat(stored).contains("circle").doesNotContain("script").doesNotContain("onload");
    }

    @Test
    void mediaInUseCannotBeDeleted() throws Exception {
        long logoId = count("select logo_media_id from site_settings");
        mvc.perform(post("/admin/media/" + logoId + "/delete").with(asOwner()).with(csrf()))
                .andExpect(flash().attribute("statusKind", "error"));
        assertThat(count("select count(*) from media where id = ? and deleted_at is null", logoId)).isEqualTo(1);
    }

    @Test
    void footerRejectsNonHttpsSocialLinks() throws Exception {
        mvc.perform(post("/admin/footer").with(asEditor()).with(csrf())
                        .param("email", "BAKBAN99@gmail.com").param("instagramUrl", "http://instagram.com/bakban"))
                .andExpect(flash().attribute("statusKind", "error"));
        assertThat(count("select count(*) from site_settings where instagram_url = ''")).isEqualTo(1);
    }

    @Test
    void footerChangesShowOnEveryPage() throws Exception {
        try {
            mvc.perform(post("/admin/footer").with(asEditor()).with(csrf())
                            .param("tagline", "สโลแกนทดสอบ").param("address", "ที่อยู่ทดสอบ").param("email", "shop@example.com")
                            .param("lineUrl", "https://line.me/R/ti/p/@bakban-test"))
                    .andExpect(flash().attribute("statusKind", "ok"));
            for (String page : new String[] {"/", "/signup", "/contact"}) {
                mvc.perform(get(page)).andExpect(content().string(containsString("สโลแกนทดสอบ")))
                        .andExpect(content().string(containsString("https://line.me/R/ti/p/@bakban-test")));
            }
        } finally {
            jdbc.update("update site_settings set tagline = 'กระเป๋ามือสองสุดคิ้วท์ แบกความสดใสไปกับคุณ', "
                    + "address = 'True Digital Park 101 ถ. สุขุมวิท แขวงบางจาก เขตพระโขนง กรุงเทพมหานคร 10260', "
                    + "hours = 'Tue – Sun · 10:00 – 19:00\n(Close on Mondays)', email = 'BAKBAN99@gmail.com', line_url = ''");
        }
    }

    @Test
    void hiddenSectionDisappearsFromHomePage() throws Exception {
        mvc.perform(post("/admin/pages/home/sections/signup/visibility").with(asEditor()).with(csrf()).param("visible", "false"));
        mvc.perform(get("/")).andExpect(content().string(org.hamcrest.Matchers.not(containsString("id=\"signup\""))));
    }

    @Test
    void sectionsCanBeReordered() throws Exception {
        mvc.perform(post("/admin/pages/home/sections/signup/move").with(asEditor()).with(csrf()).param("dir", "up"));
        String html = mvc.perform(get("/")).andReturn().getResponse().getContentAsString();
        assertThat(html.indexOf("id=\"signup\"")).isLessThan(html.indexOf("id=\"products\""));
        mvc.perform(post("/admin/pages/home/sections/signup/move").with(asEditor()).with(csrf()).param("dir", "down"));
    }

    @Test
    void mapLinkMustBeGoogleMaps() throws Exception {
        mvc.perform(post("/admin/pages/contact/sections/map").with(asEditor()).with(csrf()).param("mapUrl", "https://evil.example/map"))
                .andExpect(flash().attribute("statusKind", "error"));
        try {
            mvc.perform(post("/admin/pages/contact/sections/map").with(asEditor()).with(csrf())
                            .param("mapUrl", "<iframe src=\"https://www.google.com/maps/embed?pb=abc\" width=\"600\"></iframe>"))
                    .andExpect(flash().attribute("statusKind", "ok"));
            mvc.perform(get("/contact")).andExpect(content().string(containsString("src=\"https://www.google.com/maps/embed?pb=abc\"")));
        } finally {
            jdbc.update("update site_settings set map_url = ''");
        }
    }

    @Test
    void messageReplyIsMarked() throws Exception {
        mvc.perform(post("/contact").with(csrf()).param("name", "ฝน").param("email", "fon@example.com").param("message", "hi"));
        long id = count("select id from messages limit 1");
        mvc.perform(post("/admin/messages/" + id + "/reply").with(asEditor()).with(csrf()).param("body", "ยังอยู่ค่ะ"))
                .andExpect(flash().attribute("statusKind", "ok"));
        assertThat(count("select count(*) from messages where id = ? and status = 'replied' and reply_body = 'ยังอยู่ค่ะ'", id))
                .isEqualTo(1);
    }

    @Test
    void allAdminScreensRender() throws Exception {
        for (String url : new String[] {"/admin", "/admin/pages?page=home&section=hero", "/admin/pages?page=signup",
                "/admin/pages?page=contact&section=map", "/admin/products", "/admin/products/new", "/admin/products/1",
                "/admin/members", "/admin/messages", "/admin/media", "/admin/footer", "/admin/settings"}) {
            mvc.perform(get(url).with(asOwner())).andExpect(status().isOk())
                    .andExpect(content().string(containsString("role=\"status\"")));
        }
    }

    private static byte[] png(int w, int h) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB), "png", out);
        return out.toByteArray();
    }
}
