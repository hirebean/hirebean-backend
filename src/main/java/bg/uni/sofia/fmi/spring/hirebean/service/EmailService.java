package bg.uni.sofia.fmi.spring.hirebean.service;

import bg.uni.sofia.fmi.spring.hirebean.model.enums.ApplicationStatus;

public interface EmailService {

    void sendPasswordResetEmail(String toEmail, String resetToken);

    void sendApplicationStatusEmail(String toEmail, String jobTitle, ApplicationStatus status);
}
