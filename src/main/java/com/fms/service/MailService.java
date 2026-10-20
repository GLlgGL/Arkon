package com.fms.service;

import com.fms.domain.AppConfig;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.nio.file.Path;
import java.util.Properties;
import org.springframework.core.io.FileSystemResource;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

/**
 * Sends emails (invoices, the test email) using the mail server entered under
 * Administration -> Email settings. Nothing is configured in files; when email
 * isn't set up, sending explains that instead of failing.
 */
@Service
public class MailService {

    private final ConfigService config;

    public MailService(ConfigService config) {
        this.config = config;
    }

    public boolean isReady() {
        return config.get().isMailReady();
    }

    public void sendInvoice(String to, String subject, String body, Path pdf) {
        send(to, subject, body, pdf);
    }

    public void sendTest(String to) {
        send(to, "IMS test email", "This is a test email from IMS – Inventory Management System.\n\n"
                + "If you can read this, the email settings work.\n", null);
    }

    private void send(String to, String subject, String body, Path attachment) {
        AppConfig c = config.get();
        if (!c.isMailReady()) {
            throw new OrderException("Email isn't set up yet. An administrator can set it up under Administration → Email settings.");
        }
        if (to == null || !to.trim().matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+")) {
            throw new OrderException("Enter a valid email address.");
        }
        try {
            JavaMailSenderImpl sender = sender(c);
            MimeMessage msg = sender.createMimeMessage();
            MimeMessageHelper h = new MimeMessageHelper(msg, attachment != null, "UTF-8");
            h.setFrom(c.getMailFrom());
            h.setTo(to.trim());
            h.setSubject(subject);
            h.setText(body, false);
            if (attachment != null) {
                h.addAttachment(attachment.getFileName().toString(), new FileSystemResource(attachment));
            }
            sender.send(msg);
        } catch (MessagingException | MailException e) {
            throw new OrderException("The email couldn't be sent: " + rootMessage(e)
                    + ". Check the server, port, security, username and password in Email settings.");
        }
    }

    private static JavaMailSenderImpl sender(AppConfig c) {
        JavaMailSenderImpl s = new JavaMailSenderImpl();
        s.setHost(c.getMailHost());
        s.setPort(c.getMailPort());
        s.setDefaultEncoding("UTF-8");
        Properties p = s.getJavaMailProperties();
        boolean auth = c.getMailUsername() != null;
        if (auth) {
            s.setUsername(c.getMailUsername());
            s.setPassword(c.getMailPassword());
        }
        p.put("mail.smtp.auth", String.valueOf(auth));
        p.put("mail.smtp.connectiontimeout", "15000");
        p.put("mail.smtp.timeout", "15000");
        p.put("mail.smtp.writetimeout", "15000");
        switch (c.getMailSecurity() == null ? "STARTTLS" : c.getMailSecurity()) {
            case "SSL" -> p.put("mail.smtp.ssl.enable", "true");
            case "NONE" -> { }
            default -> {
                p.put("mail.smtp.starttls.enable", "true");
                p.put("mail.smtp.starttls.required", "true");
            }
        }
        return s;
    }

    private static String rootMessage(Throwable e) {
        Throwable t = e;
        while (t.getCause() != null && t.getCause() != t) {
            t = t.getCause();
        }
        return t.getMessage() == null ? t.getClass().getSimpleName() : t.getMessage();
    }
}
