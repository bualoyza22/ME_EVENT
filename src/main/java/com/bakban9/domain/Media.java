package com.bakban9.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.hibernate.annotations.ColumnTransformer;

@Entity
@Table(name = "media")
public class Media extends BaseEntity {

    public static final String KIND_LOGO = "logo";
    public static final String KIND_PRODUCT = "product";
    public static final String KIND_OTHER = "other";

    @Column(nullable = false)
    private String filename;

    @Column(nullable = false)
    private String url;

    @Column(name = "storage_key", nullable = false)
    private String storageKey;

    @Column(nullable = false)
    private String mime;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    private Integer width;
    private Integer height;

    @Column(nullable = false)
    private String kind;

    @Column(name = "alt_text", nullable = false)
    private String altText = "";

    @Convert(converter = JsonMapConverter.class)
    @ColumnTransformer(write = "?::jsonb")
    @Column(name = "variant_urls", nullable = false, columnDefinition = "jsonb")
    private Map<String, String> variantUrls = new LinkedHashMap<>();

    @Column(name = "deleted_at")
    private Instant deletedAt;

    /** srcset value for responsive images; empty when there are no resized variants. */
    public String getSrcset() {
        if (variantUrls == null || variantUrls.isEmpty()) {
            return null;
        }
        String set = variantUrls.entrySet().stream()
                .sorted((a, b) -> Integer.compare(Integer.parseInt(a.getKey()), Integer.parseInt(b.getKey())))
                .map(e -> e.getValue() + " " + e.getKey() + "w")
                .collect(Collectors.joining(", "));
        if (width != null) {
            set = set + ", " + url + " " + width + "w";
        }
        return set;
    }

    public boolean isSvg() {
        return "image/svg+xml".equals(mime);
    }

    public String getFilename() { return filename; }
    public void setFilename(String filename) { this.filename = filename; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public String getStorageKey() { return storageKey; }
    public void setStorageKey(String storageKey) { this.storageKey = storageKey; }
    public String getMime() { return mime; }
    public void setMime(String mime) { this.mime = mime; }
    public long getSizeBytes() { return sizeBytes; }
    public void setSizeBytes(long sizeBytes) { this.sizeBytes = sizeBytes; }
    public Integer getWidth() { return width; }
    public void setWidth(Integer width) { this.width = width; }
    public Integer getHeight() { return height; }
    public void setHeight(Integer height) { this.height = height; }
    public String getKind() { return kind; }
    public void setKind(String kind) { this.kind = kind; }
    public String getAltText() { return altText; }
    public void setAltText(String altText) { this.altText = altText == null ? "" : altText; }
    public Map<String, String> getVariantUrls() { return variantUrls; }
    public void setVariantUrls(Map<String, String> variantUrls) { this.variantUrls = variantUrls; }
    public Instant getDeletedAt() { return deletedAt; }
    public void setDeletedAt(Instant deletedAt) { this.deletedAt = deletedAt; }
}
