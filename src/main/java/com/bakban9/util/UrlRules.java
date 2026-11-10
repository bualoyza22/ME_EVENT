package com.bakban9.util;

import java.net.URI;
import java.util.Locale;

public final class UrlRules {

    private UrlRules() {}

    /** true when the value is an absolute https URL with a host. */
    public static boolean isHttpsUrl(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        try {
            URI uri = new URI(value.trim());
            return "https".equalsIgnoreCase(uri.getScheme()) && uri.getHost() != null && !uri.getHost().isBlank();
        } catch (Exception e) {
            return false;
        }
    }

    /** Link target for the "website" field, which may be typed as "www.example.com". */
    public static String websiteHref(String website) {
        String w = Texts.nz(website);
        if (w.isEmpty()) {
            return "";
        }
        String lower = w.toLowerCase(Locale.ROOT);
        if (lower.startsWith("https://") || lower.startsWith("http://")) {
            return w;
        }
        return "https://" + w;
    }

    /** "+66 81-234-5678" → "+66812345678" for tel: links. */
    public static String telHref(String tel) {
        String t = Texts.nz(tel);
        if (t.isEmpty()) {
            return "";
        }
        return "tel:" + t.replaceAll("[^0-9+]", "");
    }

    public static boolean isValidPhone(String tel) {
        String t = Texts.nz(tel);
        return t.isEmpty() || (t.matches("[0-9+\\-\\s().]{6,30}") && t.replaceAll("\\D", "").length() >= 6);
    }

    public static boolean isValidEmail(String email) {
        String e = Texts.nz(email);
        return e.length() <= 254 && e.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+");
    }
}
