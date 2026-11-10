package com.bakban9.web.pub;

import com.bakban9.config.AppProperties;
import com.bakban9.service.ContentService;
import com.bakban9.service.MemberService;
import com.bakban9.service.NotFoundException;
import com.bakban9.service.RateLimiter;
import com.bakban9.util.Texts;
import com.bakban9.util.UrlRules;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class SignupController {

    private final ContentService content;
    private final MemberService members;
    private final RateLimiter limiter;
    private final AppProperties props;

    public SignupController(ContentService content, MemberService members, RateLimiter limiter, AppProperties props) {
        this.content = content;
        this.members = members;
        this.limiter = limiter;
        this.props = props;
    }

    public static class SignupForm {
        private String name = "";
        private String email = "";
        private String phone = "";
        private boolean consent;
        /** honeypot: hidden from people, bots fill it in */
        private String company = "";
        private String source = "signup";

        public String getName() { return name; }
        public void setName(String v) { name = v; }
        public String getEmail() { return email; }
        public void setEmail(String v) { email = v; }
        public String getPhone() { return phone; }
        public void setPhone(String v) { phone = v; }
        public boolean isConsent() { return consent; }
        public void setConsent(boolean v) { consent = v; }
        public String getCompany() { return company; }
        public void setCompany(String v) { company = v; }
        public String getSource() { return source; }
        public void setSource(String v) { source = v; }
    }

    @GetMapping("/signup")
    public String page(Model model) {
        ContentService.PageView view = content.load("signup");
        if (!view.page().isPublished()) {
            throw new NotFoundException();
        }
        model.addAttribute("view", view);
        model.addAttribute("activeNav", "signup");
        if (!model.containsAttribute("signupForm")) {
            model.addAttribute("signupForm", new SignupForm());
        }
        return "public/signup";
    }

    @PostMapping("/signup")
    public String submit(@ModelAttribute SignupForm form, HttpServletRequest req, RedirectAttributes ra) {
        boolean fromHome = "home".equals(form.getSource());
        String back = fromHome ? "redirect:/#signup" : "redirect:/signup";
        boolean showPhone = !fromHome && content.load("signup").get("form").flag("showPhone");

        if (!Texts.nz(form.getCompany()).isEmpty()) {
            ra.addFlashAttribute("signupResult", "created"); // pretend success to bots, store nothing
            return back;
        }
        Map<String, String> errors = validate(form, showPhone);
        if (!errors.isEmpty()) {
            ra.addFlashAttribute("signupForm", form);
            ra.addFlashAttribute("signupErrors", errors);
            ra.addFlashAttribute("signupError", "ขอชื่อกับอีเมลที่ถูกต้องก่อนน้า");
            return back;
        }
        // only accepted submissions count, so fixing a typo never locks a visitor out
        AppProperties.RateLimit rl = props.rateLimit();
        if (!limiter.tryAcquire("signup:" + ClientIp.of(req), rl.formMax(), Duration.ofMinutes(rl.formWindowMinutes()))) {
            ra.addFlashAttribute("signupForm", form);
            ra.addFlashAttribute("signupError", "ส่งบ่อยเกินไป ลองใหม่อีกครั้งในอีก " + rl.formWindowMinutes() + " นาทีนะ");
            return back;
        }
        MemberService.SignupResult result = members.signup(form.getName(), form.getEmail(),
                showPhone ? form.getPhone() : "", !fromHome && form.isConsent());
        ra.addFlashAttribute("signupResult", result == MemberService.SignupResult.CREATED ? "created" : "duplicate");
        return back;
    }

    static Map<String, String> validate(SignupForm f, boolean showPhone) {
        Map<String, String> errors = new LinkedHashMap<>();
        String name = Texts.nz(f.getName());
        if (name.isEmpty()) {
            errors.put("name", "กรุณาใส่ชื่อ");
        } else if (name.length() > 120) {
            errors.put("name", "ชื่อยาวเกิน 120 ตัวอักษร");
        }
        if (!UrlRules.isValidEmail(f.getEmail())) {
            errors.put("email", "รูปแบบอีเมลไม่ถูกต้อง");
        }
        if (showPhone && !UrlRules.isValidPhone(f.getPhone())) {
            errors.put("phone", "เบอร์โทรใส่ได้เฉพาะตัวเลข");
        }
        return errors;
    }
}
