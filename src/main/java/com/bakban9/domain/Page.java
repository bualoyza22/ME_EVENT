package com.bakban9.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "pages")
public class Page extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String slug;

    @Column(nullable = false)
    private String title;

    @Column(name = "is_published", nullable = false)
    private boolean published = true;

    protected Page() {}

    public Page(String slug, String title) {
        this.slug = slug;
        this.title = title;
    }

    public String getSlug() { return slug; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public boolean isPublished() { return published; }
    public void setPublished(boolean published) { this.published = published; }
}
