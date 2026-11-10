package com.bakban9.web.pub;

import com.bakban9.config.AppProperties;
import com.bakban9.domain.ProductRepository;
import com.bakban9.service.ContentService;
import com.bakban9.service.MessageService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ContactController {

    private final ContentService content;
    private final MessageService messages;
    private final ProductRepository products;
    private final RateLimiter limiter;
    private final AppProperties props;

    public ContactController(ContentService content, MessageService messages, ProductRepository products,
                             RateLimiter limiter, AppProperties props) {
        this.content = content;
        this.messages = messages;
        this.products = products;
        this.limiter = limiter;
        this.props = props;
    }

    public static class ContactForm {
        private String name = "";
        private String email = "";
        private String subject = "";
        private String message = "";
        private String company = "";

        public String getName() { return name; }
        public void setName(String v) { name = v; }
        public String getEmail() { return email; }
        public void setEmail(String v) { email = v; }
        public String getSubject() { return subject; }
        public void setSubject(String v) { subject = v; }
        public String getMessage() { return message; }
        public void setMessage(String v) { message = v; }
        public String getCompany() { return company; }
        public void setCompany(String v) { company = v; }
    }

    @GetMapping("/contact")
    public String page(@RequestParam(name = "product", required = false) Long productId, Model model) {
        ContentService.PageView view = content.load("contact");
        if (!view.page().isPublished()) {
            throw new NotFoundException();
        }
        model.addAttribute("view", view);
        model.addAttribute("activeNav", "contact");
        if (!model.containsAttribute("contactForm")) {
            ContactForm f = new ContactForm();
            if (productId != null) {
                products.findActiveById(productId).filter(p -> p.isVisible())
                        .ifPresent(p -> f.setSubject("สนใจ: " + p.getName()));
            }
            model.addAttribute("contactForm", f);
        }
        return "public/contact";
    }

    @PostMapping("/contact")
    public String submit(@ModelAttribute ContactForm form, HttpServletRequest req, RedirectAttributes ra) {
        ContentService.PageView view = content.load("contact");
        if (!view.page().isPublished() || !view.isVisible("form")) {
            ra.addFlashAttribute("contactError", "ตอนนี้ปิดรับข้อความทางหน้าเว็บชั่วคราว ติดต่อเราทางช่องทางอื่นได้เลย");
            return "redirect:/contact";
        }
        if (!Texts.nz(form.getCompany()).isEmpty()) {
            ra.addFlashAttribute("contactSent", true);
            return "redirect:/contact";
        }
        Map<String, String> errors = validate(form);
        if (!errors.isEmpty()) {
            ra.addFlashAttribute("contactForm", form);
            ra.addFlashAttribute("contactErrors", errors);
            ra.addFlashAttribute("contactError", "กรอกชื่อ อีเมล และข้อความให้ครบก่อนน้า");
            return "redirect:/contact";
        }
        // only accepted submissions count, so fixing a typo never locks a visitor out
        AppProperties.RateLimit rl = props.rateLimit();
        if (!limiter.tryAcquire("contact:" + ClientIp.of(req), rl.formMax(), Duration.ofMinutes(rl.formWindowMinutes()))) {
            ra.addFlashAttribute("contactForm", form);
            ra.addFlashAttribute("contactError", "ส่งบ่อยเกินไป ลองใหม่อีกครั้งในอีก " + rl.formWindowMinutes() + " นาทีนะ");
            return "redirect:/contact";
        }
        messages.create(form.getName(), form.getEmail(), form.getSubject(), form.getMessage());
        ra.addFlashAttribute("contactSent", true);
        return "redirect:/contact";
    }

    static Map<String, String> validate(ContactForm f) {
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
        if (Texts.nz(f.getSubject()).length() > 200) {
            errors.put("subject", "หัวข้อยาวเกิน 200 ตัวอักษร");
        }
        String msg = Texts.nz(f.getMessage());
        if (msg.isEmpty()) {
            errors.put("message", "กรุณาใส่ข้อความ");
        } else if (msg.length() > 5000) {
            errors.put("message", "ข้อความยาวเกิน 5,000 ตัวอักษร");
        }
        return errors;
    }
}
