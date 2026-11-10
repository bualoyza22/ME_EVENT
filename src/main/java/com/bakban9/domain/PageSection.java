package com.bakban9.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.hibernate.annotations.ColumnTransformer;

@Entity
@Table(name = "page_sections")
public class PageSection extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "page_id")
    private Page page;

    @Column(nullable = false)
    private String key;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "is_visible", nullable = false)
    private boolean visible = true;

    @Convert(converter = JsonMapConverter.class)
    @ColumnTransformer(write = "?::jsonb")
    @Column(nullable = false, columnDefinition = "jsonb")
    private Map<String, String> content = new LinkedHashMap<>();

    @Column(name = "media_ids", nullable = false, columnDefinition = "bigint[]")
    private Long[] mediaIds = new Long[0];

    protected PageSection() {}

    public PageSection(Page page, String key, int sortOrder) {
        this.page = page;
        this.key = key;
        this.sortOrder = sortOrder;
    }

    public String text(String field) {
        String v = content.get(field);
        return v == null ? "" : v;
    }

    public List<Long> getMediaIdList() {
        return mediaIds == null ? List.of() : new ArrayList<>(Arrays.asList(mediaIds));
    }

    public void setMediaIdList(List<Long> ids) {
        this.mediaIds = ids.toArray(new Long[0]);
    }

    public Page getPage() { return page; }
    public String getKey() { return key; }
    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
    public boolean isVisible() { return visible; }
    public void setVisible(boolean visible) { this.visible = visible; }
    public Map<String, String> getContent() { return content; }
    public void setContent(Map<String, String> content) { this.content = new LinkedHashMap<>(content); }
}
