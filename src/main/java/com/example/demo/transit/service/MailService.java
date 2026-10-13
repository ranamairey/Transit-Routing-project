package com.example.demo.transit.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Sends account emails through SMTP when SPRING_MAIL_HOST is configured; otherwise logs the
 * message (including the link) so development works without a mail server.
 */
@Service
public class MailService {

    private static final Logger log = LoggerFactory.getLogger(MailService.class);

    private final ObjectProvider<JavaMailSender> senderProvider;
    private final String host;
    private final String from;
    private final String publicUrl;

    public MailService(ObjectProvider<JavaMailSender> senderProvider,
                       @Value("${spring.mail.host:}") String host,
                       @Value("${app.mail.from:no-reply@transit.local}") String from,
                       @Value("${app.public-url:http://localhost:5173}") String publicUrl) {
        this.senderProvider = senderProvider;
        this.host = host;
        this.from = from;
        this.publicUrl = publicUrl.replaceAll("/+$", "");
    }

    public boolean isConfigured() {
        return host != null && !host.isBlank() && senderProvider.getIfAvailable() != null;
    }

    public String publicUrl() {
        return publicUrl;
    }

    public void send(String to, String subject, String body) {
        if (!isConfigured()) {
            log.info("[mail not configured] To: {} | {} | {}", to, subject, body.replace('\n', ' '));
            return;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            senderProvider.getObject().send(message);
        } catch (RuntimeException ex) {
            log.error("Failed to send mail to {}: {}", to, ex.getMessage());
        }
    }

    public void sendVerification(String to, String token) {
        String link = publicUrl + "/verify?token=" + token;
        send(to, "تأكيد البريد الإلكتروني | Verify your email",
                "مرحباً،\nلتأكيد بريدك في تطبيق مواصلات سوريا الذكية افتح الرابط التالي:\n" + link
                        + "\n\nHello,\nTo verify your email for Smart Syrian Transit open this link:\n" + link);
    }

    public void sendPasswordReset(String to, String token) {
        String link = publicUrl + "/reset?token=" + token;
        send(to, "إعادة تعيين كلمة السر | Reset your password",
                "مرحباً،\nلإعادة تعيين كلمة السر افتح الرابط التالي (صالح لمدة ساعة):\n" + link
                        + "\n\nHello,\nTo reset your password open this link (valid for one hour):\n" + link
                        + "\n\nإذا لم تطلب ذلك تجاهل هذه الرسالة. / If you did not request this, ignore this message.");
    }
}
