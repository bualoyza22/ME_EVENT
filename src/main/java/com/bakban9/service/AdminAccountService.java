package com.bakban9.service;

import com.bakban9.config.AppProperties;
import com.bakban9.domain.Admin;
import com.bakban9.domain.AdminInvite;
import com.bakban9.domain.AdminInviteRepository;
import com.bakban9.domain.AdminRepository;
import com.bakban9.util.Texts;
import com.bakban9.util.UrlRules;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminAccountService {

    public static final int MIN_PASSWORD = 8;
    private static final Duration INVITE_TTL = Duration.ofDays(3);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final AdminRepository admins;
    private final AdminInviteRepository invites;
    private final PasswordEncoder encoder;
    private final MailService mail;
    private final AppProperties props;
    private final AuditService audit;

    public AdminAccountService(AdminRepository admins, AdminInviteRepository invites, PasswordEncoder encoder,
                               MailService mail, AppProperties props, AuditService audit) {
        this.admins = admins;
        this.invites = invites;
        this.encoder = encoder;
        this.mail = mail;
        this.props = props;
        this.audit = audit;
    }

    public static class AccountException extends Exception {
        public AccountException(String message) {
            super(message);
        }
    }

    /** Creates the first owner from OWNER_EMAIL / OWNER_PASSWORD when no admin exists. */
    @Transactional
    public boolean bootstrapOwner() {
        AppProperties.Owner o = props.owner();
        if (admins.count() > 0 || o == null || Texts.nz(o.email()).isEmpty() || Texts.nz(o.password()).isEmpty()) {
            return false;
        }
        if (o.password().length() < MIN_PASSWORD) {
            throw new IllegalStateException("OWNER_PASSWORD must be at least " + MIN_PASSWORD + " characters");
        }
        createAdmin(Texts.nz(o.name()).isEmpty() ? "เจ้าของร้าน" : o.name(), o.email(), o.password(), Admin.OWNER);
        return true;
    }

    @Transactional
    public Admin createAdmin(String name, String email, String rawPassword, String role) {
        Admin a = new Admin();
        a.setName(Texts.nz(name));
        a.setEmail(Texts.nz(email).toLowerCase());
        a.setPasswordHash(encoder.encode(rawPassword));
        a.setRole(role);
        return admins.save(a);
    }

    @Transactional(readOnly = true)
    public List<Admin> all() {
        return admins.findAllOrdered();
    }

    @Transactional(readOnly = true)
    public List<AdminInvite> pendingInvites() {
        return invites.findPending();
    }

    @Transactional
    public void changePassword(Long adminId, String current, String next) throws AccountException {
        Admin a = admins.findById(adminId).orElseThrow(NotFoundException::new);
        if (current == null || !encoder.matches(current, a.getPasswordHash())) {
            throw new AccountException("รหัสผ่านปัจจุบันไม่ถูกต้อง");
        }
        validatePassword(next);
        a.setPasswordHash(encoder.encode(next));
        audit.log("admin.password", "admin", adminId, "เปลี่ยนรหัสผ่าน");
    }

    /** Owner invites an editor by email; returns the accept link (also emailed). */
    @Transactional
    public String invite(String email, Long invitedBy) throws AccountException {
        String e = Texts.nz(email).toLowerCase();
        if (!UrlRules.isValidEmail(e)) {
            throw new AccountException("รูปแบบอีเมลไม่ถูกต้อง");
        }
        if (admins.findByEmailIgnoreCase(e).isPresent()) {
            throw new AccountException("อีเมลนี้เป็นผู้ดูแลอยู่แล้ว");
        }
        byte[] raw = new byte[32];
        RANDOM.nextBytes(raw);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
        AdminInvite inv = new AdminInvite();
        inv.setEmail(e);
        inv.setRole(Admin.EDITOR);
        inv.setTokenHash(sha256(token));
        inv.setInvitedBy(invitedBy);
        inv.setExpiresAt(Instant.now().plus(INVITE_TTL));
        invites.save(inv);
        String link = props.baseUrl() + "/admin/invite/" + token;
        try {
            mail.send(e, null, "คำเชิญเป็นผู้ดูแลเว็บไซต์ BAKBAN9",
                    "คุณได้รับคำเชิญให้เป็นผู้ดูแลเว็บไซต์ BAKBAN9\n\nตั้งรหัสผ่านได้ที่ลิงก์นี้ภายใน 3 วัน:\n" + link);
        } catch (MailService.MailException ex) {
            throw new AccountException("สร้างคำเชิญแล้ว แต่ส่งอีเมลไม่สำเร็จ ส่งลิงก์นี้ให้ผู้ดูแลเอง: " + link);
        }
        audit.log("admin.invite", "admin_invite", inv.getId(), e);
        return link;
    }

    @Transactional(readOnly = true)
    public Optional<AdminInvite> findUsableInvite(String token) {
        return invites.findByTokenHash(sha256(token == null ? "" : token)).filter(AdminInvite::isUsable);
    }

    @Transactional
    public Admin acceptInvite(String token, String name, String password) throws AccountException {
        AdminInvite inv = findUsableInvite(token).orElseThrow(() -> new AccountException("ลิงก์คำเชิญหมดอายุหรือถูกใช้ไปแล้ว"));
        if (Texts.nz(name).isEmpty()) {
            throw new AccountException("กรุณาใส่ชื่อ");
        }
        validatePassword(password);
        if (admins.findByEmailIgnoreCase(inv.getEmail()).isPresent()) {
            throw new AccountException("อีเมลนี้เป็นผู้ดูแลอยู่แล้ว");
        }
        Admin a = createAdmin(name, inv.getEmail(), password, inv.getRole());
        inv.setAcceptedAt(Instant.now());
        audit.log("admin.accept_invite", "admin", a.getId(), a.getEmail());
        return a;
    }

    @Transactional
    public void setActive(Long adminId, boolean active, Long actingAdminId) throws AccountException {
        Admin a = admins.findById(adminId).orElseThrow(NotFoundException::new);
        if (a.isOwner()) {
            throw new AccountException("ปิดบัญชีเจ้าของร้านไม่ได้");
        }
        if (a.getId().equals(actingAdminId)) {
            throw new AccountException("ปิดบัญชีของตัวเองไม่ได้");
        }
        a.setActive(active);
        audit.log(active ? "admin.activate" : "admin.deactivate", "admin", adminId, a.getEmail());
    }

    @Transactional
    public void unlock(Long adminId) {
        Admin a = admins.findById(adminId).orElseThrow(NotFoundException::new);
        a.setFailedAttempts(0);
        a.setLockedUntil(null);
        audit.log("admin.unlock", "admin", adminId, a.getEmail());
    }

    public static void validatePassword(String password) throws AccountException {
        if (password == null || password.length() < MIN_PASSWORD) {
            throw new AccountException("รหัสผ่านต้องมีอย่างน้อย " + MIN_PASSWORD + " ตัว");
        }
        if (password.length() > 72) {
            throw new AccountException("รหัสผ่านยาวได้ไม่เกิน 72 ตัว");
        }
    }

    static String sha256(String s) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
