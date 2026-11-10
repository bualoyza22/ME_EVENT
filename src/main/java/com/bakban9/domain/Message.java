package com.bakban9.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "messages")
public class Message extends BaseEntity {

    public static final String OPEN = "open";
    public static final String REPLIED = "replied";

    @Column(nullable = false) private String name;
    @Column(nullable = false) private String email;
    @Column(nullable = false) private String subject = "";
    @Column(nullable = false) private String body;
    @Column(nullable = false) private String status = OPEN;
    @Column(name = "replied_at") private Instant repliedAt;
    @Column(name = "reply_body") private String replyBody;

    public boolean isReplied() {
        return REPLIED.equals(status);
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject == null ? "" : subject; }
    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Instant getRepliedAt() { return repliedAt; }
    public void setRepliedAt(Instant repliedAt) { this.repliedAt = repliedAt; }
    public String getReplyBody() { return replyBody; }
    public void setReplyBody(String replyBody) { this.replyBody = replyBody; }
}
