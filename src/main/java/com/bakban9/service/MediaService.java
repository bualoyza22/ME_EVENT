package com.bakban9.service;

import com.bakban9.domain.Media;
import com.bakban9.domain.MediaRepository;
import com.bakban9.domain.PageSection;
import com.bakban9.domain.PageSectionRepository;
import com.bakban9.domain.Product;
import com.bakban9.domain.ProductRepository;
import com.bakban9.domain.SiteSettingsRepository;
import com.bakban9.storage.ImageProcessor;
import com.bakban9.storage.ProcessedImage;
import com.bakban9.storage.StorageService;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MediaService {

    public static final Set<String> KINDS = Set.of(Media.KIND_LOGO, Media.KIND_PRODUCT, Media.KIND_OTHER);
    private static final DateTimeFormatter FOLDER = DateTimeFormatter.ofPattern("yyyy/MM").withZone(ZoneOffset.UTC);

    private final MediaRepository repo;
    private final ProductRepository products;
    private final PageSectionRepository sections;
    private final SiteSettingsRepository settings;
    private final ImageProcessor processor;
    private final StorageService storage;
    private final AuditService audit;

    public MediaService(MediaRepository repo, ProductRepository products, PageSectionRepository sections,
                        SiteSettingsRepository settings, ImageProcessor processor, StorageService storage, AuditService audit) {
        this.repo = repo;
        this.products = products;
        this.sections = sections;
        this.settings = settings;
        this.processor = processor;
        this.storage = storage;
        this.audit = audit;
    }

    public static class UploadException extends Exception {
        public UploadException(String message) {
            super(message);
        }
    }

    @Transactional
    public Media upload(String originalFilename, byte[] data, String kind, String altText) throws UploadException {
        String k = KINDS.contains(kind) ? kind : Media.KIND_OTHER;
        ProcessedImage img;
        try {
            img = processor.process(data);
        } catch (ImageProcessor.InvalidImageException e) {
            throw new UploadException(safeName(originalFilename) + ": " + e.getMessage());
        }
        String base = FOLDER.format(Instant.now()) + "/" + UUID.randomUUID();
        String key = base + "." + img.type().ext;
        Media m = new Media();
        m.setFilename(safeName(originalFilename));
        m.setStorageKey(key);
        m.setMime(img.type().mime);
        m.setSizeBytes(img.original().length);
        m.setWidth(img.width());
        m.setHeight(img.height());
        m.setKind(k);
        m.setAltText(altText);
        m.setUrl(storage.put(key, img.original(), img.type().mime));
        Map<String, String> variants = new LinkedHashMap<>();
        img.variants().forEach((w, bytes) -> variants.put(String.valueOf(w),
                storage.put(base + "-w" + w + "." + img.variantExt(), bytes, "jpg".equals(img.variantExt()) ? "image/jpeg" : "image/png")));
        m.setVariantUrls(variants);
        repo.save(m);
        audit.log("media.upload", "media", m.getId(), m.getFilename());
        return m;
    }

    /** Where a media file is used; deleting is blocked while this is not empty. */
    @Transactional(readOnly = true)
    public List<String> usage(Media m) {
        List<String> out = new ArrayList<>();
        settings.findSingleton().ifPresent(s -> {
            if (s.getLogo() != null && s.getLogo().getId().equals(m.getId())) {
                out.add("โลโก้หลัก");
            }
        });
        for (Product p : products.findUsingMedia(m.getId())) {
            out.add("สินค้า: " + p.getName());
        }
        if (repo.countProductUsage(m.getId()) > products.findUsingMedia(m.getId()).size()) {
            out.add("สินค้าที่ลบแล้ว (รอลบถาวร)");
        }
        for (PageSection s : sections.findUsingMedia(m.getId())) {
            String page = SectionDefs.page(s.getPage().getSlug()).map(SectionDefs.PageDef::label).orElse(s.getPage().getSlug());
            String sec = SectionDefs.section(s.getPage().getSlug(), s.getKey()).map(SectionDefs.SectionDef::label).orElse(s.getKey());
            out.add(page + " · " + sec);
        }
        return out;
    }

    public long usageCount(Long mediaId) {
        return repo.countProductUsage(mediaId) + repo.countSectionUsage(mediaId) + repo.countLogoUsage(mediaId);
    }

    @Transactional
    public void updateAlt(Long id, String alt) {
        Media m = repo.findActiveById(id).orElseThrow(NotFoundException::new);
        m.setAltText(alt == null ? "" : alt.trim());
        audit.log("media.alt", "media", id, m.getFilename());
    }

    /** Soft delete; refused while the file is in use. */
    @Transactional
    public void softDelete(Long id) throws UploadException {
        Media m = repo.findActiveById(id).orElseThrow(NotFoundException::new);
        if (usageCount(id) > 0) {
            throw new UploadException("ลบ " + m.getFilename() + " ไม่ได้ เพราะยังใช้อยู่: " + String.join(", ", usage(m)));
        }
        m.setDeletedAt(Instant.now());
        audit.log("media.delete", "media", id, m.getFilename());
    }

    /** Hard delete of files soft-deleted before the cut-off, including stored objects. */
    @Transactional
    public int purgeDeletedBefore(Instant before) {
        int n = 0;
        for (Media m : repo.findDeletedBefore(before)) {
            if (usageCount(m.getId()) > 0) {
                continue;
            }
            storage.delete(m.getStorageKey());
            String base = m.getStorageKey().replaceAll("\\.[a-z]+$", "");
            m.getVariantUrls().forEach((w, url) -> storage.delete(base + "-w" + w + url.substring(url.lastIndexOf('.'))));
            repo.delete(m);
            n++;
        }
        return n;
    }

    private static String safeName(String name) {
        String n = name == null ? "upload" : name.replace('\\', '/');
        n = n.substring(n.lastIndexOf('/') + 1).replaceAll("[\\p{Cntrl}]", "");
        return n.isBlank() ? "upload" : (n.length() > 200 ? n.substring(n.length() - 200) : n);
    }
}
