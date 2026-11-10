package com.bakban9.util;

import java.text.NumberFormat;
import java.util.Locale;

public final class Texts {

    private Texts() {}

    public static final String PRICE_ON_REQUEST = "สอบถามราคา";

    public static String nz(String s) {
        return s == null ? "" : s.trim();
    }

    /** "1,290 บาท", or "สอบถามราคา" when the price is not set. */
    public static String formatPrice(Integer priceThb) {
        if (priceThb == null) {
            return PRICE_ON_REQUEST;
        }
        return NumberFormat.getIntegerInstance(Locale.US).format(priceThb) + " บาท";
    }

    /**
     * Lower-cased LIKE "contains" pattern with '\' as the escape character, so user input such
     * as "50%" or "a_b" matches literally.
     */
    public static String containsPattern(String query) {
        String q = nz(query).toLowerCase(Locale.ROOT);
        StringBuilder sb = new StringBuilder("%");
        for (char c : q.toCharArray()) {
            if (c == '\\' || c == '%' || c == '_') {
                sb.append('\\');
            }
            sb.append(c);
        }
        return sb.append('%').toString();
    }

    public static String truncate(String s, int max) {
        String v = nz(s);
        return v.length() <= max ? v : v.substring(0, max);
    }
}
