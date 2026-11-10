package com.bakban9.service;

import com.bakban9.domain.Media;
import com.bakban9.domain.MediaRepository;
import com.bakban9.domain.Page;
import com.bakban9.domain.PageRepository;
import com.bakban9.domain.PageSection;
import com.bakban9.domain.PageSectionRepository;
import com.bakban9.service.SectionDefs.FieldDef;
import com.bakban9.service.SectionDefs.PageDef;
import com.bakban9.service.SectionDefs.SectionDef;
import com.bakban9.util.MapEmbed;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Page sections: read for the public pages, edited from admin "หน้าเว็บ". */
@Service
public class ContentService {

    private final PageRepository pages;
    private final PageSectionRepository sections;
    private final MediaRepository media;
    private final SettingsService settings;
    private final AuditService audit;

    public ContentService(PageRepository pages, PageSectionRepository sections, MediaRepository media,
                          SettingsService settings, AuditService audit) {
        this.pages = pages;
        this.sections = sections;
        this.media = media;
        this.settings = settings;
        this.audit = audit;
    }

    public record SectionView(String key, String label, boolean visible, boolean fixed, Map<String, String> content,
                              List<Media> media) {
        public String text(String field) {
            String v = content.get(field);
            return v == null ? "" : v;
        }

        public boolean flag(String field) {
            return "true".equals(content.get(field));
        }

        public Media image(int index) {
            return index < media.size() ? media.get(index) : null;
        }
    }

    public record PageView(Page page, List<SectionView> sections) {
        public SectionView get(String key) {
            return sections.stream().filter(s -> s.key().equals(key)).findFirst()
                    .orElseGet(() -> new SectionView(key, key, false, false, Map.of(), List.of()));
        }

        public List<SectionView> visible() {
            return sections.stream().filter(SectionView::visible).toList();
        }

        public boolean isVisible(String key) {
            return get(key).visible();
        }
    }

    @Transactional(readOnly = true)
    public PageView load(String slug) {
        Page page = pages.findBySlug(slug).orElseThrow(NotFoundException::new);
        List<PageSection> rows = sections.findByPageSlug(slug);
        List<Long> ids = rows.stream().flatMap(s -> s.getMediaIdList().stream()).distinct().toList();
        Map<Long, Media> byId = ids.isEmpty() ? Map.of()
                : media.findActiveByIds(ids).stream().collect(Collectors.toMap(Media::getId, Function.identity()));
        List<SectionView> views = new ArrayList<>();
        for (PageSection s : rows) {
            Optional<SectionDef> def = SectionDefs.section(slug, s.getKey());
            Map<String, String> content = new LinkedHashMap<>(def.map(SectionDef::defaults).orElse(Map.of()));
            content.putAll(s.getContent());
            List<Media> m = s.getMediaIdList().stream().map(byId::get).filter(x -> x != null).toList();
            views.add(new SectionView(s.getKey(), def.map(SectionDef::label).orElse(s.getKey()), s.isVisible(),
                    def.map(SectionDef::fixed).orElse(false), content, m));
        }
        return new PageView(page, views);
    }

    /** Creates pages and sections that are defined in SectionDefs but missing in the database. */
    @Transactional
    public void ensureStructure() {
        for (PageDef pd : SectionDefs.PAGES) {
            Page page = pages.findBySlug(pd.slug()).orElseGet(() -> pages.save(new Page(pd.slug(), pd.label())));
            int order = 0;
            for (SectionDef sd : pd.sections()) {
                order += 10;
                if (sections.findByPageSlugAndKey(pd.slug(), sd.key()).isEmpty()) {
                    PageSection s = new PageSection(page, sd.key(), order);
                    s.setContent(sd.defaults());
                    sections.save(s);
                }
            }
        }
    }

    @Transactional
    public void setSectionMedia(String slug, String key, List<Long> mediaIds) {
        PageSection s = sections.findByPageSlugAndKey(slug, key).orElseThrow(NotFoundException::new);
        s.setMediaIdList(mediaIds);
    }

    /** Validates and saves a section. Returns field → error (empty when saved). */
    @Transactional
    public Map<String, String> saveSection(String slug, String key, Map<String, String> input, List<Long> mediaIds,
                                           String mapUrl) {
        SectionDef def = SectionDefs.section(slug, key).orElseThrow(NotFoundException::new);
        PageSection s = sections.findByPageSlugAndKey(slug, key).orElseThrow(NotFoundException::new);
        Map<String, String> errors = new LinkedHashMap<>();
        Map<String, String> content = new LinkedHashMap<>(s.getContent());
        for (FieldDef f : def.fields()) {
            String v = input.getOrDefault(f.key(), "");
            v = v == null ? "" : v.trim();
            switch (f.type()) {
                case BOOLEAN -> v = "true".equals(v) || "on".equals(v) ? "true" : "false";
                case NUMBER -> {
                    if (!v.matches("\\d{1,2}") || Integer.parseInt(v) < 1 || Integer.parseInt(v) > 48) {
                        errors.put(f.key(), f.label() + " ต้องเป็นตัวเลข 1–48");
                    }
                }
                default -> {
                    if (v.length() > f.maxLength()) {
                        errors.put(f.key(), f.label() + " ยาวเกิน " + f.maxLength() + " ตัวอักษร");
                    }
                }
            }
            content.put(f.key(), v);
        }
        List<Long> ids = mediaIds == null ? List.of() : mediaIds.stream().filter(x -> x != null).toList();
        if (def.imageSlots() > 0) {
            if (ids.size() > def.imageSlots()) {
                errors.put("images", "ส่วนนี้ใส่รูปได้ไม่เกิน " + def.imageSlots() + " รูป");
            } else if (media.findActiveByIds(ids).size() != ids.stream().distinct().count()) {
                errors.put("images", "ไม่พบรูปที่เลือกในคลังรูป");
            }
        }
        if (def.editsMapUrl() && !MapEmbed.isAcceptable(mapUrl)) {
            errors.put("mapUrl", "ใส่ลิงก์ Google Maps ที่ขึ้นต้นด้วย https:// หรือโค้ดฝังแผนที่จาก Google Maps");
        }
        if (!errors.isEmpty()) {
            return errors;
        }
        s.setContent(content);
        if (def.imageSlots() > 0) {
            s.setMediaIdList(ids);
        }
        if (def.editsMapUrl()) {
            settings.saveMapUrl(mapUrl);
        }
        audit.log("section.update", "page_section", s.getId(), slug + "/" + key);
        return errors;
    }

    @Transactional
    public void setVisible(String slug, String key, boolean visible) {
        PageSection s = sections.findByPageSlugAndKey(slug, key).orElseThrow(NotFoundException::new);
        SectionDef def = SectionDefs.section(slug, key).orElseThrow(NotFoundException::new);
        if (def.fixed()) {
            return;
        }
        s.setVisible(visible);
        audit.log(visible ? "section.show" : "section.hide", "page_section", s.getId(), slug + "/" + key);
    }

    /** Moves a section up (-1) or down (+1) among the movable sections of the page. */
    @Transactional
    public void move(String slug, String key, int direction) {
        List<PageSection> movable = sections.findByPageSlug(slug).stream()
                .filter(s -> !SectionDefs.section(slug, s.getKey()).map(SectionDef::fixed).orElse(false))
                .toList();
        int i = -1;
        for (int j = 0; j < movable.size(); j++) {
            if (movable.get(j).getKey().equals(key)) {
                i = j;
            }
        }
        int target = i + Integer.signum(direction);
        if (i < 0 || target < 0 || target >= movable.size()) {
            return;
        }
        PageSection a = movable.get(i);
        PageSection b = movable.get(target);
        int tmp = a.getSortOrder();
        a.setSortOrder(b.getSortOrder());
        b.setSortOrder(tmp);
        audit.log("section.move", "page_section", a.getId(), slug + "/" + key + (direction < 0 ? " ขึ้น" : " ลง"));
    }

    @Transactional
    public void setPublished(String slug, boolean published) {
        Page p = pages.findBySlug(slug).orElseThrow(NotFoundException::new);
        if ("home".equals(slug) && !published) {
            return; // the home page cannot be taken offline
        }
        p.setPublished(published);
        audit.log(published ? "page.publish" : "page.unpublish", "page", p.getId(), slug);
    }

    @Transactional(readOnly = true)
    public List<Page> allPages() {
        return SectionDefs.PAGES.stream().map(d -> pages.findBySlug(d.slug()).orElse(null)).filter(p -> p != null).toList();
    }
}
