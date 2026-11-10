package com.bakban9.web.admin;

import com.bakban9.domain.Message;
import com.bakban9.service.MailService;
import com.bakban9.service.MessageService;
import com.bakban9.util.Texts;
import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AdminMessagesController {

    private final MessageService messages;
    private final MailService mail;

    public AdminMessagesController(MessageService messages, MailService mail) {
        this.messages = messages;
        this.mail = mail;
    }

    @GetMapping("/admin/messages")
    public String list(@RequestParam(defaultValue = "all") String filter, @RequestParam(required = false) Long id,
                       Model model) {
        List<Message> items = messages.list(filter);
        Message selected = id != null ? messages.get(id) : (items.isEmpty() ? null : items.get(0));
        model.addAttribute("nav", "messages");
        model.addAttribute("filter", filter);
        model.addAttribute("items", items);
        model.addAttribute("sel", selected);
        model.addAttribute("mailConfigured", mail.isConfigured());
        return "admin/messages";
    }

    @PostMapping("/admin/messages/{id}/reply")
    public String reply(@PathVariable Long id, @RequestParam(defaultValue = "") String body,
                        @RequestParam(defaultValue = "all") String filter, RedirectAttributes ra) {
        if (Texts.nz(body).isEmpty()) {
            Flash.error(ra, "พิมพ์คำตอบก่อนส่ง");
        } else if (body.length() > 10000) {
            Flash.error(ra, "คำตอบยาวเกิน 10,000 ตัวอักษร");
        } else {
            try {
                messages.reply(id, body);
                Flash.ok(ra, "ส่งคำตอบทางอีเมลแล้ว และทำเครื่องหมายว่าตอบแล้ว");
            } catch (MailService.MailException e) {
                ra.addFlashAttribute("replyDraft", body);
                Flash.error(ra, "ส่งอีเมลไม่สำเร็จ ลองใหม่อีกครั้ง หรือตรวจการตั้งค่า SMTP");
            }
        }
        return "redirect:/admin/messages?filter=" + filter + "&id=" + id;
    }

    @PostMapping("/admin/messages/{id}/status")
    public String status(@PathVariable Long id, @RequestParam(defaultValue = "true") boolean replied,
                         @RequestParam(defaultValue = "all") String filter, RedirectAttributes ra) {
        messages.setReplied(id, replied);
        Flash.ok(ra, replied ? "ทำเครื่องหมายว่าตอบแล้ว" : "เปลี่ยนเป็นยังไม่ตอบ");
        return "redirect:/admin/messages?filter=" + filter + "&id=" + id;
    }
}
