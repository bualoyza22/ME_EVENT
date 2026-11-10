package com.bakban9.web.admin;

import com.bakban9.service.MemberService;
import java.time.LocalDate;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AdminMembersController {

    private final MemberService members;

    public AdminMembersController(MemberService members) {
        this.members = members;
    }

    @GetMapping("/admin/members")
    public String list(@RequestParam(defaultValue = "") String q, @RequestParam(defaultValue = "all") String consent,
                       Model model) {
        model.addAttribute("nav", "members");
        model.addAttribute("rows", members.search(q, consent));
        model.addAttribute("q", q);
        model.addAttribute("consent", consent);
        return "admin/members";
    }

    /** Owner only (SecurityConfig). UTF-8 with BOM so Excel shows Thai correctly. */
    @GetMapping("/admin/members/export")
    public ResponseEntity<byte[]> export(@RequestParam(defaultValue = "") String q,
                                         @RequestParam(defaultValue = "all") String consent) {
        byte[] csv = members.exportCsv(q, consent);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename("bakban9-members-" + LocalDate.now(MemberService.BANGKOK) + ".csv").build().toString())
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .contentType(new MediaType("text", "csv", java.nio.charset.StandardCharsets.UTF_8))
                .body(csv);
    }

    @PostMapping("/admin/members/{id}/code")
    public String code(@PathVariable Long id, @RequestParam(defaultValue = "false") boolean sent, RedirectAttributes ra) {
        members.setCodeSent(id, sent);
        Flash.ok(ra, sent ? "บันทึกว่าส่งโค้ดแล้ว" : "เปลี่ยนเป็นยังไม่ส่งโค้ด");
        return "redirect:/admin/members";
    }

    @PostMapping("/admin/members/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        members.softDelete(id);
        Flash.ok(ra, "ลบข้อมูลสมาชิกแล้ว ระบบจะลบถาวรภายใน 30 วัน");
        return "redirect:/admin/members";
    }
}
