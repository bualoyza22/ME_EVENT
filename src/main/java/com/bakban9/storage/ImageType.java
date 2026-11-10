package com.bakban9.storage;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Optional;

/** Allowed upload types (spec: PNG, JPG, WEBP, SVG), detected from the file bytes, not the name. */
public enum ImageType {
    PNG("image/png", "png"),
    JPEG("image/jpeg", "jpg"),
    WEBP("image/webp", "webp"),
    SVG("image/svg+xml", "svg");

    public final String mime;
    public final String ext;

    ImageType(String mime, String ext) {
        this.mime = mime;
        this.ext = ext;
    }

    public static Optional<ImageType> detect(byte[] b) {
        if (b == null || b.length < 12) {
            return Optional.empty();
        }
        if ((b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G') {
            return Optional.of(PNG);
        }
        if ((b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) {
            return Optional.of(JPEG);
        }
        if (b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F' && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P') {
            return Optional.of(WEBP);
        }
        String head = new String(b, 0, Math.min(b.length, 4096), StandardCharsets.UTF_8)
                .replace("﻿", "").trim().toLowerCase(Locale.ROOT);
        if ((head.startsWith("<?xml") || head.startsWith("<svg") || head.startsWith("<!--")) && head.contains("<svg")) {
            return Optional.of(SVG);
        }
        return Optional.empty();
    }
}
