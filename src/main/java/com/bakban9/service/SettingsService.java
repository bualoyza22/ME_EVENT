package com.bakban9.service;

import com.bakban9.domain.Media;
import com.bakban9.domain.MediaRepository;
import com.bakban9.domain.SiteSettings;
import com.bakban9.domain.SiteSettingsRepository;
import com.bakban9.util.MapEmbed;
import com.bakban9.util.UrlRules;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SettingsService {

    private final SiteSettingsRepository repo;
    private final MediaRepository media;
    private final AuditService audit;

    public SettingsService(SiteSettingsRepository repo, MediaRepository media, AuditService audit) {
        this.repo = repo;
        this.media = media;
        this.audit = audit;
    }

    /** Read path: never writes, so a request during first-run seeding cannot create an empty row. */
    @Transactional(readOnly = true)
    public SiteSettings get() {
        return repo.findSingleton().orElseGet(SiteSettings::new);
    }

    private SiteSettings editable() {
        return repo.findSingleton().orElseGet(() -> repo.save(new SiteSettings()));
    }

    public record FooterForm(String tagline, String website, String address, String hours, String tel, String email,
                             String instagramUrl, String lineUrl, String facebookUrl, String tiktokUrl) {}

    /** Validates the Footer form; returns field → error message (empty = valid). */
    public Map<String, String> validateFooter(FooterForm f) {
        Map<String, String> errors = new LinkedHashMap<>();
        if (!UrlRules.isValidEmail(f.email()) && !blank(f.email())) {
            errors.put("email", "รูปแบบอีเมลไม่ถูกต้อง");
        }
        if (!UrlRules.isValidPhone(f.tel())) {
            errors.put("tel", "เบอร์โทรใส่ได้เฉพาะตัวเลข + - และเว้นวรรค");
        }
        checkHttps(errors, "instagramUrl", f.instagramUrl());
        checkHttps(errors, "lineUrl", f.lineUrl());
        checkHttps(errors, "facebookUrl", f.facebookUrl());
        checkHttps(errors, "tiktokUrl", f.tiktokUrl());
        if (!blank(f.website()) && !f.website().trim().matches("(?i)(https?://)?[a-z0-9.-]+\\.[a-z]{2,}(/\\S*)?")) {
            errors.put("website", "ใส่โดเมน เช่น www.example.com");
        }
        return errors;
    }

    @Transactional
    public void saveFooter(FooterForm f) {
        SiteSettings s = editable();
        s.setTagline(f.tagline());
        s.setWebsite(f.website());
        s.setAddress(f.address());
        s.setHours(f.hours());
        s.setTel(f.tel());
        s.setEmail(f.email());
        s.setInstagramUrl(f.instagramUrl());
        s.setLineUrl(f.lineUrl());
        s.setFacebookUrl(f.facebookUrl());
        s.setTiktokUrl(f.tiktokUrl());
        audit.log("footer.update", "site_settings", s.getId(), "แก้ข้อมูล Footer");
    }

    @Transactional
    public void saveMapUrl(String raw) {
        SiteSettings s = editable();
        s.setMapUrl(MapEmbed.normalizeInput(raw));
        audit.log("map.update", "site_settings", s.getId(), "แก้แผนที่หน้าติดต่อเรา");
    }

    @Transactional
    public void saveSite(String siteName, Long logoMediaId) {
        SiteSettings s = editable();
        s.setSiteName(siteName);
        if (logoMediaId != null) {
            Media logo = media.findActiveById(logoMediaId).orElseThrow(NotFoundException::new);
            s.setLogo(logo);
        }
        audit.log("settings.update", "site_settings", s.getId(), "ชื่อเว็บ/โลโก้");
    }

    private static void checkHttps(Map<String, String> errors, String field, String value) {
        if (!blank(value) && !UrlRules.isHttpsUrl(value)) {
            errors.put(field, "ต้องเป็นลิงก์ที่ขึ้นต้นด้วย https://");
        }
    }

    private static boolean blank(String s) {
        return s == null || s.isBlank();
    }
}
