package com.bakban9.storage;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StorageTest {

    @Test
    void detectsTypeFromBytes() {
        assertThat(ImageType.detect(new byte[] {(byte) 0x89, 'P', 'N', 'G', 13, 10, 26, 10, 0, 0, 0, 0})).contains(ImageType.PNG);
        assertThat(ImageType.detect(new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0, 0, 0, 0, 0, 0, 0, 0, 0})).contains(ImageType.JPEG);
        assertThat(ImageType.detect("RIFF0000WEBPVP8 ".getBytes(StandardCharsets.US_ASCII))).contains(ImageType.WEBP);
        assertThat(ImageType.detect("<?xml version=\"1.0\"?><svg></svg>".getBytes(StandardCharsets.UTF_8))).contains(ImageType.SVG);
        assertThat(ImageType.detect("<html><body>not an image</body></html>".getBytes(StandardCharsets.UTF_8))).isEmpty();
    }

    @Test
    void svgSanitizerRemovesActiveContent() throws Exception {
        String svg = "<svg xmlns=\"http://www.w3.org/2000/svg\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" onload=\"x()\">"
                + "<script>alert(1)</script><foreignObject><div>hi</div></foreignObject>"
                + "<a xlink:href=\"javascript:alert(1)\"><rect width=\"1\" height=\"1\"/></a>"
                + "<use href=\"#ok\"/><image href=\"https://evil.example/x.png\"/>"
                + "<set attributeName=\"href\" to=\"javascript:alert(1)\"/>"
                + "<rect style=\"fill:url(https://evil.example)\"/><circle id=\"ok\" r=\"2\"/></svg>";
        String out = new String(SvgSanitizer.sanitize(svg.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8);
        assertThat(out).doesNotContain("script").doesNotContain("onload").doesNotContain("foreignObject")
                .doesNotContain("javascript").doesNotContain("evil.example").doesNotContain("<set")
                .contains("href=\"#ok\"").contains("circle");
    }

    @Test
    void svgWithDoctypeIsRejected() {
        String xxe = "<?xml version=\"1.0\"?><!DOCTYPE svg [<!ENTITY x SYSTEM \"file:///etc/passwd\">]><svg>&x;</svg>";
        assertThatThrownBy(() -> SvgSanitizer.sanitize(xxe.getBytes(StandardCharsets.UTF_8)))
                .isInstanceOf(SvgSanitizer.InvalidSvgException.class);
    }

    @Test
    void oversizedUploadIsRejected() {
        byte[] big = new byte[(int) ImageProcessor.MAX_BYTES + 1];
        big[0] = (byte) 0xFF; big[1] = (byte) 0xD8; big[2] = (byte) 0xFF;
        assertThatThrownBy(() -> new ImageProcessor().process(big)).hasMessageContaining("5 MB");
    }
}
