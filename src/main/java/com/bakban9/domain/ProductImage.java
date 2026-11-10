package com.bakban9.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "product_images")
public class ProductImage extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id")
    private Product product;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "media_id")
    private Media media;

    /** 0 = main image. */
    @Column(nullable = false)
    private int position;

    protected ProductImage() {}

    public ProductImage(Product product, Media media, int position) {
        this.product = product;
        this.media = media;
        this.position = position;
    }

    public Product getProduct() { return product; }
    public Media getMedia() { return media; }
    public int getPosition() { return position; }
}
