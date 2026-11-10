package com.bakban9.web.admin;

import com.bakban9.domain.Media;
import com.bakban9.domain.MediaRepository;
import com.bakban9.service.ContentService;
import com.bakban9.service.NotFoundException;
import com.bakban9.service.SectionDefs;
import com.bakban9.service.SettingsService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AdminPagesController {

    private final ContentService content;
    private final MediaRepository media;
    private final SettingsService settings;

    public AdminPagesController(ContentService content, MediaRepository media, SettingsService settings) {
        this.content = content;
        this.media = media;
        this.settings = settings;
    }

    @GetMapping("/admin/pages")
    public String index(@RequestParam(name = "page", defaultValue = "home") String slug,
                        @RequestParam(name = "section", required = false) String sectionKey, Model model) {
        SectionDefs.PageDef pageDef = SectionDefs.page(slug).orElseThrow(NotFoundException::new);
        ContentService.PageView view = content.load(slug);
        String key = sectionKey != null && SectionDefs.section(slug, sectionKey).isPresent()
                ? sectionKey : view.sections().get(0).key();
        SectionDefs.SectionDef def = SectionDefs.section(slug, key).orElseThrow();
        ContentService.SectionView current = view.get(key);

        if (!model.containsAttribute("values")) {
            model.addAttribute("values", current.content());
        }
        List<Long> selected = new ArrayList<>(current.media().stream().map(Media::getId).toList());
        while (selected.size() < def.imageSlots()) {
            selected.add(null);
        }
        model.addAttribute("nav", "pages");
        model.addAttribute("pageDefs", SectionDefs.PAGES);
        model.addAttribute("pageDef", pageDef);
        model.addAttribute("view", view);
        model.addAttribute("def", def);
        model.addAttribute("current", current);
        model.addAttribute("selectedMedia", selected);
        model.addAttribute("library", def.imageSlots() > 0 ? media.findActive() : List.of());
        model.addAttribute("mapUrl", settings.get().getMapUrl());
        return "admin/pages";
    }

    @PostMapping("/admin/pages/{slug}/sections/{key}")
    public String save(@PathVariable String slug, @PathVariable String key,
                       @RequestParam MultiValueMap<String, String> form, RedirectAttributes ra) {
        Map<String, String> values = new HashMap<>();
        form.forEach((k, v) -> {
            if (k.startsWith("f_") && !v.isEmpty()) {
                values.put(k.substring(2), v.get(v.size() - 1));
            }
        });
        List<Long> mediaIds = new ArrayList<>();
        for (String v : form.getOrDefault("mediaIds", List.of())) {
            if (v != null && v.matches("\\d+")) {
                mediaIds.add(Long.parseLong(v));
            }
        }
        Map<String, String> errors = content.saveSection(slug, key, values, mediaIds, form.getFirst("mapUrl"));
        if (errors.isEmpty()) {
            Flash.ok(ra, "บันทึกและเผยแพร่แล้ว หน้าเว็บเปลี่ยนตามทันที");
        } else {
            ra.addFlashAttribute("values", values);
            Flash.errors(ra, errors);
        }
        return "redirect:/admin/pages?page=" + slug + "&section=" + key;
    }

    @PostMapping("/admin/pages/{slug}/sections/{key}/move")
    public String move(@PathVariable String slug, @PathVariable String key, @RequestParam String dir, RedirectAttributes ra) {
        content.move(slug, key, "up".equals(dir) ? -1 : 1);
        Flash.ok(ra, "เลื่อนลำดับแล้ว");
        return "redirect:/admin/pages?page=" + slug + "&section=" + key;
    }

    @PostMapping("/admin/pages/{slug}/sections/{key}/visibility")
    public String visibility(@PathVariable String slug, @PathVariable String key,
                             @RequestParam(defaultValue = "false") boolean visible, RedirectAttributes ra) {
        content.setVisible(slug, key, visible);
        Flash.ok(ra, visible ? "เปิดแสดงส่วนนี้แล้ว" : "ซ่อนส่วนนี้แล้ว");
        return "redirect:/admin/pages?page=" + slug + "&section=" + key;
    }

    @PostMapping("/admin/pages/{slug}/publish")
    public String publish(@PathVariable String slug, @RequestParam(defaultValue = "false") boolean published,
                          RedirectAttributes ra) {
        if ("home".equals(slug) && !published) {
            Flash.error(ra, "ปิดหน้าแรกไม่ได้");
        } else {
            content.setPublished(slug, published);
            Flash.ok(ra, published ? "เผยแพร่หน้านี้แล้ว" : "ปิดหน้านี้แล้ว ผู้เข้าชมจะเห็นหน้าไม่พบ");
        }
        return "redirect:/admin/pages?page=" + slug;
    }
}
