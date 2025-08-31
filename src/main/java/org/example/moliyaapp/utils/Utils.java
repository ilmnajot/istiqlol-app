package org.example.moliyaapp.utils;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.example.moliyaapp.entity.Code;
import org.example.moliyaapp.entity.User;
import org.example.moliyaapp.repository.CodeRepository;
import org.example.moliyaapp.repository.UserRepository;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Optional;
import java.util.Random;

@Component
@RequiredArgsConstructor
public class Utils {

//    private final JavaMailSender mailSender;
    private final CodeRepository codeRepository;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;


    public boolean checkPhoneNumber(String phoneNumber) {
        if (phoneNumber != null && !phoneNumber.trim().isEmpty())
            return phoneNumber.matches("^[0-9]{9}$");
        else return false;
    }

    public boolean checkEmail(String email) {
        if (email == null || email.isEmpty())
            return false;
        return email.contains("@");
    }

    public String getSecretKey() {
        return jwtUtil.getSecretKeyAsString();
    }

    public String getCode() {
        Random random = new Random();
        int randomCode = 10000 + random.nextInt(90000); // Generates a number between 10000 and 99999
        return String.valueOf(randomCode);
    }

    public boolean checkCode(String email, String code) {
        Optional<User> optionalUser = this.userRepository.findByEmail(email);
        if (optionalUser.isPresent()) {
            User user = optionalUser.get();
            Optional<Code> optional = this.codeRepository.findByUserId(user.getId());
            if (optional.isPresent()) {
                Code c = optional.get();
                return code != null && !code.trim().isEmpty() && c.getCode().equals(code);
            }
        }
        return false;
    }


    public boolean sendCodeToMail(String mail, String code) {
        String body = String.format("<p class=\"code\">%s</p>", code);
        String htmlContent = """
                <!DOCTYPE html>
                              <html lang="en">
                              <head>
                                  <meta charset="UTF-8">
                                  <meta name="viewport" content="width=device-width, initial-scale=1.0">
                                  <style>
                                      body {
                                          font-family: Arial, sans-serif;
                                          background-color: #f4f4f4;
                                          margin: 0;
                                          padding: 0;
                                      }
                                      .container {
                                          max-width: 600px;
                                          margin: 50px auto;
                                          background-color: #ffffff;
                                          border-radius: 8px;
                                          box-shadow: 0 0 10px rgba(0, 0, 0, 0.1);
                                          overflow: hidden;
                                      }
                                      .header {
                                          background-color: #145214;
                                          color: white;
                                          text-align: center;
                                          padding: 20px;
                                      }
                                      .content {
                                          padding: 30px;
                                          text-align: center;
                                      }
                                      .footer {
                                          background-color: #f1f1f1;
                                          color: #777;
                                          text-align: center;
                                          padding: 15px;
                                          font-size: 12px;
                                      }
                                      a {
                                          color: #4CAF50;
                                          text-decoration: none;
                                      }
                                      .code {
                                          font-size: 24px;
                                          font-weight: bold;
                                          color: #333;
                                          background-color: #f1f1f1;
                                          padding: 10px 20px;
                                          border-radius: 4px;
                                          margin: 20px auto;
                                          display: inline-block;
                                      }
                                  </style>
                              </head>
                              <body>
                                  <div class="container">
                                      <div class="header">
                                          <h2>%s</h2>
                                      </div>
                                      <div class="content">
                                       %s
                                      </div>
                                      <div class="footer">
                                          <p><b>© 2025 https://iftixorschool.uz/</b></p>
                                      </div>
                                  </div>
                              </body>
                              </html>
                """.formatted("Tasdiqlash kodi: ", body);

        //            MimeMessage message = mailSender.createMimeMessage();
//            MimeMessageHelper helper = new MimeMessageHelper(message);
//            helper.setFrom("xalqaroshartnomalaruz@gmail.com");
//            helper.setTo(mail);
//            helper.setSubject("Moliya - APP");
//            helper.setText(htmlContent, true);
//            mailSender.send(message);
        return true;

    }

    public boolean existUsername(String username) {
        User user = this.userRepository.checkUsername(username)
                .orElse(null);
        return user != null;
    }

    public static <E> E getIfExists(E newObj, E oldObj) {
        return Objects.nonNull(newObj) ? newObj : oldObj;
    }
}
