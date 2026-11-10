package com.bakban9.web.pub;

import com.bakban9.service.CatalogService;
import com.bakban9.service.ContentService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class HomeController {

    static final int DEFAULT_PER_PAGE = 6;

    private final ContentService content;
    private final CatalogService catalog;

    public HomeController(ContentService content, CatalogService catalog) {
        this.content = content;
        this.catalog = catalog;
    }

    @GetMapping("/")
    public String home(@RequestParam(name = "brand", defaultValue = "") String brand,
                       @RequestParam(name = "page", defaultValue = "1") String page,
                       Model model) {
        ContentService.PageView view = content.load("home");
        int perPage = parseInt(view.get("products").text("perPage"), DEFAULT_PER_PAGE);
        CatalogService.CatalogPage cat = catalog.search(brand, parseInt(page, 1), perPage);
        model.addAttribute("view", view);
        model.addAttribute("catalog", cat);
        model.addAttribute("catalogJson", CatalogJson.of(cat.all()));
        model.addAttribute("pager", Pager.of(cat));
        model.addAttribute("activeNav", "products");
        if (!model.containsAttribute("signupForm")) {
            model.addAttribute("signupForm", new SignupController.SignupForm());
        }
        return "public/home";
    }

    public record PageLink(int num, String url, boolean current) {}

    public record Pager(java.util.List<PageLink> pages, String prev, String next) {
        static Pager of(CatalogService.CatalogPage c) {
            java.util.List<PageLink> links = new java.util.ArrayList<>();
            for (int i = 1; i <= c.pageCount(); i++) {
                links.add(new PageLink(i, url(c.brand(), i), i == c.page()));
            }
            return new Pager(links, url(c.brand(), c.page() - 1), url(c.brand(), c.page() + 1));
        }

        static String url(String brand, int page) {
            StringBuilder sb = new StringBuilder("/?");
            if (brand != null && !brand.isBlank()) {
                sb.append("brand=").append(java.net.URLEncoder.encode(brand, java.nio.charset.StandardCharsets.UTF_8)).append('&');
            }
            return sb.append("page=").append(page).append("#products").toString();
        }
    }

    static int parseInt(String s, int fallback) {
        try {
            int v = Integer.parseInt(s == null ? "" : s.trim());
            return v > 0 ? v : fallback;
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
