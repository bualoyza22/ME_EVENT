package com.bakban9.web.admin;

import com.bakban9.domain.Media;
import com.bakban9.domain.MediaRepository;
import com.bakban9.security.AdminPrincipal;
import com.bakban9.service.AdminAccountService;
import com.bakban9.service.AuditService;
import com.bakban9.service.SettingsService;
import com.bakban9.util.Texts;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AdminSettingsController {

    /** Read-only swatches; the values live in tokens.css (Design System BAKBAN). */
    public record Swatch(String token, String hex) {}

    static final List<Swatch> SWATCHES = List.of(
            new Swatch("bg-primary", "#F4EFE6"), new Swatch("bg-secondary", "#E7DDD0"),
            new Swatch("color-primary", "#84563C"), new Swatch("color-sub-accent", "#936B4F"),
            new Swatch("text-primary", "#2C2623"), new Swatch("text-secondary", "#5E524D"),
            new Swatch("text-light", "#FFFFFF"), new Swatch("border-dark", "#3D272A"));

    private final SettingsService settings;
    private final MediaRepository media;
    private final AdminAccountService accounts;
    private final AuditService audit;

    public AdminSettingsController(SettingsService settings, MediaRepository media, AdminAccountService accounts,
                                   AuditService audit) {
        this.settings = settings;
        this.media = media;
        this.accounts = accounts;
        this.audit = audit;
    }

    @GetMapping("/admin/settings")
    public String index(@AuthenticationPrincipal AdminPrincipal me, Model model) {
        model.addAttribute("nav", "settings");
        model.addAttribute("settings", settings.get());
        model.addAttribute("logos", media.findActiveByKind(Media.KIND_LOGO));
        model.addAttribute("swatches", SWATCHES);
        if (me.isOwner()) {
            model.addAttribute("admins", accounts.all());
            model.addAttribute("invites", accounts.pendingInvites());
            model.addAttribute("auditLog", audit.latest(30));
        }
        return "admin/settings";
    }

    @PostMapping("/admin/settings")
    public String save(@RequestParam(defaultValue = "") String siteName, @RequestParam(required = false) Long logoId,
                       RedirectAttributes ra) {
        if (Texts.nz(siteName).isEmpty() || siteName.length() > 200) {
            Flash.error(ra, "ชื่อเว็บไซต์ต้องมี 1–200 ตัวอักษร");
        } else {
            settings.saveSite(siteName, logoId);
            Flash.ok(ra, "บันทึกการตั้งค่าแล้ว");
        }
        return "redirect:/admin/settings";
    }

    @PostMapping("/admin/settings/password")
    public String password(@AuthenticationPrincipal AdminPrincipal me, @RequestParam(defaultValue = "") String current,
                           @RequestParam(defaultValue = "") String next, @RequestParam(defaultValue = "") String confirm,
                           RedirectAttributes ra) {
        if (!next.equals(confirm)) {
            Flash.error(ra, "รหัสผ่านใหม่ทั้งสองช่องไม่ตรงกัน");
            return "redirect:/admin/settings";
        }
        try {
            accounts.changePassword(me.getId(), current, next);
            Flash.ok(ra, "เปลี่ยนรหัสผ่านแล้ว");
        } catch (AdminAccountService.AccountException e) {
            Flash.error(ra, e.getMessage());
        }
        return "redirect:/admin/settings";
    }

    @PostMapping("/admin/settings/admins/invite")
    public String invite(@AuthenticationPrincipal AdminPrincipal me, @RequestParam(defaultValue = "") String email,
                         RedirectAttributes ra) {
        try {
            accounts.invite(email, me.getId());
            Flash.ok(ra, "ส่งคำเชิญไปที่ " + email.trim() + " แล้ว ลิงก์ใช้ได้ 3 วัน");
        } catch (AdminAccountService.AccountException e) {
            Flash.error(ra, e.getMessage());
        }
        return "redirect:/admin/settings";
    }

    @PostMapping("/admin/settings/admins/{id}/active")
    public String active(@AuthenticationPrincipal AdminPrincipal me, @PathVariable Long id,
                         @RequestParam(defaultValue = "false") boolean active, RedirectAttributes ra) {
        try {
            accounts.setActive(id, active, me.getId());
            Flash.ok(ra, active ? "เปิดบัญชีผู้ดูแลแล้ว" : "ปิดบัญชีผู้ดูแลแล้ว");
        } catch (AdminAccountService.AccountException e) {
            Flash.error(ra, e.getMessage());
        }
        return "redirect:/admin/settings";
    }

    @PostMapping("/admin/settings/admins/{id}/unlock")
    public String unlock(@PathVariable Long id, RedirectAttributes ra) {
        accounts.unlock(id);
        Flash.ok(ra, "ปลดล็อกบัญชีแล้ว");
        return "redirect:/admin/settings";
    }
}
