package com.example.workflow_management_system.service;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class MailService {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(MailService.class);

    private final JavaMailSender emailSender;
    private final org.springframework.core.io.ResourceLoader resourceLoader;
    @org.springframework.beans.factory.annotation.Value("${app.frontend.url}")
    private String frontendUrl;

    @org.springframework.beans.factory.annotation.Value("${spring.mail.host}")
    private String mailHost;

    @org.springframework.beans.factory.annotation.Value("${spring.mail.port}")
    private String mailPort;

    @org.springframework.beans.factory.annotation.Value("${spring.mail.username}")
    private String mailUsername;

    @jakarta.annotation.PostConstruct
    public void logMailConfig() {
        logger.info("MailService Initialized with Config: Host={}, Port={}, Username={}", mailHost, mailPort,
                mailUsername);
    }

    public MailService(JavaMailSender emailSender, org.springframework.core.io.ResourceLoader resourceLoader) {
        this.emailSender = emailSender;
        this.resourceLoader = resourceLoader;
    }

    public void sendMail(String to, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);
        emailSender.send(message);
    }

    @org.springframework.scheduling.annotation.Async
    public void sendInvitationEmail(String to, String name, String tenantName, String token) {
        try {
            jakarta.mail.internet.MimeMessage mimeMessage = emailSender.createMimeMessage();
            org.springframework.mail.javamail.MimeMessageHelper helper = new org.springframework.mail.javamail.MimeMessageHelper(
                    mimeMessage, "utf-8");

            String inviteLink = frontendUrl + "/set-password?token=" + token;
            String htmlMsg = loadTemplate("invitation.html");

            htmlMsg = htmlMsg.replace("{{name}}", name)
                    .replace("{{tenantName}}", tenantName)
                    .replace("{{inviteLink}}", inviteLink);

            helper.setText(htmlMsg, true);
            helper.setTo(to);
            helper.setSubject("You're invited to join " + tenantName);
            helper.setFrom("noreply@workflowsystem.com");

            emailSender.send(mimeMessage);
        } catch (Exception e) {
            // Log error but don't rethrow to avoid breaking async flow validation
            logger.error("Failed to send invitation email to {}", to, e);
        }
    }

    @org.springframework.scheduling.annotation.Async
    public void sendResetPasswordEmail(String to, String name, String token) {
        try {
            jakarta.mail.internet.MimeMessage mimeMessage = emailSender.createMimeMessage();
            org.springframework.mail.javamail.MimeMessageHelper helper = new org.springframework.mail.javamail.MimeMessageHelper(
                    mimeMessage, "utf-8");

            String resetLink = frontendUrl + "/reset-password?token=" + token;
            String htmlMsg = loadTemplate("password_reset.html");

            htmlMsg = htmlMsg.replace("{{name}}", name)
                    .replace("{{resetLink}}", resetLink);

            helper.setText(htmlMsg, true);
            helper.setTo(to);
            helper.setSubject("Reset your password");
            helper.setFrom("noreply@workflowsystem.com");

            emailSender.send(mimeMessage);
        } catch (Exception e) {
            logger.error("Failed to send password reset email to {}", to, e);
        }
    }

    public void sendHtmlMail(String to, String subject, String htmlBody) {
        try {
            logger.info("Preparing to send HTML email to: {}", to);
            jakarta.mail.internet.MimeMessage mimeMessage = emailSender.createMimeMessage();
            org.springframework.mail.javamail.MimeMessageHelper helper = new org.springframework.mail.javamail.MimeMessageHelper(
                    mimeMessage, "utf-8");

            helper.setText(htmlBody, true);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setFrom("noreply@workflowsystem.com");

            logger.info("Dispatching email via JavaMailSender...");
            emailSender.send(mimeMessage);
            logger.info("Email dispatched successfully to {}", to);
        } catch (Exception e) {
            logger.error("Failed to send HTML email to {}", to, e);
        }
    }

    public String loadTemplate(String templateName) {
        org.springframework.core.io.Resource resource = resourceLoader
                .getResource("classpath:templates/email/" + templateName);
        try (java.io.InputStream inputStream = resource.getInputStream()) {
            return new String(inputStream.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        } catch (java.io.IOException e) {
            logger.error("Failed to load email template: {}", templateName, e);
            return "";
        }
    }
}
