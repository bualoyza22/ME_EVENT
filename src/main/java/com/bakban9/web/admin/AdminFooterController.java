package com.bakban9.web.admin;

import com.bakban9.domain.SiteSettings;
import com.bakban9.service.SettingsService;
import com.bakban9.service.SettingsService.FooterForm;
import java.util.Map;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AdminFooterController {

    private final SettingsService settings;

    public AdminFooterController(SettingsService settings) {
        this.settings = settings;
    }

    @GetMapping("/admin/footer")
    public String form(Model model) {
        if (!model.containsAttribute("form")) {
            SiteSettings s = settings.get();
            model.addAttribute("form", new FooterForm(s.getTagline(), s.getWebsite(), s.getAddress(), s.getHours(),
                    s.getTel(), s.getEmail(), s.getInstagramUrl(), s.getLineUrl(), s.getFacebookUrl(), s.getTiktokUrl()));
        }
        model.addAttribute("nav", "footer");
        return "admin/footer";
    }

    @PostMapping("/admin/footer")
    public String save(@ModelAttribute FooterForm form, RedirectAttributes ra) {
        Map<String, String> errors = settings.validateFooter(form);
        if (!errors.isEmpty()) {
            ra.addFlashAttribute("form", form);
            Flash.errors(ra, errors);
        } else {
            settings.saveFooter(form);
            Flash.ok(ra, "บันทึก Footer แล้ว ทุกหน้าและหน้าติดต่อเราเปลี่ยนตามทันที");
        }
        return "redirect:/admin/footer";
    }
}
