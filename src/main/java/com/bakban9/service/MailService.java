package com.bakban9.service;

import com.bakban9.config.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.env.Environment;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/** Sends mail through SMTP when SMTP_HOST is configured; otherwise logs the message (development). */
@Service
public class MailService {

    private static final Logger log = LoggerFactory.getLogger(MailService.class);

    private final ObjectProvider<JavaMailSender> sender;
    private final Environment env;
    private final AppProperties props;

    public MailService(ObjectProvider<JavaMailSender> sender, Environment env, AppProperties props) {
        this.sender = sender;
        this.env = env;
        this.props = props;
    }

    public static class MailException extends Exception {
        public MailException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public boolean isConfigured() {
        String host = env.getProperty("spring.mail.host", "");
        return !host.isBlank() && sender.getIfAvailable() != null;
    }

    public void send(String to, String replyTo, String subject, String body) throws MailException {
        if (to == null || to.isBlank()) {
            return;
        }
        if (!isConfigured()) {
            log.info("[mail not configured] to={} subject={}\n{}", to, subject, body);
            return;
        }
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            String from = props.mail().from();
            if (from != null && !from.isBlank()) {
                msg.setFrom(from);
            }
            if (replyTo != null && !replyTo.isBlank()) {
                msg.setReplyTo(replyTo);
            }
            msg.setTo(to);
            msg.setSubject(subject);
            msg.setText(body);
            sender.getObject().send(msg);
        } catch (RuntimeException e) {
            throw new MailException("ส่งอีเมลไม่สำเร็จ", e);
        }
    }
}
