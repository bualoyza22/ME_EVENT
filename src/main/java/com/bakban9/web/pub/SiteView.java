package com.bakban9.web.pub;

import com.bakban9.domain.Media;
import com.bakban9.domain.SiteSettings;
import com.bakban9.util.MapEmbed;
import com.bakban9.util.UrlRules;
import java.util.ArrayList;
import java.util.List;

/** Shop info prepared for templates: hrefs built, empty values left out. */
public record SiteView(String siteName, Media logo, String tagline, String website, String websiteHref,
                       String address, String hours, String tel, String telHref, String email,
                       List<Social> socials, String lineUrl, MapEmbed.Result map) {

    public record Social(String label, String url) {}

    public static SiteView of(SiteSettings s) {
        List<Social> socials = new ArrayList<>();
        add(socials, "Instagram", s.getInstagramUrl());
        add(socials, "LINE", s.getLineUrl());
        add(socials, "Facebook", s.getFacebookUrl());
        add(socials, "TikTok", s.getTiktokUrl());
        return new SiteView(s.getSiteName(), s.getLogo(), s.getTagline(), s.getWebsite(), UrlRules.websiteHref(s.getWebsite()),
                s.getAddress(), s.getHours(), s.getTel(), UrlRules.telHref(s.getTel()), s.getEmail(), socials,
                UrlRules.isHttpsUrl(s.getLineUrl()) ? s.getLineUrl() : "", MapEmbed.resolve(s.getMapUrl()));
    }

    public boolean hasContact() {
        return !hours.isBlank() || !tel.isBlank() || !email.isBlank();
    }

    private static void add(List<Social> list, String label, String url) {
        if (UrlRules.isHttpsUrl(url)) {
            list.add(new Social(label, url));
        }
    }
}
