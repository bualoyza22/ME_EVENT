package com.bakban9.storage;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.imageio.ImageIO;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.stereotype.Component;

/** Validates and resizes uploads. Variants are only made when the original is wider. */
@Component
public class ImageProcessor {

    public static final long MAX_BYTES = 5L * 1024 * 1024;
    public static final int[] VARIANT_WIDTHS = {400, 1000};

    public static class InvalidImageException extends Exception {
        public InvalidImageException(String message) {
            super(message);
        }
    }

    public ProcessedImage process(byte[] data) throws InvalidImageException {
        if (data == null || data.length == 0) {
            throw new InvalidImageException("ไฟล์ว่าง");
        }
        if (data.length > MAX_BYTES) {
            throw new InvalidImageException("ไฟล์ใหญ่เกิน 5 MB");
        }
        ImageType type = ImageType.detect(data)
                .orElseThrow(() -> new InvalidImageException("รับเฉพาะไฟล์ PNG, JPG, WEBP หรือ SVG"));
        if (type == ImageType.SVG) {
            try {
                return new ProcessedImage(type, SvgSanitizer.sanitize(data), null, null, null, Map.of());
            } catch (SvgSanitizer.InvalidSvgException e) {
                throw new InvalidImageException(e.getMessage());
            }
        }
        BufferedImage img;
        try {
            img = ImageIO.read(new ByteArrayInputStream(data));
        } catch (IOException e) {
            img = null;
        }
        if (img == null) {
            throw new InvalidImageException("เปิดไฟล์รูปไม่ได้ ไฟล์อาจเสีย");
        }
        boolean alpha = img.getColorModel().hasAlpha();
        String format = alpha ? "png" : "jpg";
        Map<Integer, byte[]> variants = new LinkedHashMap<>();
        for (int w : VARIANT_WIDTHS) {
            if (img.getWidth() > w) {
                try {
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    Thumbnails.of(img).width(w).outputFormat(format).outputQuality(0.85).toOutputStream(out);
                    variants.put(w, out.toByteArray());
                } catch (IOException e) {
                    throw new InvalidImageException("ย่อขนาดรูปไม่สำเร็จ");
                }
            }
        }
        return new ProcessedImage(type, data, img.getWidth(), img.getHeight(), format, variants);
    }
}
