package com.bakban9.web.admin;

import java.util.Map;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Result line shown in the role="status" area after every save. */
final class Flash {

    private Flash() {}

    static void ok(RedirectAttributes ra, String text) {
        ra.addFlashAttribute("status", text);
        ra.addFlashAttribute("statusKind", "ok");
    }

    static void error(RedirectAttributes ra, String text) {
        ra.addFlashAttribute("status", text);
        ra.addFlashAttribute("statusKind", "error");
    }

    static void errors(RedirectAttributes ra, Map<String, String> errors) {
        ra.addFlashAttribute("errors", errors);
        error(ra, "บันทึกไม่สำเร็จ: " + String.join(" · ", errors.values()));
    }
}
