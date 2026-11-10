package com.bakban9.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "products")
public class Product extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String brand = "";

    /** null = price not set yet; the shop shows "สอบถามราคา". */
    @Column(name = "price_thb")
    private Integer priceThb;

    @Column(nullable = false)
    private String condition = "";

    @Column(nullable = false)
    private String size = "";

    @Column(nullable = false)
    private String accessories = "";

    @Column(nullable = false)
    private String description = "";

    @Column(name = "is_visible", nullable = false)
    private boolean visible = true;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC")
    private List<ProductImage> images = new ArrayList<>();

    public Media getMainImage() {
        return images.isEmpty() ? null : images.get(0).getMedia();
    }

    public List<Media> getImageMedia() {
        return images.stream().map(ProductImage::getMedia).toList();
    }

    /** Replaces the image list; the first media becomes the main image (position 0). */
    public void setImageMedia(List<Media> media) {
        images.clear();
        for (int i = 0; i < media.size(); i++) {
            images.add(new ProductImage(this, media.get(i), i));
        }
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand == null ? "" : brand; }
    public Integer getPriceThb() { return priceThb; }
    public void setPriceThb(Integer priceThb) { this.priceThb = priceThb; }
    public String getCondition() { return condition; }
    public void setCondition(String condition) { this.condition = condition == null ? "" : condition; }
    public String getSize() { return size; }
    public void setSize(String size) { this.size = size == null ? "" : size; }
    public String getAccessories() { return accessories; }
    public void setAccessories(String accessories) { this.accessories = accessories == null ? "" : accessories; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description == null ? "" : description; }
    public boolean isVisible() { return visible; }
    public void setVisible(boolean visible) { this.visible = visible; }
    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
    public Instant getDeletedAt() { return deletedAt; }
    public void setDeletedAt(Instant deletedAt) { this.deletedAt = deletedAt; }
    public List<ProductImage> getImages() { return images; }
}
