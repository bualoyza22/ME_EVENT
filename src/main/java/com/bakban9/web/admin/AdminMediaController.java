package com.bakban9.web.admin;

import com.bakban9.domain.Media;
import com.bakban9.domain.MediaRepository;
import com.bakban9.service.MediaService;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AdminMediaController {

    private final MediaRepository repo;
    private final MediaService media;

    public AdminMediaController(MediaRepository repo, MediaService media) {
        this.repo = repo;
        this.media = media;
    }

    public record Item(Media media, List<String> usage) {}

    @GetMapping("/admin/media")
    public String list(@RequestParam(defaultValue = "all") String filter, Model model) {
        List<Media> all = switch (filter) {
            case "logo" -> repo.findActiveByKind(Media.KIND_LOGO);
            case "product" -> repo.findActiveByKind(Media.KIND_PRODUCT);
            case "other" -> repo.findActiveByKind(Media.KIND_OTHER);
            default -> repo.findActive();
        };
        Map<String, String> filters = new LinkedHashMap<>();
        filters.put("all", "ทั้งหมด");
        filters.put("logo", "โลโก้");
        filters.put("product", "สินค้า");
        filters.put("other", "อัปโหลดใหม่");
        model.addAttribute("nav", "media");
        model.addAttribute("filter", filter);
        model.addAttribute("filters", filters);
        model.addAttribute("items", all.stream().map(m -> new Item(m, media.usage(m))).toList());
        return "admin/media";
    }

    @PostMapping("/admin/media")
    public String upload(@RequestParam(name = "files", required = false) List<MultipartFile> files,
                         @RequestParam(defaultValue = "other") String kind, RedirectAttributes ra) {
        int ok = 0;
        List<String> failed = new ArrayList<>();
        for (MultipartFile f : files == null ? List.<MultipartFile>of() : files) {
            if (f.isEmpty()) {
                continue;
            }
            try {
                media.upload(f.getOriginalFilename(), f.getBytes(), kind, "");
                ok++;
            } catch (MediaService.UploadException e) {
                failed.add(e.getMessage());
            } catch (IOException e) {
                failed.add(f.getOriginalFilename() + ": อ่านไฟล์ไม่สำเร็จ");
            }
        }
        if (ok == 0 && failed.isEmpty()) {
            Flash.error(ra, "เลือกไฟล์ก่อนอัปโหลด");
        } else if (failed.isEmpty()) {
            Flash.ok(ra, "อัปโหลดแล้ว " + ok + " รูป");
        } else {
            Flash.error(ra, (ok > 0 ? "อัปโหลดแล้ว " + ok + " รูป · " : "") + "ไม่สำเร็จ: " + String.join(" · ", failed));
        }
        return "redirect:/admin/media?filter=" + ("logo".equals(kind) || "product".equals(kind) ? kind : "other");
    }

    @PostMapping("/admin/media/{id}/alt")
    public String alt(@PathVariable Long id, @RequestParam(defaultValue = "") String alt,
                      @RequestParam(defaultValue = "all") String filter, RedirectAttributes ra) {
        if (alt.length() > 255) {
            Flash.error(ra, "คำอธิบายรูปยาวเกิน 255 ตัวอักษร");
        } else {
            media.updateAlt(id, alt);
            Flash.ok(ra, "บันทึกคำอธิบายรูปแล้ว");
        }
        return "redirect:/admin/media?filter=" + filter;
    }

    @PostMapping("/admin/media/{id}/delete")
    public String delete(@PathVariable Long id, @RequestParam(defaultValue = "all") String filter, RedirectAttributes ra) {
        try {
            media.softDelete(id);
            Flash.ok(ra, "ลบรูปแล้ว");
        } catch (MediaService.UploadException e) {
            Flash.error(ra, e.getMessage());
        }
        return "redirect:/admin/media?filter=" + filter;
    }
}
