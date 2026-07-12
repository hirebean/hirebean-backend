package bg.uni.sofia.fmi.spring.hirebean.service.impl;

import bg.uni.sofia.fmi.spring.hirebean.model.enums.ApplicationStatus;
import bg.uni.sofia.fmi.spring.hirebean.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.backend-url}")
    private String backendUrl;

    @Override
    @Async
    public void sendPasswordResetEmail(String toEmail, String resetToken) {
        String resetLink = backendUrl + "/api/users/password/reset-confirm?token=" + resetToken;

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("HireBean - Password Reset Request");
        message.setText("Hello,\n\n"
                + "You requested to reset your password.\n\n"
                + "Click the link below to reset it (valid for 30 minutes):\n"
                + resetLink
                + "\n\n"
                + "If you did not request this, please ignore this email.\n\n"
                + "Best regards,\nThe HireBean Team");

        mailSender.send(message);
    }

    @Override
    @Async
    public void sendApplicationStatusEmail(String toEmail, String jobTitle, ApplicationStatus status) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("HireBean - Application status updated");
        message.setText("Hello,\n\n"
                + "Your application for \""
                + jobTitle
                + "\" was updated.\n\n"
                + "Current status: "
                + status
                + "\n\n"
                + "Best regards,\nThe HireBean Team");

        mailSender.send(message);
    }
}
