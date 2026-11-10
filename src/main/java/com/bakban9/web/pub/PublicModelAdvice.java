package com.bakban9.web.pub;

import com.bakban9.domain.PageSectionRepository;
import com.bakban9.service.SectionDefs;
import com.bakban9.service.SettingsService;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/** Header and Footer data for every public page — read from the database on each request. */
@ControllerAdvice(basePackageClasses = PublicModelAdvice.class)
public class PublicModelAdvice {

    private final SettingsService settings;
    private final PageSectionRepository sections;

    public PublicModelAdvice(SettingsService settings, PageSectionRepository sections) {
        this.settings = settings;
        this.sections = sections;
    }

    @ModelAttribute("site")
    public SiteView site() {
        return SiteView.of(settings.get());
    }

    @ModelAttribute("menu")
    public Map<String, String> menu() {
        Map<String, String> m = new LinkedHashMap<>(SectionDefs.section("home", "menu").orElseThrow().defaults());
        sections.findByPageSlugAndKey("home", "menu").ifPresent(s -> s.getContent().forEach((k, v) -> {
            if (v != null && !v.isBlank()) {
                m.put(k, v);
            }
        }));
        return m;
    }
}
