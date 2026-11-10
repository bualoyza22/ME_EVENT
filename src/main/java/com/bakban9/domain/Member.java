package com.bakban9.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "members")
public class Member extends BaseEntity {

    @Column(nullable = false) private String name;
    @Column(nullable = false) private String email;
    @Column(nullable = false) private String phone = "";
    @Column(name = "consent_marketing", nullable = false) private boolean consentMarketing;
    @Column(name = "consent_at") private Instant consentAt;
    @Column(name = "code_sent_at") private Instant codeSentAt;
    @Column(name = "deleted_at") private Instant deletedAt;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone == null ? "" : phone; }
    public boolean isConsentMarketing() { return consentMarketing; }
    public void setConsentMarketing(boolean consentMarketing) { this.consentMarketing = consentMarketing; }
    public Instant getConsentAt() { return consentAt; }
    public void setConsentAt(Instant consentAt) { this.consentAt = consentAt; }
    public Instant getCodeSentAt() { return codeSentAt; }
    public void setCodeSentAt(Instant codeSentAt) { this.codeSentAt = codeSentAt; }
    public Instant getDeletedAt() { return deletedAt; }
    public void setDeletedAt(Instant deletedAt) { this.deletedAt = deletedAt; }
}
