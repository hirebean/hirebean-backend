package bg.uni.sofia.fmi.spring.hirebean.service;

import bg.uni.sofia.fmi.spring.hirebean.model.enums.ApplicationStatus;
import java.time.LocalDateTime;

public interface EmailService {

    void sendPasswordResetEmail(String toEmail, String resetToken);

    void sendApplicationStatusEmail(String toEmail, String jobTitle, ApplicationStatus status);

    void sendApplicationFeedbackEmail(
            String toEmail, String jobTitle, ApplicationStatus status, String feedbackMessage);

    void sendInterviewInvitationEmail(
            String toEmail, String jobTitle, LocalDateTime interviewAt, String interviewMessage, boolean updated);

    void sendInterviewCancellationEmail(String toEmail, String jobTitle);
}
