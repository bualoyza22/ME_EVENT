package com.bakban9.util;

import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UtilTest {

    @Test
    void prices() {
        assertThat(Prices.parse("")).isInstanceOf(Prices.Empty.class);
        assertThat(Prices.parse(" 1,290 ")).isEqualTo(new Prices.Valid(1290));
        assertThat(Prices.parse("12.5")).isInstanceOf(Prices.Invalid.class);
        assertThat(Prices.parse("-5")).isInstanceOf(Prices.Invalid.class);
        assertThat(Prices.parse("พันห้า")).isInstanceOf(Prices.Invalid.class);
        assertThat(Prices.parse("99999999")).isInstanceOf(Prices.Invalid.class);
        assertThat(Texts.formatPrice(null)).isEqualTo("สอบถามราคา");
        assertThat(Texts.formatPrice(30000)).isEqualTo("30,000 บาท");
    }

    @Test
    void likePatternEscapesWildcards() {
        assertThat(Texts.containsPattern(" Dior ")).isEqualTo("%dior%");
        assertThat(Texts.containsPattern("50%_x\\")).isEqualTo("%50\\%\\_x\\\\%");
    }

    @Test
    void urls() {
        assertThat(UrlRules.isHttpsUrl("https://instagram.com/bakban")).isTrue();
        assertThat(UrlRules.isHttpsUrl("http://instagram.com/bakban")).isFalse();
        assertThat(UrlRules.isHttpsUrl("javascript:alert(1)")).isFalse();
        assertThat(UrlRules.websiteHref("www.example.com")).isEqualTo("https://www.example.com");
        assertThat(UrlRules.telHref("+66 81-234-5678")).isEqualTo("tel:+66812345678");
        assertThat(UrlRules.isValidEmail("a@b.co")).isTrue();
        assertThat(UrlRules.isValidEmail("a@b")).isFalse();
    }

    @Test
    void mapEmbed() {
        assertThat(MapEmbed.resolve("").isEmpty()).isTrue();
        assertThat(MapEmbed.resolve("https://www.google.com/maps/embed?pb=x").embedUrl()).isEqualTo("https://www.google.com/maps/embed?pb=x");
        assertThat(MapEmbed.resolve("<iframe src=\"https://www.google.com/maps/embed?pb=1&amp;b=2\"></iframe>").embedUrl())
                .isEqualTo("https://www.google.com/maps/embed?pb=1&b=2");
        MapEmbed.Result share = MapEmbed.resolve("https://maps.app.goo.gl/abc");
        assertThat(share.embedUrl()).isNull();
        assertThat(share.linkUrl()).isEqualTo("https://maps.app.goo.gl/abc");
        assertThat(MapEmbed.resolve("https://evil.example/maps/embed").isEmpty()).isTrue();
        assertThat(MapEmbed.isAcceptable("https://evil.example")).isFalse();
    }

    @Test
    void csvHasBomQuotesAndFormulaGuard() {
        byte[] b = Csv.toBytes(List.of(List.of("ชื่อ", "x"), List.of("=HYPERLINK(\"a\")", "say \"hi\"")));
        assertThat(b[0]).isEqualTo((byte) 0xEF);
        String s = new String(b, 3, b.length - 3, StandardCharsets.UTF_8);
        assertThat(s).isEqualTo("\"ชื่อ\",\"x\"\r\n\"'=HYPERLINK(\"\"a\"\")\",\"say \"\"hi\"\"\"\r\n");
    }
}
