package com.bakban9.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** Single row: shop info used by the Footer and the contact page. */
@Entity
@Table(name = "site_settings")
public class SiteSettings extends BaseEntity {

    @Column(name = "site_name", nullable = false)
    private String siteName = "";

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "logo_media_id")
    private Media logo;

    @Column(nullable = false) private String tagline = "";
    @Column(nullable = false) private String website = "";
    @Column(nullable = false) private String address = "";
    @Column(nullable = false) private String hours = "";
    @Column(nullable = false) private String tel = "";
    @Column(nullable = false) private String email = "";
    @Column(name = "instagram_url", nullable = false) private String instagramUrl = "";
    @Column(name = "line_url", nullable = false) private String lineUrl = "";
    @Column(name = "facebook_url", nullable = false) private String facebookUrl = "";
    @Column(name = "tiktok_url", nullable = false) private String tiktokUrl = "";
    @Column(name = "map_url", nullable = false) private String mapUrl = "";

    private static String nz(String s) {
        return s == null ? "" : s.trim();
    }

    public String getSiteName() { return siteName; }
    public void setSiteName(String v) { this.siteName = nz(v); }
    public Media getLogo() { return logo; }
    public void setLogo(Media logo) { this.logo = logo; }
    public String getTagline() { return tagline; }
    public void setTagline(String v) { this.tagline = nz(v); }
    public String getWebsite() { return website; }
    public void setWebsite(String v) { this.website = nz(v); }
    public String getAddress() { return address; }
    public void setAddress(String v) { this.address = nz(v); }
    public String getHours() { return hours; }
    public void setHours(String v) { this.hours = nz(v); }
    public String getTel() { return tel; }
    public void setTel(String v) { this.tel = nz(v); }
    public String getEmail() { return email; }
    public void setEmail(String v) { this.email = nz(v); }
    public String getInstagramUrl() { return instagramUrl; }
    public void setInstagramUrl(String v) { this.instagramUrl = nz(v); }
    public String getLineUrl() { return lineUrl; }
    public void setLineUrl(String v) { this.lineUrl = nz(v); }
    public String getFacebookUrl() { return facebookUrl; }
    public void setFacebookUrl(String v) { this.facebookUrl = nz(v); }
    public String getTiktokUrl() { return tiktokUrl; }
    public void setTiktokUrl(String v) { this.tiktokUrl = nz(v); }
    public String getMapUrl() { return mapUrl; }
    public void setMapUrl(String v) { this.mapUrl = nz(v); }
}
