package bg.uni.sofia.fmi.spring.hirebean.service;

public interface EmailService {

    void sendPasswordResetEmail(String toEmail, String resetToken);
}
