package com.bakban9.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "audit_logs")
public class AuditLog extends BaseEntity {

    @Column(name = "admin_id") private Long adminId;
    @Column(name = "admin_email", nullable = false) private String adminEmail = "";
    @Column(nullable = false) private String action;
    @Column(name = "entity_type", nullable = false) private String entityType;
    @Column(name = "entity_id") private Long entityId;
    @Column(nullable = false) private String summary = "";

    protected AuditLog() {}

    public AuditLog(Long adminId, String adminEmail, String action, String entityType, Long entityId, String summary) {
        this.adminId = adminId;
        this.adminEmail = adminEmail == null ? "" : adminEmail;
        this.action = action;
        this.entityType = entityType;
        this.entityId = entityId;
        this.summary = summary == null ? "" : (summary.length() > 500 ? summary.substring(0, 500) : summary);
    }

    public Long getAdminId() { return adminId; }
    public String getAdminEmail() { return adminEmail; }
    public String getAction() { return action; }
    public String getEntityType() { return entityType; }
    public Long getEntityId() { return entityId; }
    public String getSummary() { return summary; }
}
