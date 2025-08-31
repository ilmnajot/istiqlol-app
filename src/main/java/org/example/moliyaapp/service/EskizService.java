package org.example.moliyaapp.service;

import jakarta.annotation.PostConstruct;
import org.example.moliyaapp.dto.EskizAuthResponse;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class EskizService {
    private final RestTemplate restTemplate = new RestTemplate();
    private static final String TOKEN = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJleHAiOjE3NTMzMzc1MTQsImlhdCI6MTc1MDc0NTUxNCwicm9sZSI6InRlc3QiLCJzaWduIjoiZDQxMWQyZjY2YjJkNDczODVjMGI4ZDZlODEyMDA2YjZiYjVjYzJkMWZkNmMwOTM3ZTcxNjU0NjNlZjJhN2ZkZiIsInN1YiI6IjExNDc5In0.ArHn8EpfaxVezzEJrJwowH_M_fgZ64oDaFFLHTiyZCw";
    private static final String REFRESH_TOKEN = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJleHAiOjE3NTMzMzc3MjYsImlhdCI6MTc1MDc0NTcyNiwicm9sZSI6InRlc3QiLCJzaWduIjoiZDQxMWQyZjY2YjJkNDczODVjMGI4ZDZlODEyMDA2YjZiYjVjYzJkMWZkNmMwOTM3ZTcxNjU0NjNlZjJhN2ZkZiIsInN1YiI6IjExNDc5In0.3lbzOGWrI9l6MB5QhBqWJKG4RwlmBAay-RSg8bwZa_8";

    private static final String SMS_SEND_URL = "https://notify.eskiz.uz/api/message/sms/send";
//    private static final String TOKEN = "Bearer sizning_tokeningiz"; // runtime'da dynamic bo‘lishi mumkin


    public boolean sendSms(String phoneNumber, String message) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", TOKEN);

        Map<String, String> body = Map.of(
                "mobile_phone", phoneNumber,
                "message", message,
                "from", "4546",
                "callback_url", ""
        );

        HttpEntity<?> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(SMS_SEND_URL, request, String.class);
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
