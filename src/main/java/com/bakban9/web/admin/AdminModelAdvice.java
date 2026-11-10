package com.bakban9.web.admin;

import com.bakban9.security.AdminPrincipal;
import com.bakban9.service.SettingsService;
import com.bakban9.web.pub.SiteView;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice(basePackageClasses = AdminModelAdvice.class)
public class AdminModelAdvice {

    private final SettingsService settings;

    public AdminModelAdvice(SettingsService settings) {
        this.settings = settings;
    }

    @ModelAttribute("me")
    public AdminPrincipal me(@AuthenticationPrincipal AdminPrincipal me) {
        return me;
    }

    @ModelAttribute("site")
    public SiteView site() {
        return SiteView.of(settings.get());
    }
}
