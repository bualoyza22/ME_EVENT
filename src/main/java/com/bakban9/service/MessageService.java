package com.bakban9.service;

import com.bakban9.config.AppProperties;
import com.bakban9.domain.Message;
import com.bakban9.domain.MessageRepository;
import com.bakban9.util.Texts;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MessageService {

    private static final Logger log = LoggerFactory.getLogger(MessageService.class);

    private final MessageRepository repo;
    private final MailService mail;
    private final SettingsService settings;
    private final AppProperties props;
    private final AuditService audit;

    public MessageService(MessageRepository repo, MailService mail, SettingsService settings, AppProperties props,
                          AuditService audit) {
        this.repo = repo;
        this.mail = mail;
        this.settings = settings;
        this.props = props;
        this.audit = audit;
    }

    /** Saves a contact-form message and notifies the shop by email (failure to notify is logged, not fatal). */
    @Transactional
    public Message create(String name, String email, String subject, String body) {
        Message m = new Message();
        m.setName(Texts.nz(name));
        m.setEmail(Texts.nz(email));
        m.setSubject(Texts.nz(subject));
        m.setBody(Texts.nz(body));
        repo.save(m);
        String to = Texts.nz(props.mail().notifyTo()).isEmpty() ? settings.get().getEmail() : props.mail().notifyTo();
        try {
            mail.send(to, m.getEmail(), "[BAKBAN9] ข้อความใหม่: " + (m.getSubject().isEmpty() ? "(ไม่มีหัวข้อ)" : m.getSubject()),
                    "จาก: " + m.getName() + " <" + m.getEmail() + ">\n\n" + m.getBody()
                            + "\n\nเปิดอ่านและตอบกลับได้ที่ " + props.baseUrl() + "/admin/messages?id=" + m.getId());
        } catch (MailService.MailException e) {
            log.warn("could not notify shop about message {}", m.getId(), e);
        }
        return m;
    }

    @Transactional(readOnly = true)
    public List<Message> list(String filter) {
        String status = switch (filter == null ? "" : filter) {
            case "open" -> Message.OPEN;
            case "replied" -> Message.REPLIED;
            default -> null;
        };
        return repo.findByStatusFilter(status);
    }

    @Transactional(readOnly = true)
    public List<Message> latest(int n) {
        return repo.findLatest(Limit.of(n));
    }

    @Transactional(readOnly = true)
    public long countOpen() {
        return repo.countByStatus(Message.OPEN);
    }

    @Transactional(readOnly = true)
    public Message get(Long id) {
        return repo.findById(id).orElseThrow(NotFoundException::new);
    }

    @Transactional
    public void reply(Long id, String body) throws MailService.MailException {
        Message m = get(id);
        String text = Texts.nz(body);
        String shopEmail = settings.get().getEmail();
        mail.send(m.getEmail(), shopEmail, "Re: " + (m.getSubject().isEmpty() ? "ข้อความถึง BAKBAN9" : m.getSubject()),
                text + "\n\n— BAKBAN9\n\n> " + m.getBody().replace("\n", "\n> "));
        m.setReplyBody(text);
        m.setStatus(Message.REPLIED);
        m.setRepliedAt(Instant.now());
        audit.log("message.reply", "message", id, m.getEmail());
    }

    @Transactional
    public void setReplied(Long id, boolean replied) {
        Message m = get(id);
        m.setStatus(replied ? Message.REPLIED : Message.OPEN);
        m.setRepliedAt(replied ? Instant.now() : null);
        audit.log(replied ? "message.mark_replied" : "message.mark_open", "message", id, m.getEmail());
    }
}
