package org.example.moliyaapp.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.moliyaapp.dto.ApiResponse;
import org.example.moliyaapp.dto.MonthToMonthsMapper;
import org.example.moliyaapp.dto.SmsDto;
import org.example.moliyaapp.entity.MonthlyFee;
import org.example.moliyaapp.entity.StudentContract;
import org.example.moliyaapp.enums.Months;
import org.example.moliyaapp.enums.TariffStatus;
import org.example.moliyaapp.repository.MonthlyFeeRepository;
import org.example.moliyaapp.repository.SmsRepository;
import org.example.moliyaapp.repository.StudentContractRepository;
import org.example.moliyaapp.service.SmsService;
import org.example.moliyaapp.utils.RestConstants;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class SmsServiceImpl implements SmsService {

    private final SmsRepository smsRepository;
    private final RestTemplate restTemplate;
    private final Map<String, String> templateCache = new HashMap<>();


    private static final String LOGIN_URL = "https://notify.eskiz.uz/api/auth/login";
    private final StudentContractRepository studentContractRepository;
    private final MonthlyFeeRepository monthlyFeeRepository;

    @Value("${eskiz.api.token}")
    private String token;

    @Value("${eskiz.api.url}")
    private String apiUrl;

    @Value("${eskiz.login}")
    private String eskizLogin;

    @Value("${eskiz.password}")
    private String eskizPassword;


    // SMS xarajat hisoblash usullari

// SMS Cost hisoblashni to'g'rilash - API response'ni tekshirish bilan

    public ApiResponse calculateSMSCost(SmsDto.SmsHistoryDto dto) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.set("Authorization", this.getTokenByEmailAndPassword());

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        if (dto.getStartDate() != null && !dto.getStartDate().isEmpty()) {
            body.add("start_date", dto.getStartDate() + " 00:00");
        }
        if (dto.getEndDate() != null && !dto.getEndDate().isEmpty()) {
            body.add("end_date", dto.getEndDate() + " 23:59");
        }
        body.add("page_size", "1000"); // Ko'proq ma'lumot olish uchun

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
        String url = "https://notify.eskiz.uz/api/message/sms/get-user-messages";

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    request,
                    Map.class
            );

            Map<String, Object> bodyMap = response.getBody();
            if (bodyMap != null && bodyMap.containsKey("data")) {
                Map<String, Object> data = (Map<String, Object>) bodyMap.get("data");
                List<Map<String, Object>> messages = (List<Map<String, Object>>) data.get("result");

                // BIRINCHI MESAJ'NI TO'LIQ TEKSHIRISH
                if (!messages.isEmpty()) {
                    System.out.println("=== FIRST MESSAGE ANALYSIS ===");
                    Map<String, Object> firstMessage = messages.get(0);
                    System.out.println("All fields in first message: " + firstMessage.keySet());
                    System.out.println("Full first message: " + firstMessage);
                    System.out.println("================================");
                }

                // Xarajat hisoblash
                double totalCost = 0.0;
                int deliveredCount = 0;
                int rejectedCount = 0;
                int pendingCount = 0;

                Map<String, Integer> statusCounts = new HashMap<>();
                Map<String, Double> costByStatus = new HashMap<>();

                for (Map<String, Object> message : messages) {
                    String status = (String) message.get("status");

                    // TURLI COST FIELD'LARNI SINASH
                    double cost = 0.0;
                    Object costObj = null;

                    // Mumkin bo'lgan cost field name'lari
                    String[] costFields = {"cost", "price", "amount", "fee", "charge", "sms_cost", "total_cost"};

                    for (String field : costFields) {
                        if (message.containsKey(field)) {
                            costObj = message.get(field);
                            System.out.println("Found cost field '" + field + "': " + costObj);
                            break;
                        }
                    }

                    if (costObj != null) {
                        if (costObj instanceof Number) {
                            cost = ((Number) costObj).doubleValue();
                        } else if (costObj instanceof String && !((String) costObj).isEmpty()) {
                            try {
                                cost = Double.parseDouble((String) costObj);
                            } catch (NumberFormatException e) {
                                cost = 0.0;
                            }
                        }
                    }

                    // AGAR COST 0 BO'LSA, DEFAULT PRICE QOYISH
                    if (cost == 0.0) {
                        // Uzbekiston uchun SMS narxi (taxminan)
                        if ("DELIVERED".equalsIgnoreCase(status)) {
                            cost = 95.0; // 11 so'm taxminan
                        } else if ("ACCEPTED".equalsIgnoreCase(status)) {
                            cost = 95.0; // Accepted ham to'lanadigan
                        } else if ("REJECTED".equalsIgnoreCase(status)) {
                            cost = 0.0; // Rejected SMS uchun to'lov yo'q
                        }
                        System.out.println("Using default cost for status " + status + ": " + cost);
                    }

                    totalCost += cost;

                    // Status bo'yicha hisoblash
                    statusCounts.put(status, statusCounts.getOrDefault(status, 0) + 1);
                    costByStatus.put(status, costByStatus.getOrDefault(status, 0.0) + cost);

                    // Umumiy statistika
                    switch (status.toUpperCase()) {
                        case "DELIVERED":
                            deliveredCount++;
                            break;
                        case "REJECTED":
                            rejectedCount++;
                            break;
                        default:
                            pendingCount++;
                            break;
                    }
                }

                // REAL BALANCE ORQALI TEKSHIRISH
                Double currentBalance = getCurrentBalanceValue();
                String balanceNote = currentBalance != null ?
                        "Current balance: " + currentBalance + " UZS" : "Balance not available";

                // Natija yasash
                Map<String, Object> result = new HashMap<>();
                result.put("period", dto.getStartDate() + " - " + dto.getEndDate());
                result.put("totalCost", Math.round(totalCost * 100.0) / 100.0);
                result.put("totalMessages", messages.size());
                result.put("deliveredMessages", deliveredCount);
                result.put("rejectedMessages", rejectedCount);
                result.put("pendingMessages", pendingCount);
                result.put("costByStatus", costByStatus);
                result.put("messagesByStatus", statusCounts);
                result.put("averageCostPerMessage", messages.size() > 0 ?
                        Math.round((totalCost / messages.size()) * 100.0) / 100.0 : 0.0);
                result.put("balanceInfo", balanceNote);
                result.put("costCalculationMethod", totalCost > 0 ? "API_COST_FIELD" : "DEFAULT_PRICING");

                return ApiResponse.builder()
                        .status(HttpStatus.OK)
                        .message("SMS cost calculated successfully")
                        .data(result)
                        .build();
            }

        } catch (Exception e) {
            System.err.println("Cost calculation error: " + e.getMessage());
            return ApiResponse.builder()
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .message("Failed to calculate SMS cost: " + e.getMessage())
                    .build();
        }

        return ApiResponse.builder()
                .status(HttpStatus.BAD_REQUEST)
                .message("No SMS data found for the specified period")
                .build();
    }

    @Override
    public ApiResponse calculateCostByBalance(int year) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.set("Authorization", this.getTokenByEmailAndPassword());

        String url = "https://notify.eskiz.uz/api/report/total-by-month?year=" + year;

        HttpEntity<?> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<Object> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    Object.class
            );

            return ApiResponse.builder()
                    .status(HttpStatus.OK)
                    .message("✅ Ma'lumotlar olindi")
                    .data(response.getBody())
                    .build();

        } catch (HttpClientErrorException e) {
            return ApiResponse.builder()
                    .status(HttpStatus.BAD_REQUEST)
                    .message("❌ Eskiz xatosi:")
                    .data(e.getResponseBodyAsString())
                    .build();
        } catch (Exception e) {
            return ApiResponse.builder()
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .message("❌ Noma'lum xatolik:")
                    .data(e.getMessage())
                    .build();
        }
    }

    // Helper method - faqat balance qiymatini olish
    private Double getCurrentBalanceValue() {
        try {
            ApiResponse balanceResponse = getLimit();
            if (balanceResponse.getStatus() == HttpStatus.OK && balanceResponse.getData() != null) {
                Map<String, Object> data = (Map<String, Object>) balanceResponse.getData();
                return (Double) data.get("currentBalance");
            }
        } catch (Exception e) {
            System.err.println("Failed to get current balance: " + e.getMessage());
        }
        return null;
    }

    @Override
    public ApiResponse getTemplates() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", this.getTokenByEmailAndPassword());

        HttpEntity<Void> request = new HttpEntity<>(headers);

        String url = "https://notify.eskiz.uz/api/user/templates";

        try {
            // GET so‘rov
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    request,
                    String.class
            );

            return ApiResponse.builder()
                    .status(HttpStatus.OK)
                    .message("✅ Template-lar olindi")
                    .data(response.getBody())
                    .build();

        } catch (HttpClientErrorException e) {
            return ApiResponse.builder()
                    .status(HttpStatus.BAD_REQUEST)
                    .message("❌ Eskiz xatosi:")
                    .data(e.getResponseBodyAsString())
                    .build();
        }
    }


    // 1. Get User Messages - barcha SMS history
    public ApiResponse getSMSHistory(SmsDto.SmsHistoryDto dto) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.set("Authorization", this.getTokenByEmailAndPassword());

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();

        // To'g'ri parametr nomlari (GitHub documentation'dan)
        if (dto.getStartDate() != null && !dto.getStartDate().isEmpty()) {
            body.add("start_date", dto.getStartDate() + " 00:00"); // "2024-07-01 00:00" format
        }
        if (dto.getEndDate() != null && !dto.getEndDate().isEmpty()) {
            body.add("end_date", dto.getEndDate() + " 23:59");     // "2024-07-30 23:59" format
        }
        if (dto.getLimit() != null && dto.getLimit() > 0) {
            body.add("page_size", String.valueOf(dto.getLimit())); // limit emas, page_size
        }

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        // To'g'ri endpoint
        String url = "https://notify.eskiz.uz/api/message/sms/get-user-messages";

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    request,
                    Map.class
            );

            System.out.println("SMS History response: " + response.getBody());
            Map<String, Object> bodyMap = response.getBody();

            if (bodyMap != null && bodyMap.containsKey("data")) {
                Map<String, Object> data = (Map<String, Object>) bodyMap.get("data");

                // Response structure: data.result array, data.total count
                List<Map<String, Object>> messages = (List<Map<String, Object>>) data.get("result");
                Integer total = (Integer) data.get("total");

                Map<String, Object> responseData = new HashMap<>();
                responseData.put("messages", messages);
                responseData.put("total", total);

                return ApiResponse.builder()
                        .status(HttpStatus.OK)
                        .message(RestConstants.SUCCESS)
                        .data(responseData)
                        .build();
            } else {
                return ApiResponse.builder()
                        .status(HttpStatus.BAD_REQUEST)
                        .message("No SMS history found.")
                        .build();
            }
        } catch (HttpClientErrorException e) {
            System.err.println("SMS History error: " + e.getResponseBodyAsString());
            return ApiResponse.builder()
                    .status(HttpStatus.BAD_REQUEST)
                    .message("Failed to get SMS history: " + e.getResponseBodyAsString())
                    .build();
        } catch (Exception e) {
            System.err.println("Unexpected error: " + e.getMessage());
            return ApiResponse.builder()
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .message("Unexpected error: " + e.getMessage())
                    .build();
        }
    }


    // 4. Export Messages (CSV format) - to'g'ri endpoint bilan
    public ApiResponse exportMessages(int year, int month) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.set("Authorization", this.getTokenByEmailAndPassword());

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("year", String.valueOf(year));
        body.add("month", String.valueOf(month));

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        // Turli endpoint variantlarini sinash
        String[] possibleUrls = {
                "https://notify.eskiz.uz/api/user/messages/export",          // 1
                "https://notify.eskiz.uz/api/message/export",               // 2
                "https://notify.eskiz.uz/api/export/messages",              // 3
                "https://notify.eskiz.uz/api/user/export",                  // 4
                "https://notify.eskiz.uz/api/message/sms/export-messages"   // 5
        };

        for (int i = 0; i < possibleUrls.length; i++) {
            String url = possibleUrls[i];
            System.out.println("Trying export URL " + (i + 1) + ": " + url);

            try {
                ResponseEntity<String> response = restTemplate.exchange(
                        url,
                        HttpMethod.POST,
                        request,
                        String.class
                );

                System.out.println("SUCCESS with export URL " + (i + 1) + ": " + response.getStatusCode());
                return ApiResponse.builder()
                        .status(HttpStatus.OK)
                        .message("CSV data exported successfully from: " + url)
                        .data(response.getBody())
                        .build();

            } catch (HttpClientErrorException e) {
                System.err.println("Export URL " + (i + 1) + " failed: " + e.getStatusCode());
                continue;
            } catch (Exception e) {
                System.err.println("Export URL " + (i + 1) + " error: " + e.getMessage());
                continue;
            }
        }

        // Agar hech qaysi URL ishlamasa
        return ApiResponse.builder()
                .status(HttpStatus.NOT_FOUND)
                .message("Export endpoint not found. Tried " + possibleUrls.length + " different URLs.")
                .build();
    }

    @Override
    public String getTokenByEmailAndPassword() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED); // ⚠️ To‘g‘ri content-type

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("email", eskizLogin);
        body.add("password", eskizPassword);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    LOGIN_URL,
                    HttpMethod.POST,
                    request,
                    Map.class
            );

            System.out.println("Eskiz response: " + response.getBody());

            Map bodyMap = response.getBody();
            if (bodyMap != null && bodyMap.containsKey("data")) {
                Map<String, Object> data = (Map<String, Object>) bodyMap.get("data");
                return "Bearer " + data.get("token");
            }

            throw new RuntimeException("No token in response");

        } catch (HttpClientErrorException e) {
            System.err.println("Eskiz error: " + e.getResponseBodyAsString());
            throw new RuntimeException("Eskiz login failed", e);
        }
    }

    @Override
    public ApiResponse getSMSReport(SmsDto.SmsReportDto dto) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.set("Authorization", this.getTokenByEmailAndPassword());

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("year", String.valueOf(dto.getYear()));
        body.add("month", String.valueOf(dto.getMonth()));

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
        String url = "https://notify.eskiz.uz/api/user/totals";

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    request,
                    Map.class
            );

            System.out.println("Eskiz response: " + response.getBody());
            Map<String, Object> bodyMap = response.getBody();

            if (bodyMap != null && bodyMap.containsKey("data")) {
                Object dataObject = bodyMap.get("data");

                // Check if data is directly a List (array)
                if (dataObject instanceof List) {
                    List<Map<String, Object>> smsList = (List<Map<String, Object>>) dataObject;

                    if (!smsList.isEmpty()) {
                        return ApiResponse.builder()
                                .status(HttpStatus.OK)
                                .message(RestConstants.SUCCESS)
                                .data(smsList)
                                .build();
                    } else {
                        return ApiResponse.builder()
                                .status(HttpStatus.BAD_REQUEST)
                                .message("No SMS records found for the specified month and year.")
                                .build();
                    }
                }
                // If data is a Map, try to extract sms_list
                else if (dataObject instanceof Map) {
                    Map<String, Object> dataMap = (Map<String, Object>) dataObject;
                    List<Map<String, Object>> smsList = (List<Map<String, Object>>) dataMap.get("sms_list");

                    if (smsList != null && !smsList.isEmpty()) {
                        return ApiResponse.builder()
                                .status(HttpStatus.OK)
                                .message(RestConstants.SUCCESS)
                                .data(smsList)
                                .build();
                    } else {
                        return ApiResponse.builder()
                                .status(HttpStatus.BAD_REQUEST)
                                .message("No SMS records found for the specified month and year.")
                                .build();
                    }
                } else {
                    return ApiResponse.builder()
                            .status(HttpStatus.BAD_REQUEST)
                            .message("Unexpected data format from Eskiz API.")
                            .build();
                }
            } else {
                return ApiResponse.builder()
                        .status(HttpStatus.BAD_REQUEST)
                        .message("No data received from Eskiz API.")
                        .build();
            }
        } catch (HttpClientErrorException e) {
            System.err.println("Eskiz error: " + e.getResponseBodyAsString());
            return ApiResponse.builder()
                    .status(HttpStatus.BAD_REQUEST)
                    .message("Eskiz login failed: " + e.getResponseBodyAsString())
                    .build();
        } catch (ClassCastException e) {
            System.err.println("Data type casting error: " + e.getMessage());
            return ApiResponse.builder()
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .message("Data format error from Eskiz API")
                    .build();
        } catch (Exception e) {
            System.err.println("Unexpected error: " + e.getMessage());
            return ApiResponse.builder()
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .message("Unexpected error: " + e.getMessage())
                    .build();
        }
    }

    @Override
    public ApiResponse getLimit() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.set("Authorization", this.getTokenByEmailAndPassword());
        HttpEntity<Void> request = new HttpEntity<>(headers);
        String url = "https://notify.eskiz.uz/api/user/get-limit";
        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    request,
                    String.class);

            return ApiResponse.builder()
                    .status(HttpStatus.OK)
                    .message(RestConstants.SUCCESS)
                    .data(response.getBody())
                    .build();
        } catch (HttpClientErrorException e) {
            return ApiResponse.builder()
                    .status(HttpStatus.BAD_REQUEST)
                    .message("❌ Eskiz xatosi:")
                    .data(e.getResponseBodyAsString())
                    .build();
        }
    }

    public void sendAfterPaymentDone(String phone, String message) {
        if (phone == null || phone.trim().isEmpty()) {
            System.err.println("❌ Telefon raqami bo'sh");
            return;
        }
        if (message == null || message.trim().isEmpty()) {
            System.err.println("❌ SMS matni bo'sh");
            return;
        }


        // Set headers for JSON content
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON); // Changed to JSON
        headers.set("Authorization", this.getTokenByEmailAndPassword()); // Added "Bearer " prefix

        // Create JSON payload instead of form data
        Map<String, String> body = new HashMap<>();
        body.put("mobile_phone", phone);
        body.put("from", "4546");
        body.put("message", message);
        body.put("callback_url", ""); // optional callback URL

        HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);
        String url = "https://notify.eskiz.uz/api/message/sms/send";

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
            if (response.getStatusCode().is2xxSuccessful()) {
                System.out.println("✅ SMS yuborildi: " + response.getBody());

            } else {
                System.err.println("❌ SMS yuborishda xatolik: Status " + response.getStatusCode());
                System.err.println("❌ Response body: " + response.getBody());
            }
        } catch (HttpClientErrorException e) {
            System.err.println("❌ Eskiz xato: " + e.getStatusCode());
            System.err.println("❌ Xabar: " + e.getResponseBodyAsString());
        } catch (Exception e) {
            System.err.println("❌ Kutilmagan xatolik: " + e.getMessage());
        }
    }

//
//    public void sendToSpecificStudents(List<Long> studentContractIds, Months month) {
//        if (studentContractIds == null || studentContractIds.isEmpty()) {
//            System.err.println("❌ Talabalar shartnomalari ro'yxati bo'sh");
//            return;
//        }
//
//        // Define quarters (2.5 months each)
//        Map<Integer, List<Months>> quarters = new HashMap<>();
//        quarters.put(1, Arrays.asList(Months.SENTABR, Months.OKTABR, Months.NOYABR));
//        quarters.put(2, Arrays.asList(Months.NOYABR, Months.DEKABR, Months.YANVAR));
//        quarters.put(3, Arrays.asList(Months.FEVRAL, Months.MART, Months.APREL));
//        quarters.put(4, Arrays.asList(Months.APREL, Months.MAY, Months.IYUN));
//
//        Map<Months, Map<Integer, Double>> monthWeights = new HashMap<>();
//        for (Months m : Months.values()) {
//            monthWeights.put(m, new HashMap<>());
//        }
//        monthWeights.get(Months.SENTABR).put(1, 1.0);
//        monthWeights.get(Months.OKTABR).put(1, 1.0);
//        monthWeights.get(Months.NOYABR).put(1, 0.5);
//        monthWeights.get(Months.NOYABR).put(2, 0.5);
//        monthWeights.get(Months.DEKABR).put(2, 1.0);
//        monthWeights.get(Months.YANVAR).put(2, 1.0);
//        monthWeights.get(Months.FEVRAL).put(3, 1.0);
//        monthWeights.get(Months.MART).put(3, 1.0);
//        monthWeights.get(Months.APREL).put(3, 0.5);
//        monthWeights.get(Months.APREL).put(4, 0.5);
//        monthWeights.get(Months.MAY).put(4, 1.0);
//        monthWeights.get(Months.IYUN).put(4, 1.0);
//
//        for (Long contractId : studentContractIds) {
//            try {
//                Optional<StudentContract> contractOpt = studentContractRepository.findById(contractId);
//                if (contractOpt.isEmpty()) {
//                    System.err.println("⚠️ Shartnoma topilmadi: ID " + contractId);
//                    continue;
//                }
//
//                StudentContract contract = contractOpt.get();
//                TariffStatus tariffStatus = contract.getTariff().getTariffStatus();
//                String phone = contract.getPhone1();
//
//                if (phone == null || phone.trim().isEmpty()) {
//                    System.err.println("⚠️ Telefon raqami mavjud emas: Contract ID " + contractId);
//                    continue;
//                }
//
//                double sum = 0.0;
//                if (month == null) {
//                    // Handle all quarters or yearly/monthly tariffs
//                    if (tariffStatus == TariffStatus.MONTHLY) {
//                        sum = contract.getMonthlyFees().stream()
//                                .mapToDouble(MonthlyFee::getRemainingBalance)
//                                .sum();
//                        if (sum <= 0) {
//                            System.err.println("❌ Oylik tarif uchun qarzdorlik yo'q: Contract ID " + contractId);
//                            continue;
//                        }
//                    } else if (tariffStatus == TariffStatus.YEARLY) {
//                        double totalPaid = contract.getMonthlyFees().stream()
//                                .mapToDouble(MonthlyFee::getAmountPaid)
//                                .sum();
//                        double totalCut = contract.getMonthlyFees().stream()
//                                .mapToDouble(MonthlyFee::getCutAmount)
//                                .sum();
//                        sum = contract.getTariff().getAmount() - totalPaid - totalCut;
//                        if (sum <= 0) {
//                            System.err.println("❌ Yillik tarif uchun qarzdorlik yo'q: Contract ID " + contractId);
//                            continue;
//                        }
//                    } else if (tariffStatus == TariffStatus.QUARTERLY) {
//                        sum = calculateQuarterlyRemainingBalance(contract, quarters, monthWeights);
//                        if (sum <= 0) {
//                            System.err.println("❌ Choraklik tarif uchun qarzdorlik yo'q: Contract ID " + contractId);
//                            continue;
//                        }
//                    }
//
//                    String smsMessage = String.format(
//                            "Hurmatli ota-ona! Siz farzandingiz %s maktab to‘lovidan %.0f so'm qarzdorligingiz mavjud. Qarzdorlikni so‘ndirishingizni so'raymiz. \"IFTIXOR\" xususiy maktabi. Ma'lumot uchun: +998998382302.",
//                            contract.getStudentFullName(), sum);
//                    try {
//                        sendAfterPaymentDone(phone, smsMessage);
//                        System.out.println("✅ SMS yuborildi: " + phone + " (Contract ID: " + contractId + ")");
//                    } catch (Exception e) {
//                        System.err.println("❌ SMS yuborishda xatolik: " + phone + " (Contract ID: " + contractId + ")");
//                    }
//                } else if (tariffStatus == TariffStatus.MONTHLY) {
//                    Optional<MonthlyFee> monthlyFee = monthlyFeeRepository.findByStudentContractIdAndMonths(contractId, month);
//                    sum = monthlyFee.map(MonthlyFee::getRemainingBalance)
//                            .orElse(contract.getTariff().getAmount());
//                    if (sum <= 0) {
//                        System.err.println("❌ Oylik tarif uchun qarzdorlik yo'q: " + month + " (Contract ID: " + contractId + ")");
//                        continue;
//                    }
//
//                    String smsMessage = String.format(
//                            "Hurmatli ota-ona! Siz farzandingiz %s oyi uchun maktab to‘lovidan %.0f so'm qarzdorligingiz mavjud. Qarzdorlikni so‘ndirishingizni so'raymiz. \"IFTIXOR\" xususiy maktabi. Ma'lumot uchun: +998998382302.",
//                            contract.getStudentFullName() + " " + month, sum);
//                    try {
//                        sendAfterPaymentDone(phone, smsMessage);
//                        System.out.println("✅ SMS yuborildi: " + phone + " (Contract ID: " + contractId + ")");
//                    } catch (Exception e) {
//                        System.err.println("❌ SMS yuborishda xatolik: " + phone + " (Contract ID: " + contractId + ")");
//                    }
//                } else if (tariffStatus == TariffStatus.QUARTERLY) {
//                    Set<Integer> targetQuarters = getQuartersForMonth(month, quarters);
//                    if (targetQuarters.isEmpty()) {
//                        System.err.println("⚠️ Oy chorakka mos kelmadi: " + month + " (Contract ID: " + contractId + ")");
//                        continue;
//                    }
//
//                    for (Integer quarter : targetQuarters) {
//                        sum = calculateQuarterlyRemainingBalanceForQuarter(contract, quarters.get(quarter), monthWeights, quarter);
//                        if (sum <= 0) {
//                            System.err.println("❌ Choraklik tarif uchun qarzdorlik yo'q: Q" + quarter + " (Contract ID: " + contractId + ")");
//                            continue;
//                        }
//
//                        String smsMessage = String.format(
//                                "Hurmatli ota-ona! Siz farzandingiz %d-chorak (%s oyi) uchun maktab to‘lovidan %.0f so'm qarzdorligingiz mavjud. Qarzdorlikni so‘ndirishingizni so'raymiz. \"IFTIXOR\" xususiy maktabi. Ma'lumot uchun: +998998382302.",
//                                quarter, month, sum);
//                        try {
//                            sendAfterPaymentDone(phone, smsMessage);
//                            System.out.println("✅ SMS yuborildi: " + phone + " (Contract ID: " + contractId + ")");
//                        } catch (Exception e) {
//                            System.err.println("❌ SMS yuborishda xatolik: " + phone + " (Contract ID: " + contractId + ")");
//                        }
//                    }
//                }
//            } catch (Exception e) {
//                System.err.println("❌ Kutilmagan xatolik Contract ID " + contractId + ": " + e.getMessage());
//            }
//        }
//    }
//
//    private double calculateQuarterlyRemainingBalance(StudentContract contract, Map<Integer, List<Months>> quarters, Map<Months, Map<Integer, Double>> monthWeights) {
//        double totalRemaining = 0.0;
//        for (Integer quarter : quarters.keySet()) {
//            totalRemaining += calculateQuarterlyRemainingBalanceForQuarter(contract, quarters.get(quarter), monthWeights, quarter);
//        }
//        return totalRemaining > 0 ? totalRemaining : 0;
//    }
//
//    private double calculateQuarterlyRemainingBalanceForQuarter(StudentContract contract, List<Months> quarterMonths, Map<Months, Map<Integer, Double>> monthWeights, Integer quarter) {
//        double quarterAmount = contract.getTariff().getAmount(); // Amount for one 2.5-month quarter (6,250,000)
//        double monthlyAmount = quarterAmount / 2.5; // Amount per full month (2,500,000)
//        double expectedAmount = 0.0; // Total expected amount for this quarter
//        double paid = 0.0;
//        double cut = 0.0;
//
//        for (Months month : quarterMonths) {
//            // Get the weight of this month for the specific quarter
//            double weight = monthWeights.get(month).getOrDefault(quarter, 0.0);
//
//            // Check for MonthlyFee
//            Optional<MonthlyFee> fee = monthlyFeeRepository.findByStudentContractIdAndMonths(contract.getId(), month);
//            if (fee.isPresent()) {
//                // Use total_fee from MonthlyFee as the expected amount for this month
//                expectedAmount += fee.get().getTotalFee() * weight;
//                paid += fee.get().getAmountPaid() * weight;
//                cut += fee.get().getCutAmount() * weight;
//            } else {
//                // Use tariff-based monthly amount for months without MonthlyFee
//                expectedAmount += monthlyAmount * weight;
//            }
//        }
//
//        double remaining = expectedAmount - paid - cut;
//        return remaining > 0 ? remaining : 0;
//    }
//
//    private Set<Integer> getQuartersForMonth(Months month, Map<Integer, List<Months>> quarters) {
//        Set<Integer> targetQuarters = new HashSet<>();
//        for (Map.Entry<Integer, List<Months>> entry : quarters.entrySet()) {
//            if (entry.getValue().contains(month)) {
//                targetQuarters.add(entry.getKey());
//            }
//        }
//        return targetQuarters;
//    }

    public void sendToSpecificStudents(List<Long> studentContractIds, Months month) {
        if (studentContractIds == null || studentContractIds.isEmpty()) {
            System.err.println("❌ Talabalar shartnomalari ro'yxati bo'sh");
            return;
        }

        for (Long contractId : studentContractIds) {
            try {
                Optional<StudentContract> contractOpt = studentContractRepository.findById(contractId);
                if (contractOpt.isEmpty()) {
                    System.err.println("⚠️ Shartnoma topilmadi: ID " + contractId);
                    continue;
                }

                StudentContract contract = contractOpt.get();
                TariffStatus tariffStatus = contract.getTariff().getTariffStatus();
                String phone = contract.getPhone1();

                if (phone == null || phone.trim().isEmpty()) {
                    System.err.println("⚠️ Telefon raqami mavjud emas: Contract ID " + contractId);
                    continue;
                }

                if (month == null) {
                    handleGeneralDebtMessage(contract, contractId, tariffStatus);
                } else {
                    switch (tariffStatus) {
                        case MONTHLY:
                            handleMonthlyTariff(contract, contractId, month);
                            break;
                        case QUARTERLY:
                            handleQuarterlyTariff(contract, contractId, month);
                            break;
                        case YEARLY:
                            // For yearly, you might want to handle this case as well
                            handleGeneralDebtMessage(contract, contractId, tariffStatus);
                            break;
                    }
                }
            } catch (Exception e) {
                System.err.println("❌ Kutilmagan xatolik Contract ID " + contractId + ": " + e.getMessage());
            }
        }
    }

    private void handleGeneralDebtMessage(StudentContract contract, Long contractId, TariffStatus tariffStatus) {
        double sum = contract.getMonthlyFees().stream()
                .mapToDouble(MonthlyFee::getRemainingBalance).sum();

        if (tariffStatus == TariffStatus.MONTHLY) {
            if (sum == 0) {
                System.err.println("❌ Oylik tarif uchun oy belgilanmagan: Contract ID " + contractId);
                return;
            }
        } else if (tariffStatus == TariffStatus.YEARLY) {
            if (sum == 0 && contract.getMonthlyFees().isEmpty()) {
                sum = contract.getTariff().getAmount();
            } else if (sum != 0) {
                sum = contract.getTariff().getAmount()
                        - contract.getMonthlyFees().stream().mapToDouble(MonthlyFee::getAmountPaid).sum()
                        - contract.getMonthlyFees().stream().mapToDouble(MonthlyFee::getCutAmount).sum();
            } else {
                return;
            }
        }

        try {
            this.sendSms(contract, sum, null); // null for general debt
        } catch (Exception e) {
            System.err.println("❌ SMS yuborishda xatolik: " + contract.getPhone1() + " (Contract ID: " + contractId + ")");
        }
    }

    private void handleMonthlyTariff(StudentContract contract, Long contractId, Months month) {
        MonthlyFee monthlyFee = monthlyFeeRepository.findByStudentContractIdAndMonthsAndAcademicYear(contractId, month, contract.getAcademicYear())
                .orElse(null);
        Double remainingBalance = monthlyFee != null ? monthlyFee.getRemainingBalance() :
                contract.getTariff().getAmount();

        String smsMessage = String.format(
                "Hurmatli ota-ona ! Siz farzandingiz %s %s oyi uchun maktab to'lovidan %.0f so'm qarzdorligingiz mavjud. Qarzdorlikni so'ndirishingizni so'raymiz. \"IFTIXOR\" xususiy maktabi. Ma'lumot uchun: +998998382302.",
                contract.getStudentFullName(),
                month.name(),
                remainingBalance);

        try {
            this.sendAfterPaymentDone(contract.getPhone1(), smsMessage);
            System.out.println("✅ SMS yuborildi: " + contract.getPhone1() + " (Contract ID: " + contractId + ")");
        } catch (Exception e) {
            System.err.println("❌ SMS yuborishda xatolik: " + contract.getPhone1() + " (Contract ID: " + contractId + ")");
        }
    }

    private void handleQuarterlyTariff(StudentContract contract, Long contractId, Months month) {
        QuarterInfo quarterInfo = determineQuarter(month);
        if (quarterInfo == null) {
            System.err.println("❌ Noto'g'ri oy chorak tarifi uchun: " + month);
            return;
        }

        double totalDebt = calculateQuarterlyDebt(contractId, quarterInfo, contract.getTariff().getAmount());

        // Only skip if debt is 0 or less (fully paid)
        if (totalDebt <= 0) {
            System.out.println("ℹ️ " + quarterInfo.quarterName + " uchun qarzdorlik yo'q: Contract ID " + contractId);
            return; // No debt for this quarter
        }

        String smsMessage = String.format(
                "Hurmatli ota-ona ! Siz farzandingiz %s %s uchun maktab to'lovidan %.0f so'm qarzdorligingiz mavjud. Qarzdorlikni so'ndirishingizni so'raymiz. \"IFTIXOR\" xususiy maktabi. Ma'lumot uchun: +998998382302.",
                contract.getStudentFullName(),
                quarterInfo.quarterName,
                totalDebt);

        try {
            this.sendAfterPaymentDone(contract.getPhone1(), smsMessage);
            System.out.println("✅ SMS yuborildi: " + contract.getPhone1() + " (Contract ID: " + contractId + ") - " + quarterInfo.quarterName + " qarzdorligi: " + totalDebt);
        } catch (Exception e) {
            System.err.println("❌ SMS yuborishda xatolik: " + contract.getPhone1() + " (Contract ID: " + contractId + ")");
        }
    }

    private QuarterInfo determineQuarter(Months month) {
        return switch (month) {
            case SENTABR, OKTABR, NOYABR -> new QuarterInfo("1-chorak",
                    Arrays.asList(Months.SENTABR, Months.OKTABR, Months.NOYABR));
            case DEKABR, YANVAR -> new QuarterInfo("2-chorak",
                    Arrays.asList(Months.NOYABR, Months.DEKABR, Months.YANVAR)); // Note: November spans 2 quarters
            case FEVRAL, MART, APREL -> new QuarterInfo("3-chorak",
                    Arrays.asList(Months.FEVRAL, Months.MART, Months.APREL));
            case MAY, IYUN -> new QuarterInfo("4-chorak",
                    Arrays.asList(Months.APREL, Months.MAY, Months.IYUN)); // Note: April spans 2 quarters
            default -> null;
        };
    }

    private double calculateQuarterlyDebt(Long contractId, QuarterInfo quarterInfo, Double contractAmount) {
        // For quarterly tariff, contractAmount IS the quarterly amount (6.25M in your example)
        double quarterlyTotalAmount = contractAmount;
        double totalPaid = 0.0;
        double totalCut = 0.0;

        for (Months quarterMonth : quarterInfo.months) {
            Optional<MonthlyFee> fee = monthlyFeeRepository.findByStudentContractIdAndMonths(contractId, quarterMonth);

            if (fee.isPresent()) {
                MonthlyFee monthlyFee = fee.get();

                // Handle months that span two quarters (November and April)
                if ((quarterMonth == Months.NOYABR && quarterInfo.quarterName.equals("2-chorak")) ||
                        (quarterMonth == Months.APREL && quarterInfo.quarterName.equals("4-chorak"))) {
                    totalPaid += monthlyFee.getAmountPaid() / 2;
                    totalCut += monthlyFee.getCutAmount() / 2;
                } else {
                    totalPaid += monthlyFee.getAmountPaid();
                    totalCut += monthlyFee.getCutAmount();
                }
            }
            // If no fee record exists, no payment or cut has been made for this month
        }

        // Calculate remaining debt: Quarter amount - (paid + cut)
        double remainingDebt = quarterlyTotalAmount - totalPaid - totalCut;

        // Return the remaining debt (should be positive if there's debt, 0 or negative if fully paid)
        return Math.max(0, remainingDebt);
    }

    // Updated sendSms method to handle quarter information
    private void sendSms(StudentContract contract, Double sum, String quarterInfo) {
        String smsMessage;
        if (quarterInfo != null) {
            smsMessage = String.format(
                    "Hurmatli ota-ona ! Siz farzandingiz %s %s uchun maktab to'lovidan %.0f so'm qarzdorligingiz mavjud. Qarzdorlikni so'ndirishingizni so'raymiz. \"IFTIXOR\" xususiy maktabi. Ma'lumot uchun: +998998382302.",
                    contract.getStudentFullName(),
                    quarterInfo,
                    sum);
        } else {
            smsMessage = String.format(
                    "Hurmatli ota-ona ! Siz farzandingiz %s maktab to'lovidan %.0f so'm qarzdorligingiz mavjud. Qarzdorlikni so'ndirishingizni so'raymiz. \"IFTIXOR\" xususiy maktabi. Ma'lumot uchun: +998998382302.",
                    contract.getStudentFullName(),
                    sum);
        }

        try {
            this.sendAfterPaymentDone(contract.getPhone1(), smsMessage);
            System.out.println("✅ SMS yuborildi: " + contract.getPhone1() + " (Contract ID: " + contract.getId() + ")");
        } catch (Exception e) {
            System.err.println("❌ SMS yuborishda xatolik: " + contract.getPhone1() + " (Contract ID: " + contract.getId() + ")");
        }
    }

    // Helper class to store quarter information
    private static class QuarterInfo {
        final String quarterName;
        final List<Months> months;

        QuarterInfo(String quarterName, List<Months> months) {
            this.quarterName = quarterName;
            this.months = months;
        }
    }


    @Scheduled(cron = "0 0 9 5 * *")
    private void notifyUnpaidStudents() {
        Month month = LocalDateTime.now().getMonth();
        Months currentMonth = MonthToMonthsMapper.map(month);
        if (currentMonth == Months.IYUL || currentMonth == Months.AVGUST) {
            System.out.println("❌ Iyun yoki Iyul oyi uchun SMS yuborilmaydi.");
            return; // Iyun yoki Iyul oyi uchun SMS yuborilmaydi
        }
        List<StudentContract> contracts = this.studentContractRepository.findAllByStatus(true);
        for (StudentContract contract : contracts) {
            MonthlyFee monthlyFee = monthlyFeeRepository.findByStudentContractIdAndMonthsAndAcademicYear(contract.getId(), currentMonth, contract.getAcademicYear())
                    .orElse(null);
            Double totalFee = monthlyFee != null ? monthlyFee.getTotalFee() : null;
            if (monthlyFee != null && monthlyFee.getTotalFee() != null) {
                String smsMessage = String.format(
                        "Hurmatli ota-ona ! Siz farzandingiz %s oyi uchun maktab to‘lovidan %.0f qarzdorligingiz mavjud. Qarzdorlikni so‘ndirishingizni so'raymiz. \"IFTIXOR\" xususiy maktabi. Ma'lumot uchun: +998998382302.",
                        month,
                        totalFee);
                try {
                    this.sendAfterPaymentDone(contract.getPhone1(), smsMessage);
                    System.out.println("✅ SMS yuborildi: " + contract.getPhone1() + " (Contract ID: " + contract.getId() + ")");
                } catch (Exception e) {
                    System.err.println("❌ SMS yuborishda xatolik: " + contract.getPhone1() + " (Contract ID: " + contract.getId() + ")");
                }
            }
        }
    }

    @Scheduled(cron = "0 0 9 20 * *")  // Har oy 20-sanasida soat 09:00 da ishga tushadi
    public void notifyPartiallyPaidStudents() {
        Month month = LocalDateTime.now().getMonth();
        Months currentMonth = MonthToMonthsMapper.map(month);
        if (currentMonth == Months.IYUL || currentMonth == Months.AVGUST) {
            System.out.println("❌ Iyun yoki Iyul oyi uchun SMS yuborilmaydi.");
            return; // Iyun yoki Iyul oyi uchun SMS yuborilmaydi
        }
        List<StudentContract> contracts = studentContractRepository.findAllByStatus(true);

        for (StudentContract contract : contracts) {
            MonthlyFee monthlyFee = monthlyFeeRepository.findByStudentContractIdAndMonthsAndAcademicYear(contract.getId(), currentMonth,contract.getAcademicYear() )
                    .orElse(null);
            Double getRemainingBalance = monthlyFee != null ? monthlyFee.getRemainingBalance() : null;
            if (monthlyFee != null && getRemainingBalance != null && getRemainingBalance > 0) {
                String smsMessage = String.format(
                        "Hurmatli ota-ona ! Siz farzandingiz %s oyi uchun maktab to‘lovidan %.0f qarzdorligingiz mavjud. Qarzdorlikni so‘ndirishingizni so'raymiz. \"IFTIXOR\" xususiy maktabi. Ma'lumot uchun: +998998382302.",
                        month,
                        getRemainingBalance);
                try {
                    this.sendAfterPaymentDone(contract.getPhone1(), smsMessage);
                    System.out.println("✅ SMS yuborildi: " + contract.getPhone1() + " (Contract ID: " + contract.getId() + ")");
                } catch (Exception e) {
                    System.err.println("❌ SMS yuborishda xatolik: " + contract.getPhone1() + " (Contract ID: " + contract.getId() + ")");
                }
            }
        }
    }

}