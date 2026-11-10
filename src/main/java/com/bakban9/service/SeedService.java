package com.bakban9.service;

import com.bakban9.config.AppProperties;
import com.bakban9.domain.Media;
import com.bakban9.domain.SiteSettings;
import com.bakban9.domain.SiteSettingsRepository;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * First-run data from the spec and the Canvas prototype: shop info, 3 pages, 9 products.
 * Values the shop has not confirmed yet (prices, social links, map, phone, domain, 3 brands,
 * condition/size/accessories) are left empty for the admin to fill in — no placeholders.
 */
@Component
@Order(10)
public class SeedService implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SeedService.class);

    /** name, brand ("" = not confirmed yet), seed image file */
    record SeedProduct(String name, String brand, String image) {}

    static final List<SeedProduct> PRODUCTS = List.of(
            new SeedProduct("กระเป๋าถือหนังสีแดงเบอร์กันดี", "", "bag-burgundy-top-handle.jpg"),
            new SeedProduct("กระเป๋าทรงหมอนลายโมโนแกรมสีน้ำตาล", "Louis Vuitton", "bag-brown-monogram-duffle.jpg"),
            new SeedProduct("กระเป๋าถือหนังเย็บลายสีชมพู", "Dior", "bag-pink-quilted.jpg"),
            new SeedProduct("กระเป๋าถือลายโมโนแกรมสีกรมท่า", "Dior", "bag-navy-monogram.jpg"),
            new SeedProduct("กระเป๋าทรงอานม้าสีเบจ", "Dior", "bag-beige-saddle.jpg"),
            new SeedProduct("กระเป๋าใบเล็กสีฟ้าพาสเทล", "Jacquemus", "bag-sky-blue-mini.jpg"),
            new SeedProduct("กระเป๋าทรงหมอนลายโมโนแกรมลายเส้น", "Louis Vuitton", "bag-brown-monogram-drawn.jpg"),
            new SeedProduct("กระเป๋าสะพายทรงพระจันทร์สีดำ", "", "bag-black-crescent.jpg"),
            new SeedProduct("กระเป๋าสะพายสีดำห้อยตุ๊กตา", "", "bag-black-plush-charm.jpg"));

    private final AppProperties props;
    private final SiteSettingsRepository settingsRepo;
    private final ContentService content;
    private final MediaService media;
    private final ProductService products;
    private final AdminAccountService accounts;

    public SeedService(AppProperties props, SiteSettingsRepository settingsRepo, ContentService content,
                       MediaService media, ProductService products, AdminAccountService accounts) {
        this.props = props;
        this.settingsRepo = settingsRepo;
        this.content = content;
        this.media = media;
        this.products = products;
        this.accounts = accounts;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        content.ensureStructure();
        if (accounts.bootstrapOwner()) {
            log.info("created owner account {}", props.owner().email());
        }
        if (props.seed().enabled() && settingsRepo.count() == 0) {
            seed();
            log.info("seeded shop data: {} products", PRODUCTS.size());
        }
    }

    @Transactional
    public void seed() throws MediaService.UploadException {
        Media logo = upload("bakban9-logo.png", Media.KIND_LOGO, "โลโก้ BAKBAN9");
        upload("bakban-logo.png", Media.KIND_LOGO, "โลโก้ BAKBAN");

        Map<String, Media> images = new LinkedHashMap<>();
        int order = 0;
        for (SeedProduct sp : PRODUCTS) {
            Media img = upload(sp.image(), Media.KIND_PRODUCT, sp.name());
            images.put(sp.image(), img);
            ProductService.ProductForm f = new ProductService.ProductForm();
            f.setName(sp.name());
            f.setBrand(sp.brand());
            f.setSortOrder(++order);
            f.setVisible(true);
            products.save(null, f, List.of(img));
        }

        SiteSettings s = new SiteSettings();
        s.setSiteName("BAKBAN แบกบ้าน");
        s.setLogo(logo);
        s.setTagline("กระเป๋ามือสองสุดคิ้วท์ แบกความสดใสไปกับคุณ");
        s.setAddress("True Digital Park 101 ถ. สุขุมวิท แขวงบางจาก เขตพระโขนง กรุงเทพมหานคร 10260");
        s.setHours("Tue – Sun · 10:00 – 19:00\n(Close on Mondays)");
        s.setEmail("BAKBAN99@gmail.com");
        // website, tel, social links and map stay empty until the shop confirms them
        settingsRepo.save(s);

        content.setSectionMedia("home", "hero", List.of(
                images.get("bag-sky-blue-mini.jpg").getId(),
                images.get("bag-beige-saddle.jpg").getId(),
                images.get("bag-black-crescent.jpg").getId()));
        content.setSectionMedia("signup", "image", List.of(images.get("bag-sky-blue-mini.jpg").getId()));
    }

    private Media upload(String file, String kind, String alt) throws MediaService.UploadException {
        try (InputStream in = new ClassPathResource("seed/" + file).getInputStream()) {
            return media.upload(file, in.readAllBytes(), kind, alt);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
