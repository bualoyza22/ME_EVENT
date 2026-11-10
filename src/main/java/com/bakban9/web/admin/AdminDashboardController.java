package com.bakban9.web.admin;

import com.bakban9.service.ContentService;
import com.bakban9.service.DashboardService;
import com.bakban9.service.MessageService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AdminDashboardController {

    private final DashboardService dashboard;
    private final MessageService messages;
    private final ContentService content;

    public AdminDashboardController(DashboardService dashboard, MessageService messages, ContentService content) {
        this.dashboard = dashboard;
        this.messages = messages;
        this.content = content;
    }

    @GetMapping("/admin")
    public String index(Model model) {
        model.addAttribute("nav", "dashboard");
        model.addAttribute("stats", dashboard.stats());
        model.addAttribute("todos", dashboard.todos());
        model.addAttribute("pages", content.allPages());
        model.addAttribute("latest", messages.latest(5));
        return "admin/dashboard";
    }
}
