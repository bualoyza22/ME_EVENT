package com.bakban9.service;

import com.bakban9.domain.Media;
import com.bakban9.domain.MediaRepository;
import com.bakban9.domain.Product;
import com.bakban9.domain.ProductRepository;
import com.bakban9.util.Prices;
import com.bakban9.util.Texts;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductService {

    private final ProductRepository repo;
    private final MediaRepository media;
    private final AuditService audit;

    public ProductService(ProductRepository repo, MediaRepository media, AuditService audit) {
        this.repo = repo;
        this.media = media;
        this.audit = audit;
    }

    /** Admin edit form. {@code price} is the raw text so it can be shown back with the error. */
    public static class ProductForm {
        private String name = "";
        private String brand = "";
        private String price = "";
        private String condition = "";
        private String size = "";
        private String accessories = "";
        private String description = "";
        private boolean visible = true;
        private Integer sortOrder;
        private List<Long> imageIds = new ArrayList<>();

        public static ProductForm of(Product p) {
            ProductForm f = new ProductForm();
            f.name = p.getName();
            f.brand = p.getBrand();
            f.price = p.getPriceThb() == null ? "" : String.valueOf(p.getPriceThb());
            f.condition = p.getCondition();
            f.size = p.getSize();
            f.accessories = p.getAccessories();
            f.description = p.getDescription();
            f.visible = p.isVisible();
            f.sortOrder = p.getSortOrder();
            f.imageIds = new ArrayList<>(p.getImageMedia().stream().map(Media::getId).toList());
            return f;
        }

        public String getName() { return name; }
        public void setName(String v) { name = v; }
        public String getBrand() { return brand; }
        public void setBrand(String v) { brand = v; }
        public String getPrice() { return price; }
        public void setPrice(String v) { price = v; }
        public String getCondition() { return condition; }
        public void setCondition(String v) { condition = v; }
        public String getSize() { return size; }
        public void setSize(String v) { size = v; }
        public String getAccessories() { return accessories; }
        public void setAccessories(String v) { accessories = v; }
        public String getDescription() { return description; }
        public void setDescription(String v) { description = v; }
        public boolean isVisible() { return visible; }
        public void setVisible(boolean v) { visible = v; }
        public Integer getSortOrder() { return sortOrder; }
        public void setSortOrder(Integer v) { sortOrder = v; }
        public List<Long> getImageIds() { return imageIds; }
        public void setImageIds(List<Long> v) { imageIds = v == null ? new ArrayList<>() : v; }
    }

    public Map<String, String> validate(ProductForm f) {
        Map<String, String> errors = new LinkedHashMap<>();
        if (Texts.nz(f.getName()).isEmpty()) {
            errors.put("name", "กรุณาใส่ชื่อสินค้า");
        } else if (f.getName().trim().length() > 200) {
            errors.put("name", "ชื่อสินค้ายาวเกิน 200 ตัวอักษร");
        }
        if (Texts.nz(f.getBrand()).length() > 120) {
            errors.put("brand", "ชื่อแบรนด์ยาวเกิน 120 ตัวอักษร");
        }
        if (Prices.parse(f.getPrice()) instanceof Prices.Invalid inv) {
            errors.put("price", inv.message());
        }
        if (Texts.nz(f.getCondition()).length() > 120) errors.put("condition", "ยาวเกิน 120 ตัวอักษร");
        if (Texts.nz(f.getSize()).length() > 120) errors.put("size", "ยาวเกิน 120 ตัวอักษร");
        if (Texts.nz(f.getAccessories()).length() > 255) errors.put("accessories", "ยาวเกิน 255 ตัวอักษร");
        if (Texts.nz(f.getDescription()).length() > 5000) errors.put("description", "ยาวเกิน 5,000 ตัวอักษร");
        if (f.getImageIds().size() > 12) errors.put("images", "ใส่รูปได้ไม่เกิน 12 รูป");
        return errors;
    }

    @Transactional
    public Product save(Long id, ProductForm f, List<Media> uploaded) {
        boolean isNew = id == null;
        Product p = isNew ? new Product() : repo.findActiveById(id).orElseThrow(NotFoundException::new);
        p.setName(f.getName().trim());
        p.setBrand(Texts.nz(f.getBrand()));
        p.setPriceThb(Prices.parse(f.getPrice()) instanceof Prices.Valid v ? v.value() : null);
        p.setCondition(Texts.nz(f.getCondition()));
        p.setSize(Texts.nz(f.getSize()));
        p.setAccessories(Texts.nz(f.getAccessories()));
        p.setDescription(Texts.nz(f.getDescription()));
        p.setVisible(f.isVisible());
        p.setSortOrder(f.getSortOrder() != null ? f.getSortOrder() : (isNew ? repo.maxSortOrder() + 1 : p.getSortOrder()));

        List<Long> ids = f.getImageIds().stream().filter(x -> x != null).distinct().toList();
        Map<Long, Media> found = ids.isEmpty() ? Map.of()
                : media.findActiveByIds(ids).stream().collect(Collectors.toMap(Media::getId, Function.identity()));
        List<Media> ordered = new ArrayList<>(ids.stream().map(found::get).filter(x -> x != null).toList());
        ordered.addAll(uploaded);
        p.setImageMedia(ordered);
        repo.save(p);
        audit.log(isNew ? "product.create" : "product.update", "product", p.getId(), p.getName());
        return p;
    }

    @Transactional(readOnly = true)
    public Product get(Long id) {
        Product p = repo.findActiveById(id).orElseThrow(NotFoundException::new);
        p.getImages().size();
        return p;
    }

    @Transactional(readOnly = true)
    public List<Product> list(String q, String status) {
        String query = Texts.nz(q).toLowerCase(Locale.ROOT);
        List<Product> all = repo.findActive();
        all.forEach(p -> p.getImages().size());
        return all.stream()
                .filter(p -> query.isEmpty() || (p.getName() + " " + p.getBrand()).toLowerCase(Locale.ROOT).contains(query))
                .filter(p -> switch (status == null ? "" : status) {
                    case "shown" -> p.isVisible();
                    case "hidden" -> !p.isVisible();
                    default -> true;
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public long countAll() {
        return repo.findActive().size();
    }

    @Transactional
    public Product setVisible(Long id, boolean visible) {
        Product p = repo.findActiveById(id).orElseThrow(NotFoundException::new);
        p.setVisible(visible);
        audit.log(visible ? "product.show" : "product.hide", "product", id, p.getName());
        return p;
    }

    @Transactional
    public Product softDelete(Long id) {
        Product p = repo.findActiveById(id).orElseThrow(NotFoundException::new);
        p.setDeletedAt(Instant.now());
        audit.log("product.delete", "product", id, p.getName());
        return p;
    }

    @Transactional
    public int purgeDeletedBefore(Instant before) {
        List<Product> old = repo.findDeletedBefore(before);
        repo.deleteAll(old);
        return old.size();
    }
}
