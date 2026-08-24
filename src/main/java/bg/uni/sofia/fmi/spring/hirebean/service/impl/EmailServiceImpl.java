package bg.uni.sofia.fmi.spring.hirebean.service.impl;

import bg.uni.sofia.fmi.spring.hirebean.model.enums.ApplicationStatus;
import bg.uni.sofia.fmi.spring.hirebean.service.EmailService;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private static final DateTimeFormatter INTERVIEW_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");

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

    @Override
    @Async
    public void sendApplicationFeedbackEmail(
            String toEmail, String jobTitle, ApplicationStatus status, String feedbackMessage) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("HireBean - Feedback on your application");
        message.setText("Hello,\n\n"
                + "Your application for \"" + jobTitle + "\" was reviewed.\n\n"
                + "Current status: " + status + "\n\n"
                + (feedbackMessage == null || feedbackMessage.isBlank()
                        ? "No additional feedback was provided."
                        : "Feedback from the employer:\n" + feedbackMessage)
                + "\n\nBest regards,\nThe HireBean Team");
        mailSender.send(message);
    }

    @Override
    @Async
    public void sendInterviewInvitationEmail(
            String toEmail, String jobTitle, LocalDateTime interviewAt, String interviewMessage, boolean updated) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject(updated ? "HireBean - Interview updated" : "HireBean - Interview invitation");
        message.setText("Hello,\n\n"
                + (updated ? "Your interview was updated" : "You are invited to an interview")
                + " for \"" + jobTitle + "\".\n\n"
                + "Date and time: " + interviewAt.format(INTERVIEW_DATE_FORMATTER) + "\n"
                + (interviewMessage == null || interviewMessage.isBlank()
                        ? ""
                        : "Message from the employer:\n" + interviewMessage + "\n")
                + "\nBest regards,\nThe HireBean Team");
        mailSender.send(message);
    }

    @Override
    @Async
    public void sendInterviewCancellationEmail(String toEmail, String jobTitle) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("HireBean - Interview cancelled");
        message.setText("Hello,\n\n"
                + "The interview for \"" + jobTitle + "\" was cancelled by the employer.\n\n"
                + "Best regards,\nThe HireBean Team");
        mailSender.send(message);
    }
}
