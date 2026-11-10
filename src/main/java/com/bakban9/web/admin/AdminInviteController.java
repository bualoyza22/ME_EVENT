package com.bakban9.web.admin;

import com.bakban9.domain.AdminInvite;
import com.bakban9.service.AdminAccountService;
import java.util.Optional;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AdminInviteController {

    private final AdminAccountService accounts;

    public AdminInviteController(AdminAccountService accounts) {
        this.accounts = accounts;
    }

    @GetMapping("/admin/invite/{token}")
    public String form(@PathVariable String token, Model model) {
        Optional<AdminInvite> inv = accounts.findUsableInvite(token);
        model.addAttribute("invite", inv.orElse(null));
        model.addAttribute("token", token);
        return "admin/invite";
    }

    @PostMapping("/admin/invite/{token}")
    public String accept(@PathVariable String token, @RequestParam(defaultValue = "") String name,
                         @RequestParam(defaultValue = "") String password, @RequestParam(defaultValue = "") String confirm,
                         RedirectAttributes ra) {
        if (!password.equals(confirm)) {
            ra.addFlashAttribute("inviteError", "รหัสผ่านทั้งสองช่องไม่ตรงกัน");
            return "redirect:/admin/invite/" + token;
        }
        try {
            accounts.acceptInvite(token, name, password);
        } catch (AdminAccountService.AccountException e) {
            ra.addFlashAttribute("inviteError", e.getMessage());
            return "redirect:/admin/invite/" + token;
        }
        return "redirect:/admin/login?invited";
    }
}
