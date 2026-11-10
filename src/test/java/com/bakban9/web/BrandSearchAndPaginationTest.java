package com.bakban9.web;

import com.bakban9.domain.Product;
import com.bakban9.service.CatalogService;
import com.bakban9.service.CatalogService.CatalogPage;
import com.bakban9.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class BrandSearchAndPaginationTest extends IntegrationTest {

    @Autowired CatalogService catalog;

    // ---- brand search ----

    @Test
    void brandSearchIsCaseInsensitiveAndMatchesPartOfTheName() {
        assertThat(catalog.search("dior", 1, 6).total()).isEqualTo(3);
        assertThat(catalog.search("DIOR", 1, 6).total()).isEqualTo(3);
        assertThat(catalog.search("  vuit ", 1, 6).items()).extracting(Product::getBrand)
                .containsOnly("Louis Vuitton").hasSize(2);
        assertThat(catalog.search("Jacquemus", 1, 6).items()).extracting(Product::getName)
                .containsExactly("กระเป๋าใบเล็กสีฟ้าพาสเทล");
    }

    @Test
    void searchTreatsWildcardsLiterally() {
        assertThat(catalog.search("%", 1, 6).total()).isZero();
        assertThat(catalog.search("_", 1, 6).total()).isZero();
    }

    @Test
    void noMatchShowsTheEmptyMessage() throws Exception {
        mvc.perform(get("/").param("brand", "Chanel"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("ไม่เจอแบรนด์นี้ ลองคำอื่นดูนะ")))
                .andExpect(content().string(not(containsString("class=\"card product-card\""))));
    }

    @Test
    void searchFormStartsFromPageOne() throws Exception {
        // the search form submits only "brand", so every new search lands on page 1
        String html = mvc.perform(get("/").param("page", "2")).andReturn().getResponse().getContentAsString();
        String form = html.substring(html.indexOf("<form class=\"brand-search\""), html.indexOf("</form>", html.indexOf("brand-search")));
        assertThat(form).contains("name=\"brand\"").doesNotContain("name=\"page\"");

        CatalogPage p = catalog.search("dior", 1, 6);
        assertThat(p.page()).isEqualTo(1);
        assertThat(p.pageCount()).isEqualTo(1);
    }

    @Test
    void hiddenAndDeletedProductsAreNotFound() {
        jdbc.update("update products set is_visible = false where brand = 'Dior' and id = (select min(id) from products where brand = 'Dior')");
        jdbc.update("update products set deleted_at = now() where id = (select max(id) from products where brand = 'Dior')");
        assertThat(catalog.search("dior", 1, 6).total()).isEqualTo(1);
        assertThat(catalog.search("", 1, 6).total()).isEqualTo(7);
    }

    // ---- pagination ----

    @Test
    void nineProductsMakeTwoPagesOfSix() {
        CatalogPage p1 = catalog.search("", 1, 6);
        CatalogPage p2 = catalog.search("", 2, 6);
        assertThat(p1.total()).isEqualTo(9);
        assertThat(p1.pageCount()).isEqualTo(2);
        assertThat(p1.items()).hasSize(6);
        assertThat(p2.items()).hasSize(3);
        assertThat(p1.hasPrev()).isFalse();
        assertThat(p1.hasNext()).isTrue();
        assertThat(p2.hasNext()).isFalse();
        assertThat(p2.offset()).isEqualTo(6);
        // order follows sort_order; no product appears on both pages
        assertThat(p1.items()).doesNotContainAnyElementsOf(p2.items());
    }

    @Test
    void outOfRangePagesAreClamped() {
        assertThat(catalog.search("", 99, 6).page()).isEqualTo(2);
        assertThat(catalog.search("", -3, 6).page()).isEqualTo(1);
        assertThat(catalog.search("nothing", 5, 6).page()).isEqualTo(1);
    }

    @Test
    void pageTwoShowsTheRemainingThreeCards() throws Exception {
        MvcResult r = mvc.perform(get("/").param("page", "2")).andExpect(status().isOk()).andReturn();
        String html = r.getResponse().getContentAsString();
        assertThat(countOf(html, "class=\"card product-card\"")).isEqualTo(3);
        assertThat(html).contains("aria-label=\"หน้า 2\" aria-current=\"page\"");
        assertThat(html).contains("href=\"/?page=1#products\"");
    }

    @Test
    void invalidPageParameterFallsBackToPageOne() throws Exception {
        String html = mvc.perform(get("/").param("page", "abc")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(countOf(html, "class=\"card product-card\"")).isEqualTo(6);
    }

    @Test
    void paginationLinksKeepTheBrandFilter() throws Exception {
        jdbc.update("update page_sections set content = jsonb_set(content, '{perPage}', '\"1\"') where key = 'products'");
        String html = mvc.perform(get("/").param("brand", "louis vuitton")).andReturn().getResponse().getContentAsString();
        assertThat(countOf(html, "class=\"card product-card\"")).isEqualTo(1);
        assertThat(html).contains("href=\"/?brand=louis+vuitton&amp;page=2#products\"");
    }

    @Test
    void cardsPerPageComeFromTheAdminSetting() throws Exception {
        jdbc.update("update page_sections set content = jsonb_set(content, '{perPage}', '\"4\"') where key = 'products'");
        String html = mvc.perform(get("/")).andReturn().getResponse().getContentAsString();
        assertThat(countOf(html, "class=\"card product-card\"")).isEqualTo(4);
        assertThat(html).contains("aria-label=\"หน้า 3\"");
    }

    @Test
    void cardShowsPriceOnRequestWhenPriceIsEmpty() throws Exception {
        jdbc.update("update products set price_thb = 12900 where id = 1");
        String html = mvc.perform(get("/")).andReturn().getResponse().getContentAsString();
        assertThat(html).contains("12,900 บาท").contains("สอบถามราคา");
    }

    private static int countOf(String s, String needle) {
        int n = 0;
        for (int i = s.indexOf(needle); i >= 0; i = s.indexOf(needle, i + 1)) {
            n++;
        }
        return n;
    }
}
