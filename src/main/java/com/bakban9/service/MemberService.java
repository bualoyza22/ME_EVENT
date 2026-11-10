package com.bakban9.service;

import com.bakban9.domain.Member;
import com.bakban9.domain.MemberRepository;
import com.bakban9.util.Csv;
import com.bakban9.util.Texts;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MemberService {

    public static final ZoneId BANGKOK = ZoneId.of("Asia/Bangkok");
    private static final DateTimeFormatter CSV_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(BANGKOK);

    private final MemberRepository repo;
    private final AuditService audit;

    public MemberService(MemberRepository repo, AuditService audit) {
        this.repo = repo;
        this.audit = audit;
    }

    public enum SignupResult { CREATED, ALREADY_MEMBER }

    /** Spec: duplicate email → tell the visitor they are already a member, do not add a row. */
    @Transactional
    public SignupResult signup(String name, String email, String phone, boolean consent) {
        String normalizedEmail = Texts.nz(email).toLowerCase();
        Optional<Member> existing = repo.findAnyByEmail(normalizedEmail);
        if (existing.isPresent()) {
            if (existing.get().getDeletedAt() == null) {
                return SignupResult.ALREADY_MEMBER;
            }
            // the person asked to be deleted earlier and is signing up again: start fresh
            repo.delete(existing.get());
            repo.flush();
        }
        Member m = new Member();
        m.setName(Texts.nz(name));
        m.setEmail(normalizedEmail);
        m.setPhone(Texts.nz(phone));
        m.setConsentMarketing(consent);
        m.setConsentAt(consent ? Instant.now() : null);
        repo.save(m);
        return SignupResult.CREATED;
    }

    @Transactional(readOnly = true)
    public List<Member> search(String q, String consentFilter) {
        Boolean consent = switch (consentFilter == null ? "" : consentFilter) {
            case "yes" -> Boolean.TRUE;
            case "no" -> Boolean.FALSE;
            default -> null;
        };
        String query = Texts.nz(q);
        return repo.search(query, Texts.containsPattern(query), consent);
    }

    @Transactional
    public void setCodeSent(Long id, boolean sent) {
        Member m = repo.findActiveById(id).orElseThrow(NotFoundException::new);
        m.setCodeSentAt(sent ? Instant.now() : null);
        audit.log(sent ? "member.code_sent" : "member.code_unsent", "member", id, m.getEmail());
    }

    /** PDPA delete request: soft delete now, purged permanently after the retention period. */
    @Transactional
    public void softDelete(Long id) {
        Member m = repo.findActiveById(id).orElseThrow(NotFoundException::new);
        m.setDeletedAt(Instant.now());
        audit.log("member.delete", "member", id, "ลบสมาชิก #" + id);
    }

    @Transactional
    public int purgeDeletedBefore(Instant before) {
        List<Member> old = repo.findDeletedBefore(before);
        repo.deleteAll(old);
        return old.size();
    }

    @Transactional
    public byte[] exportCsv(String q, String consentFilter) {
        List<List<String>> rows = new ArrayList<>();
        rows.add(List.of("ชื่อ", "อีเมล", "เบอร์โทร", "วันที่สมัคร", "รับข่าวสาร", "เวลาที่ยินยอม", "ส่งโค้ดแล้ว"));
        List<Member> members = search(q, consentFilter);
        for (Member m : members) {
            rows.add(List.of(m.getName(), m.getEmail(), m.getPhone(), CSV_TIME.format(m.getCreatedAt()),
                    m.isConsentMarketing() ? "ใช่" : "ไม่", m.getConsentAt() == null ? "" : CSV_TIME.format(m.getConsentAt()),
                    m.getCodeSentAt() == null ? "ยังไม่ส่ง" : "ส่งแล้ว " + CSV_TIME.format(m.getCodeSentAt())));
        }
        audit.log("member.export", "member", null, "ดาวน์โหลด CSV " + members.size() + " รายการ");
        return Csv.toBytes(rows);
    }
}
