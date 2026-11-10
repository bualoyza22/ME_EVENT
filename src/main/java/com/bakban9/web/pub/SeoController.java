package com.bakban9.web.pub;

import com.bakban9.config.AppProperties;
import com.bakban9.service.ContentService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SeoController {

    private final AppProperties props;
    private final ContentService content;

    public SeoController(AppProperties props, ContentService content) {
        this.props = props;
        this.content = content;
    }

    @GetMapping(value = "/robots.txt", produces = MediaType.TEXT_PLAIN_VALUE)
    public String robots() {
        return "User-agent: *\nDisallow: /admin\nSitemap: " + props.baseUrl() + "/sitemap.xml\n";
    }

    @GetMapping(value = "/sitemap.xml", produces = MediaType.APPLICATION_XML_VALUE)
    public String sitemap() {
        StringBuilder sb = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");
        content.allPages().stream().filter(p -> p.isPublished()).forEach(p -> sb.append("  <url><loc>")
                .append(props.baseUrl()).append("home".equals(p.getSlug()) ? "/" : "/" + p.getSlug())
                .append("</loc></url>\n"));
        return sb.append("</urlset>\n").toString();
    }
}
