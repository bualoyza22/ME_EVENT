package com.bakban9.util;

import java.net.URI;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Turns what the admin pastes into the map field (an embed URL, the whole embed iframe code,
 * or a normal Google Maps link) into something the contact page can show.
 */
public final class MapEmbed {

    private static final Pattern IFRAME_SRC = Pattern.compile("src\\s*=\\s*[\"']([^\"']+)[\"']", Pattern.CASE_INSENSITIVE);

    /** embedUrl: safe to put in an iframe (may be null); linkUrl: open in Google Maps (may be null). */
    public record Result(String embedUrl, String linkUrl) {
        public boolean isEmpty() {
            return embedUrl == null && linkUrl == null;
        }
    }

    private MapEmbed() {}

    /** Accepts an iframe snippet and returns just its src, otherwise the trimmed input. */
    public static String normalizeInput(String raw) {
        String s = Texts.nz(raw);
        if (s.toLowerCase(Locale.ROOT).contains("<iframe")) {
            Matcher m = IFRAME_SRC.matcher(s);
            return m.find() ? m.group(1).replace("&amp;", "&").trim() : "";
        }
        return s;
    }

    public static boolean isAcceptable(String raw) {
        String s = normalizeInput(raw);
        return s.isEmpty() || (UrlRules.isHttpsUrl(s) && isGoogleMapsHost(host(s)));
    }

    public static Result resolve(String raw) {
        String s = normalizeInput(raw);
        if (s.isEmpty() || !UrlRules.isHttpsUrl(s)) {
            return new Result(null, null);
        }
        String host = host(s);
        if (!isGoogleMapsHost(host)) {
            return new Result(null, null);
        }
        URI uri = URI.create(s);
        String path = uri.getPath() == null ? "" : uri.getPath();
        boolean googleDomain = host.equals("google.com") || host.endsWith(".google.com");
        if (googleDomain && path.startsWith("/maps/embed")) {
            return new Result(s, null);
        }
        if (googleDomain && path.startsWith("/maps")) {
            String sep = s.contains("?") ? "&" : "?";
            return new Result(s.contains("output=embed") ? s : s + sep + "output=embed", s);
        }
        // short links (maps.app.goo.gl, goo.gl/maps) cannot be framed: show a link instead
        return new Result(null, s);
    }

    private static String host(String url) {
        try {
            String h = URI.create(url).getHost();
            return h == null ? "" : h.toLowerCase(Locale.ROOT);
        } catch (Exception e) {
            return "";
        }
    }

    private static boolean isGoogleMapsHost(String host) {
        return host.equals("google.com") || host.endsWith(".google.com")
                || host.equals("maps.app.goo.gl") || host.equals("goo.gl");
    }
}
