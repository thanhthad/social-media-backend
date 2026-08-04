package media.social.modules.auth.service.impl;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import media.social.modules.auth.exception.verification.EmailSendFailedException;
import media.social.modules.auth.service.EmailService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;


    @Override
    public void sendVerificationEmail(
            String email,
            String token
    ) {

        String verifyUrl =
                "http://localhost:8080/api/auth/verify-email?token="
                        + token;
        try {

            MimeMessage message =
                    mailSender.createMimeMessage();

            MimeMessageHelper helper =
                    new MimeMessageHelper(
                            message,
                            true,
                            "UTF-8"
                    );
            helper.setFrom(
                    fromEmail,
                    "Media Social"
            );
            helper.setTo(email);

            helper.setSubject(
                    "Verify your Media Social account"
            );
            String content = """
                    <html>
                    <body style="font-family: Arial, sans-serif;">

                        <h2>Welcome to Media Social 🎉</h2>

                        <p>
                            Thank you for creating your account.
                        </p>

                        <p>
                            Please click the button below to verify your email:
                        </p>

                        <a href="%s"
                           style="
                           display:inline-block;
                           padding:12px 20px;
                           background:#2563eb;
                           color:white;
                           text-decoration:none;
                           border-radius:6px;">
                           Verify Email
                        </a>
                        <p>
                            This verification link expires in 24 hours.
                        </p>
                        <p>
                            If you did not create this account, ignore this email.
                        </p>

                        <br>

                        <small>
                            Media Social Team
                        </small>

                    </body>
                    </html>
                    """.formatted(verifyUrl);
            helper.setText(content, true);
            mailSender.send(message);
        } catch (Exception e) {

            throw new EmailSendFailedException(
                    "Failed to send verification email"
            );
        }
    }
}
//@Service
//@RequiredArgsConstructor
//public class EmailServiceImpl implements EmailService {
//
//    private final JavaMailSender mailSender;
//
//    @Value("${app.frontend-url}")
//    private String frontendUrl;
//
//    @Value("${spring.mail.username}")
//    private String fromEmail;
//
//    @Override
//    public void sendVerificationEmail(String email, String token) {
//
//        String verifyUrl = frontendUrl + "/verify-email?token=" + token;
//
//        try {
//            MimeMessage message = mailSender.createMimeMessage();
//
//            MimeMessageHelper helper =
//                    new MimeMessageHelper(message, true, "UTF-8");
//
//            helper.setFrom(fromEmail, "Media Social");
//            helper.setTo(email);
//            helper.setSubject("Verify your Media Social account");
//
//            String content = """
//                    <html>
//                    <body style="font-family: Arial, sans-serif;">
//                        <h2>Welcome to Media Social 🎉</h2>
//
//                        <p>
//                            Thanks for creating your account.
//                            Please verify your email address to activate your account.
//                        </p>
//
//                        <a href="%s"
//                           style="
//                           display:inline-block;
//                           padding:12px 20px;
//                           background:#2563eb;
//                           color:white;
//                           text-decoration:none;
//                           border-radius:6px;">
//                           Verify Email
//                        </a>
//
//                        <p>
//                            This link will expire in 24 hours.
//                        </p>
//
//                        <p>
//                            If you did not create this account, ignore this email.
//                        </p>
//
//                        <br>
//
//                        <small>
//                            © 2026 Media Social
//                        </small>
//                    </body>
//                    </html>
//                    """.formatted(verifyUrl);
//
//            helper.setText(content, true);
//
//            mailSender.send(message);
//
//        } catch (Exception e) {
//            throw new EmailSendFailedException(
//                    "Failed to send verification email"
//            );
//        }
//    }
//}
