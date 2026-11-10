package com.bakban9.web.admin;

import com.bakban9.security.AdminPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AdminLoginController {

    @GetMapping("/admin/login")
    public String login(@AuthenticationPrincipal AdminPrincipal me,
                        @RequestParam(required = false) String error,
                        @RequestParam(required = false) String locked,
                        @RequestParam(required = false) String throttled,
                        @RequestParam(required = false) String logout,
                        @RequestParam(required = false) String invited,
                        Model model) {
        if (me != null) {
            return "redirect:/admin";
        }
        String message = null;
        if (error != null) {
            message = "อีเมลหรือรหัสผ่านไม่ถูกต้อง";
        } else if (locked != null) {
            message = "ใส่รหัสผ่านผิดหลายครั้ง บัญชีถูกล็อกชั่วคราว 15 นาที";
        } else if (throttled != null) {
            message = "พยายามเข้าสู่ระบบบ่อยเกินไป ลองใหม่อีกครั้งในอีกสักครู่";
        } else if (logout != null) {
            model.addAttribute("info", "ออกจากระบบแล้ว");
        } else if (invited != null) {
            model.addAttribute("info", "สร้างบัญชีแล้ว เข้าสู่ระบบด้วยอีเมลและรหัสผ่านใหม่ได้เลย");
        }
        model.addAttribute("loginError", message);
        return "admin/login";
    }

    @RequestMapping("/admin/forbidden")
    public String forbidden(Model model) {
        model.addAttribute("nav", "");
        return "admin/forbidden";
    }
}
