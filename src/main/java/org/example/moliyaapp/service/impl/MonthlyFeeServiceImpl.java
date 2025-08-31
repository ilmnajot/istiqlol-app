package org.example.moliyaapp.service.impl;

import jakarta.persistence.Tuple;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.example.moliyaapp.dto.*;
import org.example.moliyaapp.entity.*;
import org.example.moliyaapp.enums.*;
import org.example.moliyaapp.exception.ResourceNotFoundException;
import org.example.moliyaapp.filter.MonthlyFeeFilter;
import org.example.moliyaapp.filter.MonthlyFeeFilterWithStatus;
import org.example.moliyaapp.filter.TransactionEmployeeFilter;
import org.example.moliyaapp.filter.TransactionSheetFilter;
import org.example.moliyaapp.mapper.*;
import org.example.moliyaapp.repository.*;
import org.example.moliyaapp.service.EmployeeMonthlyFeeGoogleSheet;
import org.example.moliyaapp.service.MonthlyFeeService;
import org.example.moliyaapp.utils.RestConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MonthlyFeeServiceImpl implements MonthlyFeeService {

    private static final Logger log = LoggerFactory.getLogger(MonthlyFeeServiceImpl.class);

    private final MonthlyFeeRepository monthlyFeeRepository;
    private final TransactionRepository transactionRepository;
    private final MonthlyFeeMapper monthlyFeeMapper;
    private final UserRepository userRepository;
    private final StudentContractRepository studentContractRepository;
    private final CompanyRepository companyRepository;
    private final StudentContractMapper studentContractMapper;
    private final CategoryRepository categoryRepository;
    private final TeacherTableRepository teacherTableRepository;
    private final TransactionMapper transactionMapper;
    private final ImageRepository imageRepository;
    private final EmployeeMonthlyFeeGoogleSheet employeeMonthlyFeeGoogleSheet;


    @Value("${app.base-url}")
    private String baseURL;

    @Override
    @Transactional
    public ApiResponse createStudent(MonthlyFeeDto.CreateMonthlyFee dto) {
        try {
            // Validate input
            if (dto.getAmountPaid() == null || dto.getAmountPaid() <= 0) {
                return ApiResponse.builder()
                        .status(HttpStatus.BAD_REQUEST)
                        .message(RestConstants.INVALID_AMOUNT)
                        .build();
            }

            // Fetch required entities
            StudentContract studentContract = studentContractRepository.findById(dto.getStudentContractId())
                    .orElseThrow(() -> new ResourceNotFoundException(RestConstants.STUDENT_NOT_FOUND));

            Company company = companyRepository.findOne()
                    .orElseThrow(() -> new ResourceNotFoundException(RestConstants.COMPANY_NOT_FOUND));

            Category category = categoryRepository.findByName(dto.getCategoryName())
                    .orElseThrow(() -> new ResourceNotFoundException(RestConstants.CATEGORY_NOT_FOUND));

            // Check for existing monthly fee
            MonthlyFee existingFee = monthlyFeeRepository.findByContractIdAndMonthNameLast(
                            dto.getStudentContractId(),
                            dto.getMonths().name())
                    .orElse(null);

            // Handle balances (with null safety)
            Double cashBalance = company.getCashBalance() != null ? company.getCashBalance() : 0.0;
            Double cardBalance = company.getCardBalance() != null ? company.getCardBalance() : 0.0;

            // Update company balance based on payment type
            if (dto.getPaymentType() == PaymentType.NAQD) {
                cashBalance += dto.getAmountPaid();
                company.setCashBalance(cashBalance);
            } else if (dto.getPaymentType() == PaymentType.BANK) {
                cardBalance += dto.getAmountPaid();
                company.setCardBalance(cardBalance);
            }
            company = companyRepository.save(company);

            // Create or update monthly fee
            MonthlyFee fee;
            if (existingFee != null) {
                // Update existing fee
                fee = monthlyFeeMapper.toEntity(dto);
                fee.setId(existingFee.getId());
                fee.setAmountPaid(existingFee.getAmountPaid() + dto.getAmountPaid());
                fee.setRemainingBalance(Math.max(existingFee.getTotalFee() - fee.getAmountPaid(), 0.0));
            } else {
                // Create new fee
                fee = monthlyFeeMapper.toEntity(dto);
                fee.setTotalFee(studentContract.getAmount());
                fee.setAmountPaid(dto.getAmountPaid());
                fee.setRemainingBalance(Math.max(studentContract.getAmount() - dto.getAmountPaid(), 0.0));
                fee.setCutAmount(0.0);
            }

            // Set common properties
            fee.setStudentContract(studentContract);
            fee.setCompany(company);
            fee.setCategory(category);
            fee.setStatus(calculatePaymentStatus(fee));

            // Save monthly fee
            fee = monthlyFeeRepository.save(fee);

            // Create transaction
            Transaction transaction = new Transaction();
            transaction.setTransactionType(TransactionType.INCOME);
            transaction.setPaymentType(dto.getPaymentType());
            transaction.setAmount(dto.getAmountPaid());
            transaction.setDescription(dto.getDescription());
            transaction.setMonthlyFee(fee);
            transaction.setCompany(company);
            transactionRepository.save(transaction);

            return ApiResponse.builder()
                    .status(HttpStatus.OK)
                    .message(RestConstants.SUCCESS)
                    .data(studentContractMapper.toStudentContractDtoResponse(studentContract))
                    .build();

        } catch (Exception e) {
//            log.error("Error creating student monthly fee: {}", e.getMessage(), e);
            return ApiResponse.builder()
                    .status(HttpStatus.BAD_REQUEST)
                    .message("XATOLIK")
                    .build();
        }
    }

    @Transactional
    @Override
    public ApiResponse createEmployee(MonthlyFeeDto.CreateMonthlyFee dto) {
        try {
            // Validate input
            if (isInvalidAmount(dto.getAmountPaid()) || isInvalidAmount(dto.getBonus())) {
                return ApiResponse.builder()
                        .status(HttpStatus.BAD_REQUEST)
                        .message(RestConstants.INVALID_AMOUNT)
                        .build();
            }

            // Fetch required entities
            User employee = userRepository.findById(dto.getEmployeeId())
                    .orElseThrow(() -> new ResourceNotFoundException(RestConstants.USER_NOT_FOUND));

            Company company = companyRepository.findOne()
                    .orElseThrow(() -> new ResourceNotFoundException(RestConstants.COMPANY_NOT_FOUND));

            Category category = categoryRepository.findByName(dto.getCategoryName())
                    .orElseThrow(() -> new ResourceNotFoundException(RestConstants.CATEGORY_NOT_FOUND));

            // Get teacher table info
            int year = LocalDateTime.now().getYear();
            TeacherTable teacherTable = teacherTableRepository.findByTeacherIdAndMonthAndYear(
                            employee.getId(),
                            dto.getMonths(),
                            year)
                    .orElseThrow(() -> new ResourceNotFoundException(RestConstants.TABEL_NOT_FOUND));

            // Check existing monthly fee
            MonthlyFee existingFee = monthlyFeeRepository.findByEmployeeIdAndMonthNameLast(
                    dto.getEmployeeId(),
                    dto.getMonths().name()
            ).orElse(null);

            // Calculate payment amounts
            double bonus = dto.getBonus() != null ? dto.getBonus() : 0.0;
            double amountPaid = dto.getAmountPaid() != null ? dto.getAmountPaid() : 0.0;
            double totalPayment = amountPaid + bonus;
            double baseSalary = teacherTable.getAmount();
            double totalFee = baseSalary + bonus;

            // Check company balances (with null safety)
            double cashBalance = company.getCashBalance() != null ? company.getCashBalance() : 0.0;
            double cardBalance = company.getCardBalance() != null ? company.getCardBalance() : 0.0;

            // Validate sufficient balance based on payment type
            if (dto.getPaymentType() == PaymentType.NAQD) {
                if (cashBalance < totalPayment) {
                    return ApiResponse.builder()
                            .status(HttpStatus.BAD_REQUEST)
                            .message(RestConstants.MONEY_IS_NOT_ENOUGH)
                            .build();
                }
            } else if (dto.getPaymentType() == PaymentType.BANK) {
                if (cardBalance < totalPayment) {
                    return ApiResponse.builder()
                            .status(HttpStatus.BAD_REQUEST)
                            .message(RestConstants.MONEY_IS_NOT_ENOUGH)
                            .build();
                }
            }
            // Update company balance
            if (dto.getPaymentType() == PaymentType.NAQD) {
                company.setCashBalance(cashBalance - totalPayment);
            } else if (dto.getPaymentType() == PaymentType.BANK) {
                company.setCardBalance(cardBalance - totalPayment);
            }
            company = companyRepository.save(company);

            // Create or update monthly fee
            MonthlyFee fee;
            if (existingFee != null) {
                // Update existing fee
                fee = existingFee;
                fee.setAmountPaid(fee.getAmountPaid() + amountPaid);
                fee.setBonus(fee.getBonus() + bonus);
                fee.setRemainingBalance(Math.max(totalFee - fee.getAmountPaid(), 0.0));
            } else {
                // Create new fee
                fee = monthlyFeeMapper.toEntity(dto);
                fee.setTotalFee(totalFee);
                fee.setAmountPaid(amountPaid);
                fee.setBonus(bonus);
                fee.setRemainingBalance(Math.max(totalFee - amountPaid, 0.0));
            }


            // Set payment status
            fee.setStatus(calculatePaymentStatus(fee));
            fee.setEmployee(employee);
            fee.setCompany(company);
            fee.setCategory(category);

            MonthlyFee savedFee = monthlyFeeRepository.save(fee);
            try {
                employeeMonthlyFeeGoogleSheet.initializeSheet();
                this.employeeMonthlyFeeGoogleSheet.recordEmployeeMonthlyFee(savedFee);
            } catch (Exception e) {
                log.error(e.getMessage());
            }
            // Create transaction
            saveTransaction(dto, savedFee, company, totalPayment);

            return ApiResponse.builder()
                    .status(HttpStatus.OK)
                    .message(RestConstants.SUCCESS)
                    .data(monthlyFeeMapper.toDto(savedFee))
                    .build();

        } catch (Exception e) {
//            log.error("Error creating employee monthly fee: {}", e.getMessage(), e);
            return ApiResponse.builder()
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .message("XATOLIK")
                    .build();
        }
    }

    private boolean isInvalidAmount(Double amount) {
        return amount == null || amount < 0;
    }

    private PaymentStatus calculatePaymentStatus(MonthlyFee fee) {
        if (fee.getRemainingBalance() <= 0) {
            return PaymentStatus.FULLY_PAID;
        } else if (fee.getAmountPaid() <= 0) {
            return PaymentStatus.UNPAID;
        } else {
            return PaymentStatus.PARTIALLY_PAID;
        }
    }

    private void saveTransaction(MonthlyFeeDto.CreateMonthlyFee dto, MonthlyFee fee, Company company, double amount) {
        Transaction transaction = new Transaction();
        transaction.setTransactionType(TransactionType.OUTCOME);
        transaction.setPaymentType(dto.getPaymentType());
        transaction.setDescription(dto.getDescription());
        transaction.setAmount(amount);
        transaction.setMonthlyFee(fee);
        transaction.setCompany(company);
        transactionRepository.save(transaction);
    }


    @Override
    public ApiResponse getById(Long id) {
        MonthlyFee monthlyFee = this.monthlyFeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(RestConstants.MONTHLY_FEE_NOT_FOUND));
        Double balanceForMonth = this.calculateRemainingBalanceForMonth(id, monthlyFee.getMonths());
        MonthlyFeeDto dto = this.monthlyFeeMapper.toDto(monthlyFee);
        dto.setRemainingBalance(balanceForMonth);
        return ApiResponse.builder()
                .status(HttpStatus.OK)
                .data(dto)
                .build();
    }

    private Double calculateRemainingBalanceForMonth(Long studentContractId, Months month) {
        StudentContract studentContract = this.studentContractRepository.findById(studentContractId).orElse(null);
        Optional<MonthlyFee> monthlyFee = this.monthlyFeeRepository.findByStudentContractIdAndMonthsAndAcademicYear(studentContractId, month, studentContract.getAcademicYear());
        return monthlyFee.map(MonthlyFee::getRemainingBalance).orElse(studentContract != null ? studentContract.getAmount() : 0.0);
    }


    @Override
    public ApiResponse getAll(MonthlyFeeFilter filter, Pageable pageable) {
        Page<MonthlyFee> page = this.monthlyFeeRepository.findAll(filter, pageable);
        if (!page.isEmpty()) {
            return ApiResponse.builder()
                    .status(HttpStatus.OK)
                    .message(RestConstants.SUCCESS)
                    .data(this.monthlyFeeMapper.dtoList(page.getContent()))
                    .pages(page.getTotalPages())
                    .elements(page.getTotalElements())
                    .build();
        }
        return ApiResponse.builder()
                .status(HttpStatus.OK)
                .data(new ArrayList<>())
                .build();
    }

    @Override
    public ApiResponse delete(Long id) {
        MonthlyFee monthlyFee = this.monthlyFeeRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException(RestConstants.MONTHLY_FEE_NOT_FOUND));
        monthlyFee.setDeleted(true);
        this.monthlyFeeRepository.save(monthlyFee);
        return ApiResponse.builder()
                .status(HttpStatus.OK)
                .message(RestConstants.SUCCESS)
                .build();
    }

    @Override
    public ApiResponse getAllByStatus(PaymentStatus status, Pageable pageable) {
        Page<MonthlyFee> page = this.monthlyFeeRepository.findAllByStatus(status.name(), pageable);
        if (page != null && !page.isEmpty()) {
            return ApiResponse.builder()
                    .status(HttpStatus.OK)
                    .pages(page.getTotalPages())
                    .elements(page.getTotalElements())
                    .data(this.monthlyFeeMapper.dtoList(page.getContent()))
                    .build();
        }
        return ApiResponse.builder()
                .status(HttpStatus.OK)
                .data(new ArrayList<>())
                .build();
    }

    @Override
    public ApiResponse getAllByMonths(Months months, Pageable pageable) {
        Page<MonthlyFee> page = this.monthlyFeeRepository.findAllByMonths(months.name(), pageable);
        if (page != null && !page.isEmpty()) {
            return ApiResponse.builder()
                    .status(HttpStatus.OK)
                    .pages(page.getTotalPages())
                    .elements(page.getTotalElements())
                    .data(this.monthlyFeeMapper.dtoList(page.getContent()))
                    .build();
        }
        return ApiResponse.builder()
                .status(HttpStatus.OK)
                .data(new ArrayList<>())
                .build();
    }

    @Override
    public ApiResponse countByStatus() {
        Map<String, Long> map = new HashMap<>();
        List<Tuple> tuples = this.monthlyFeeRepository.countByStatus();
        if (tuples != null && !tuples.isEmpty()) {
            for (Tuple t : tuples) {
                map.put(t.get(0, String.class), t.get(1, Long.class));
            }
        }
        return ApiResponse.builder()
                .status(HttpStatus.OK)
                .data(map)
                .build();
    }

    @Override
    public ApiResponse getAllAmountByTypeADay(TransactionType transactionType, LocalDate date) {
        BigDecimal allIncomeADay = this.transactionRepository.getAllAmountByTypeADay(transactionType.name(), date);
        if (allIncomeADay == null) allIncomeADay = BigDecimal.ZERO;
        return ApiResponse.builder()
                .status(HttpStatus.OK)
                .data(allIncomeADay)
                .build();
    }

    @Override
    public ApiResponse getAllAmountByTypeAWeek(TransactionType type, LocalDate start, LocalDate end) {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().atTime(LocalTime.MAX);
        BigDecimal allIncomeAWeek = this.transactionRepository.getAllAmountByTypeAWeek(type.name(), startOfDay, endOfDay);
        if (allIncomeAWeek == null) allIncomeAWeek = BigDecimal.ZERO;
        return ApiResponse.builder()
                .status(HttpStatus.OK)
                .data(allIncomeAWeek)
                .build();
    }

    @Override
    public ApiResponse getAllAmountByTypeAMonths(TransactionType type, LocalDate start, LocalDate end) {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().atTime(LocalTime.MAX);
        BigDecimal allIncomeAMonths = this.transactionRepository.getAllAmountByTypeAMonths(type.name(), startOfDay, endOfDay);
        if (allIncomeAMonths == null) allIncomeAMonths = BigDecimal.ZERO;
        return ApiResponse.builder()
                .status(HttpStatus.OK)
                .data(allIncomeAMonths)
                .build();
    }

    @Override
    public ApiResponse getAllAmountByTypeAYear(TransactionType type, Integer year) {
        BigDecimal allIncomeAYear = this.transactionRepository.getAllAmountByTypeAYear(type.name(), year);
        if (allIncomeAYear == null) allIncomeAYear = BigDecimal.ZERO;
        return ApiResponse.builder()
                .status(HttpStatus.OK)
                .message(RestConstants.SUCCESS)
                .data(allIncomeAYear)
                .build();
    }


    @Transactional
    @Override
    public ApiResponse updateBalance(Double amount, Long id, Months months, PaymentType paymentType, PaymentStatus paymentStatus) {
        try {
            // Validate input
            if (amount == null || amount <= 0) {
                return ApiResponse.builder()
                        .status(HttpStatus.BAD_REQUEST)
                        .message(RestConstants.INVALID_AMOUNT)
                        .build();
            }

            // Fetch required entities
            Company company = companyRepository.findOne()
                    .orElseThrow(() -> new ResourceNotFoundException(RestConstants.COMPANY_NOT_FOUND));

            MonthlyFee fee = monthlyFeeRepository.findByIdAndDeletedFalse(id)
                    .orElseThrow(() -> new ResourceNotFoundException(RestConstants.MONTHLY_FEE_NOT_FOUND));

            // Get current balances with null safety
            double cashBalance = company.getCashBalance() != null ? company.getCashBalance() : 0.0;
            double cardBalance = company.getCardBalance() != null ? company.getCardBalance() : 0.0;
            Double amountPaid = fee.getAmountPaid() != null ? fee.getAmountPaid() : 0.0;
            Double remainingBalance = fee.getRemainingBalance() != null ? fee.getRemainingBalance() : 0.0;

            // Validate refund amount doesn't exceed paid amount
            if (amount > amountPaid) {
                return ApiResponse.builder()
                        .status(HttpStatus.BAD_REQUEST)
                        .message(RestConstants.FAILED_TO_PROCESS_REFUND)
                        .build();
            }

            // Update company balance based on original payment type
            if (paymentType == PaymentType.NAQD) {
                cashBalance += amount;
                company.setCashBalance(cashBalance);
            } else if (paymentType == PaymentType.BANK) {
                cardBalance += amount;
                company.setCardBalance(cardBalance);
            }

            // Update fee records
            fee.setAmountPaid(amountPaid - amount);
            fee.setRemainingBalance(remainingBalance + amount);
            fee.setMonths(months);
            fee.setStatus(calculateRefundStatus(fee, paymentStatus));

            // Save changes
            company = companyRepository.save(company);
            fee = monthlyFeeRepository.save(fee);

            // Record refund transaction
            Transaction transaction = new Transaction();
            transaction.setTransactionType(TransactionType.INCOME); // Refund is income for company
            transaction.setPaymentType(paymentType);
            transaction.setAmount(amount);
            transaction.setDescription("Refund for overpayment");
            transaction.setMonthlyFee(fee);
            transaction.setCompany(company);
            transactionRepository.save(transaction);

            return ApiResponse.builder()
                    .status(HttpStatus.OK)
                    .message(RestConstants.SUCCESS)
                    .data(monthlyFeeMapper.toDto(fee))
                    .build();

        } catch (Exception e) {
//            log.error("Error processing balance update: {}", e.getMessage(), e);
            return ApiResponse.builder()
                    .status(HttpStatus.BAD_GATEWAY)
                    .message(RestConstants.FAILED_TO_PROCESS_REFUND)
                    .build();
        }
    }

    private PaymentStatus calculateRefundStatus(MonthlyFee fee, PaymentStatus requestedStatus) {
        if (fee.getAmountPaid() <= 0) {
            return PaymentStatus.UNPAID;
        } else if (fee.getRemainingBalance() <= 0) {
            return PaymentStatus.FULLY_PAID;
        } else {
            return PaymentStatus.PARTIALLY_PAID;
        }
    }

    @Override
    public ApiResponse getGraphStatistics(Integer year) {

        List<Object[]> result = this.transactionRepository.getMonthlyAmountsByTypeAndYear(year);
        Map<Integer, MonthlyStatisticsDto> statisticsDtoMap = new LinkedHashMap<>();

        Months[] allMonths = Months.values();
        for (int i = 0; i < allMonths.length; i++) {
            statisticsDtoMap.put(i + 1, new MonthlyStatisticsDto(allMonths[i].name()));
        }
        for (Object[] row : result) {
            Integer monthNum = ((Number) row[0]).intValue(); // Extract month number
            String type = (String) row[1];  // Extract transaction type (INCOME/OUTCOME)
            Double amount = (Double) row[2];  // Extract amount

            // Get the MonthlyStatisticsDto for the corresponding month number
            MonthlyStatisticsDto dto = statisticsDtoMap.get(monthNum);

            // Set the income or outcome depending on the type
            if (type.equalsIgnoreCase(TransactionType.INCOME.name())) {
                dto.setIncome(amount);
            } else if (type.equalsIgnoreCase(TransactionType.OUTCOME.name())) {
                dto.setOutcome(amount);
            }
        }
        List<MonthlyStatisticsDto> finalList = new ArrayList<>(statisticsDtoMap.values());
        return ApiResponse.builder()
                .status(HttpStatus.OK)
                .message(RestConstants.SUCCESS)
                .data(finalList)
                .build();
    }

    @Override
    public ApiResponse deleteMFList(List<Long> ids) {

        return null;
    }

    @Override
    public ApiResponse getAllByCategoryStatus(MonthlyFeeFilterWithStatus filter, Pageable pageable) {
        Page<MonthlyFee> monthlyFeePage = this.monthlyFeeRepository.findAll(filter, pageable);
        if (!monthlyFeePage.isEmpty()) {
            return ApiResponse.builder()
                    .status(HttpStatus.OK)
                    .message(RestConstants.SUCCESS)
                    .data(this.monthlyFeeMapper.dtoList(monthlyFeePage.getContent()))
                    .elements(monthlyFeePage.getTotalElements())
                    .pages(monthlyFeePage.getTotalPages())
                    .build();
        }
        return ApiResponse.builder()
                .status(HttpStatus.OK)
                .data(new ArrayList<>())
                .build();
    }

    @Override
    public ApiResponse getStatistics(PeriodType periodType,
                                     TransactionType transactionType,
                                     LocalDate startDate,
                                     LocalDate endDate,
                                     Integer year) {
        BigDecimal amount = BigDecimal.ZERO;
        LocalDateTime startDateTime;
        LocalDateTime endDateTime;
        LocalDate today = LocalDate.now();

        switch (periodType) {
            case DAILY -> {
                amount = transactionRepository.getAllAmountByTypeADay(
                        transactionType != null ? transactionType.name() : null, today);
            }

            case WEEKLY -> {
                LocalDate startOfWeek = today.with(DayOfWeek.MONDAY);
                LocalDate endOfWeek = today.with(DayOfWeek.SUNDAY);
                startDateTime = startOfWeek.atStartOfDay();
                endDateTime = endOfWeek.atTime(LocalTime.MAX);

                amount = transactionRepository.getAllAmountByTypeAWeek(
                        transactionType != null ? transactionType.name() : null, startDateTime, endDateTime);

            }

            case MONTHLY -> {
                LocalDate startOfMonth = today.withDayOfMonth(1);
                LocalDate endOfMonth = today.withDayOfMonth(today.lengthOfMonth());
                startDateTime = startOfMonth.atStartOfDay();
                endDateTime = endOfMonth.atTime(LocalTime.MAX);

                amount = transactionRepository.getAllAmountByTypeAMonths(
                        transactionType != null ? transactionType.name() : null, startDateTime, endDateTime);

            }

            case YEARLY -> {
                int targetYear = (year != null) ? year : today.getYear();
                amount = transactionRepository.getAllAmountByTypeAYear(
                        transactionType != null ? transactionType.name() : null, targetYear);

            }

            case RANGE -> {
                // Only RANGE uses user-supplied dates
                if (startDate == null || endDate == null) {
                    return ApiResponse.builder()
                            .status(HttpStatus.BAD_REQUEST)
                            .message("Start date and end date are required for RANGE type")
                            .build();
                }
                startDateTime = startDate.atStartOfDay();
                endDateTime = endDate.atTime(LocalTime.MAX);

                amount = transactionRepository.getAllAmountByTypeBetweenDates(
                        transactionType, startDateTime, endDateTime);

            }
        }

        return ApiResponse.builder()
                .status(HttpStatus.OK)
                .data(amount != null ? amount : BigDecimal.ZERO)
                .build();
    }

    @Override
    public ApiResponse getAllTransactionsStudent(Long studentContractId) {
        StudentContract studentContract = this.studentContractRepository.findByIdAndDeletedFalse(studentContractId)
                .orElseThrow(() -> new ResourceNotFoundException(RestConstants.USER_NOT_FOUND));
        List<Transaction> transactionList = new ArrayList<>();
        List<MonthlyFee> monthlyFees = studentContract.getMonthlyFees();
        List<StudentCutAmountDto> cutList = new ArrayList<>();
        List<String> imageUrls = new ArrayList<>();
        for (MonthlyFee fee : monthlyFees) {
            transactionList.addAll(this.transactionRepository.findAllByMonthlyFeeId(fee.getId()));
            List<Image> imageList = this.imageRepository.findAllByMonthlyFeeId(fee.getId());
            List<String> stringList = imageList.stream()
                    .map(Image::getUrl)
                    .toList();
            imageUrls.addAll(stringList);
            // Cutting amounts
            if (fee.getCutAmount() != null && fee.getCutAmount() > 0) {
                cutList.add(
                        StudentCutAmountDto.builder()
                                .studentContractId(studentContract.getId())
                                .month(fee.getMonths())
                                .amount(fee.getCutAmount())
                                .reason(fee.getReason())
                                .imageUrls(imageUrls)
                                .feeId(fee.getId())
                                .build()
                );
            }
        }
        TransactionDto.TransactionResponseWithCuttingAmountDto transactionDtoList = new TransactionDto.TransactionResponseWithCuttingAmountDto();
        List<TransactionDto> dtoList = transactionMapper.toDto(transactionList);
        transactionDtoList.setTransactions(dtoList);
        transactionDtoList.setCuttingAmounts(cutList);

        return ApiResponse.builder()
                .status(HttpStatus.OK)
                .message(RestConstants.SUCCESS)
                .data(transactionDtoList)
                .build();
    }

    @Override
    public ApiResponse getAllTransactionsEmployee(Long employeeId) {
        User employee = this.userRepository.findByIdAndDeletedFalse(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException(RestConstants.USER_NOT_FOUND));
        List<MonthlyFee> monthlyFee = this.monthlyFeeRepository.findAllByEmployeeId(employee.getId());
        List<Transaction> transactionList = new ArrayList<>();
        for (MonthlyFee fee : monthlyFee) {
            transactionList.addAll(this.transactionRepository.findAllByMonthlyFeeId(fee.getId()));
        }
        List<TransactionDto> dtoList = transactionMapper.toDto(transactionList);
        return ApiResponse.builder()
                .status(HttpStatus.OK)
                .message(RestConstants.SUCCESS)
                .data(dtoList)
                .build();
    }

    private final static String ADJUSTMENT = "ADJUSTMENT";

    @Override
    public ApiResponse cutStudentAmount(StudentCutAmountDto dto) {

        if (dto.getImageUrls() == null || dto.getImageUrls().isEmpty()) {
            return ApiResponse.builder()
                    .status(HttpStatus.BAD_REQUEST)
                    .message("RASM YUKLASH MAJBURIY!")
                    .build();
        }
        List<Image> images = new ArrayList<>();
        for (String imageUrl : dto.getImageUrls()) {
            Image image = this.imageRepository.findByUrl(imageUrl)
                    .orElseThrow(() -> new ResourceNotFoundException("RASM TOPILMADI!"));
            images.add(image);
        }

        StudentContract studentContract = this.studentContractRepository
                .findByIdAndDeletedFalse(dto.getStudentContractId())
                .orElseThrow(() -> new ResourceNotFoundException(RestConstants.USER_NOT_FOUND));

        Category category = this.categoryRepository.findByName(ADJUSTMENT)
                .orElseThrow(() -> new ResourceNotFoundException(RestConstants.CATEGORY_NOT_FOUND));

        StudentTariff tariff = studentContract.getTariff();

        // Get the correct monthly amount based on tariff type
        double correctMonthlyAmount = getCorrectMonthlyAmount(tariff);

        Optional<MonthlyFee> monthlyFeeOptional = this.monthlyFeeRepository
                .findByStudentContractIdAndMonthsAndAcademicYear(studentContract.getId(), dto.getMonth(), studentContract.getAcademicYear());

        if (monthlyFeeOptional.isPresent()) {
            MonthlyFee fee = monthlyFeeOptional.get();

            double oldTotal = fee.getTotalFee();
            double amountPaid = fee.getAmountPaid();
            double cutAmount = dto.getAmount() != null ? dto.getAmount() : 0.0;
            double newTotal = oldTotal - cutAmount;
            double newRemainingBalance = newTotal - amountPaid;
            if (cutAmount > oldTotal) {
                return ApiResponse.builder()
                        .status(HttpStatus.BAD_REQUEST)
                        .message("Chegirma summasi oy uchun to'liq summadan katta bo'lishi mumkin emas!")
                        .build();
            }

            fee.setTotalFee(newTotal);
            fee.setRemainingBalance(newRemainingBalance);
            fee.setCutAmount(cutAmount);
            fee.setReason(dto.getReason());
            fee.setCategory(category);
            fee.setTariffName(tariff.getName());
            // attach images
            if (fee.getImages() == null) fee.setImages(new ArrayList<>());
            fee.getImages().addAll(images);
            images.forEach(img -> img.setMonthlyFee(fee));

            if (amountPaid >= newTotal) {
                fee.setStatus(PaymentStatus.FULLY_PAID);
            } else if (amountPaid > 0) {
                fee.setStatus(PaymentStatus.PARTIALLY_PAID);
            } else {
                fee.setStatus(PaymentStatus.UNPAID);
            }

            this.monthlyFeeRepository.save(fee);
        } else {
            // Use the correct monthly amount based on tariff type, not the full contract amount
            double cuttingAmount = dto.getAmount() != null ? dto.getAmount() : 0.0;
            double newTotalFee = correctMonthlyAmount - cuttingAmount;

            if (cuttingAmount > correctMonthlyAmount) {
                return ApiResponse.builder()
                        .status(HttpStatus.BAD_REQUEST)
                        .message("Chegirma summasi oy uchun to'liq summadan katta bo'lishi mumkin emas!")
                        .build();
            }

            MonthlyFee fee = new MonthlyFee();
            fee.setMonths(dto.getMonth());
            fee.setTotalFee(newTotalFee);
            fee.setCutAmount(cuttingAmount);
            fee.setAmountPaid(0.0);
            fee.setRemainingBalance(newTotalFee);
            fee.setReason(dto.getReason());
            fee.setCategory(category);
            fee.setImages(images);
            images.forEach(img -> img.setMonthlyFee(fee)); // important to set the back-reference
            fee.setTariffName(tariff.getName()); // Set tariff name for consistency

            if (newTotalFee <= 0) {
                fee.setStatus(PaymentStatus.FULLY_PAID);
            } else {
                fee.setStatus(PaymentStatus.UNPAID);
            }

            fee.setStudentContract(studentContract);
            fee.setCompany(studentContract.getCompany());

            this.monthlyFeeRepository.save(fee);
        }

        return ApiResponse.builder()
                .status(HttpStatus.CREATED)
                .message(RestConstants.SUCCESS)
                .build();
    }

    private boolean isValidImageFile(MultipartFile file) {
        String contentType = file.getContentType();
        return contentType != null &&
                (contentType.equals("image/jpeg") ||
                        contentType.equals("image/png") ||
                        contentType.equals("image/gif"));
    }


    /**
     * Helper method to get correct monthly amount based on tariff type
     */
    private double getCorrectMonthlyAmount(StudentTariff tariff) {
        TariffStatus tariffStatus = tariff.getTariffStatus();
        double tariffAmount = tariff.getAmount();

        return switch (tariffStatus) {
            case YEARLY -> tariffAmount / 10.0; // Yearly divided by 10 months
            case QUARTERLY -> tariffAmount / 2.5; // Quarterly divided by 2.5 months (as in your payment code)
            default -> tariffAmount; // Monthly tariff
        };
    }

    @Override
    public ApiResponse updateCutStudentAmount(StudentCutAmountDto dto, Long studentContractId) {

        StudentContract studentContract = this.studentContractRepository.findByIdAndDeletedFalse(studentContractId)
                .orElseThrow(() -> new ResourceNotFoundException(RestConstants.STUDENT_NOT_FOUND));

        Category category = this.categoryRepository.findByName(ADJUSTMENT)
                .orElseThrow(() -> new ResourceNotFoundException(RestConstants.CATEGORY_NOT_FOUND));

        StudentTariff studentTariff = studentContract.getTariff();
        double monthlyAmount = this.getCorrectMonthlyAmount(studentTariff);

        MonthlyFee oldFee = this.monthlyFeeRepository.findByIdAndDeletedFalse(dto.getFeeId())
                .orElseThrow(() -> new ResourceNotFoundException(RestConstants.MONTHLY_FEE_NOT_FOUND));

        Months oldMonth = oldFee.getMonths();
        Months newMonth = dto.getMonth() != null ? dto.getMonth() : oldMonth;

        double oldCutAmount = oldFee.getCutAmount() != null ? oldFee.getCutAmount() : 0.0;
        double newCutAmount = dto.getAmount() != null ? dto.getAmount() : oldCutAmount;

        // 1. If month is the same → just update cut
        if (oldMonth.equals(newMonth)) {
            double beforeCutTotal = oldFee.getTotalFee() + oldCutAmount;
            double newTotal = beforeCutTotal - newCutAmount;

            if (newTotal < 0) {
                return ApiResponse.builder()
                        .status(HttpStatus.BAD_REQUEST)
                        .message("Chegirma summasi oy uchun to'liq summadan katta bo'lishi mumkin emas!")
                        .build();
            }

            oldFee.setCutAmount(newCutAmount);
            oldFee.setTotalFee(newTotal);
            oldFee.setRemainingBalance(newTotal - oldFee.getAmountPaid());
            if (dto.getReason() != null) oldFee.setReason(dto.getReason());
            oldFee.setCategory(category);
            oldFee.setTariffName(studentTariff.getName());

            recalcStatus(oldFee);
            this.monthlyFeeRepository.save(oldFee);

        } else {
            // 2. Reverse old month
            double restoredTotal = oldFee.getTotalFee() + oldCutAmount;
            oldFee.setTotalFee(restoredTotal);
            oldFee.setCutAmount(0.0);
            oldFee.setRemainingBalance(restoredTotal - oldFee.getAmountPaid());
            oldFee.setReason(null);
            oldFee.setCategory(null);

            recalcStatus(oldFee);
            this.monthlyFeeRepository.save(oldFee);

            // 3. Apply cut to new month
            MonthlyFee newFee = this.monthlyFeeRepository
                    .findByStudentContractIdAndMonthsAndAcademicYear(
                            studentContract.getId(), newMonth, studentContract.getAcademicYear()
                    )
                    .orElseGet(() -> {
                        MonthlyFee mf = new MonthlyFee();
                        mf.setMonths(newMonth);
                        mf.setStudentContract(studentContract);
                        mf.setCompany(studentContract.getCompany());
                        mf.setAmountPaid(0.0);
                        mf.setTariffName(studentTariff.getName());
                        return mf;
                    });

            double beforeCutTotalNew = newFee.getTotalFee() > 0 ? newFee.getTotalFee() : monthlyAmount;
            double afterCutTotal = beforeCutTotalNew - newCutAmount;
            if (afterCutTotal < 0) {
                return ApiResponse.builder()
                        .status(HttpStatus.BAD_REQUEST)
                        .message("Chegirma summasi yangi oy uchun ham katta bo'lib qoldi!")
                        .build();
            }

            newFee.setCutAmount(newCutAmount);
            newFee.setTotalFee(afterCutTotal);
            newFee.setRemainingBalance(afterCutTotal - newFee.getAmountPaid());
            newFee.setReason(dto.getReason());
            newFee.setCategory(category);

            recalcStatus(newFee);
            this.monthlyFeeRepository.save(newFee);
        }

        return ApiResponse.builder()
                .status(HttpStatus.OK)
                .message(RestConstants.SUCCESS)
                .build();
    }

    // helper
    private void recalcStatus(MonthlyFee fee) {
        if (fee.getAmountPaid() >= fee.getTotalFee()) {
            fee.setStatus(PaymentStatus.FULLY_PAID);
        } else if (fee.getAmountPaid() > 0) {
            fee.setStatus(PaymentStatus.PARTIALLY_PAID);
        } else {
            fee.setStatus(PaymentStatus.UNPAID);
        }
    }


    @Override
    public void downloadExcel(TransactionSheetFilter filter, OutputStream outputStream) {

        List<Transaction> allByStatus = this.transactionRepository.findAll(filter);

        log.info(allByStatus.toString());

        if (allByStatus.isEmpty()) {
            throw new ResourceNotFoundException("Ma'lumot topilmadi!");
        }

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Transaksiyalar");

            // Column headers
            String[] columns = {
                    "ID",
                    "Transaksiya Turi",
                    "To'lov turi",
                    "Miqdor",
                    "Izoh",
                    "Xodim",
                    "O'quvchi",
                    "Oy",
                    "Sana"
            };

            // Header Style (Bold + Centered + Light Blue)
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerFont.setFontHeightInPoints((short) 12);
            headerStyle.setFont(headerFont);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setFillForegroundColor(IndexedColors.TEAL.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            setBorders(headerStyle);

            // Data Style (Centered + Light Yellow)
            CellStyle dataStyle = workbook.createCellStyle();
            Font dataFont = workbook.createFont();
            dataStyle.setAlignment(HorizontalAlignment.CENTER);
            dataFont.setFontHeightInPoints((short) 10);
            dataStyle.setFillForegroundColor(IndexedColors.WHITE.getIndex());
            dataStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            dataStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            dataStyle.setWrapText(true); // Enable text wrapping
            dataStyle.setAlignment(HorizontalAlignment.CENTER);
            setBorders(dataStyle);

            // Create Header Row with increased height
            Row headerRow = sheet.createRow(0);
            headerRow.setHeightInPoints(20);
            for (int i = 0; i < columns.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columns[i]);
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, 5000);
            }

            // Group transactions by employee and month
            Map<String, List<Transaction>> groupedTransactions = groupTransactionsByEmployeeAndMonth(allByStatus);

            // Fill data rows with merging
            int rowIdx = 1;
            for (Map.Entry<String, List<Transaction>> entry : groupedTransactions.entrySet()) {
                List<Transaction> transactions = entry.getValue();
                int startRow = rowIdx;

                for (int i = 0; i < transactions.size(); i++) {
                    Transaction transaction = transactions.get(i);
                    Row row = sheet.createRow(rowIdx++);
                    row.setHeightInPoints(18);

                    // Fill all transaction data
                    createStyledCell(row, 0, transaction.getId(), dataStyle);
                    createStyledCell(row, 1, transaction.getTransactionType() != null ?
                            transaction.getTransactionType().name() : "N/A", dataStyle);
                    createStyledCell(row, 2, transaction.getPaymentType() != null ?
                            transaction.getPaymentType().name() : "N/A", dataStyle);
                    createStyledCell(row, 3, transaction.getAmount() != null ?
                            transaction.getAmount() : 0.0, dataStyle);
                    createStyledCell(row, 4, transaction.getDescription() != null ?
                            transaction.getDescription() : "N/A", dataStyle);

                    // Employee name and month - only fill in first row of group
                    if (i == 0) {
                        createStyledCell(row, 5, getEmployeeName(transaction), dataStyle);
                        createStyledCell(row, 7, getMonthName(transaction), dataStyle);
                    } else {
                        // Create empty cells for merging
                        createStyledCell(row, 5, "", dataStyle);
                        createStyledCell(row, 7, "", dataStyle);
                    }

                    // Student name (always show)
                    createStyledCell(row, 6, getStudentName(transaction), dataStyle);

                    // Date (always show)
                    createStyledCell(row, 8, transaction.getCreatedAt() != null
                            ? transaction.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy.MM.dd | HH:mm"))
                            : "N/A", dataStyle);
                }

                // Merge cells if there are multiple transactions for same employee+month
                if (transactions.size() > 1) {
                    int endRow = rowIdx - 1;
                    try {
                        // Merge employee name column (column 5)
                        sheet.addMergedRegion(new CellRangeAddress(startRow, endRow, 5, 5));

                        // Merge month column (column 7)
                        sheet.addMergedRegion(new CellRangeAddress(startRow, endRow, 7, 7));
                    } catch (Exception e) {
                        log.warn("Error merging cells for rows {}-{}: {}", startRow, endRow, e.getMessage());
                    }
                }
            }

            // Auto-size columns
            for (int i = 0; i < columns.length; i++) {
                try {
                    sheet.autoSizeColumn(i);
                } catch (Exception e) {
                    log.warn("Could not auto-size column {}: {}", i, e.getMessage());
                }
            }

            workbook.write(outputStream);
            outputStream.flush();
        } catch (IOException e) {
            throw new RuntimeException("Error exporting to Excel", e);
        }
    }

    // Helper method to group transactions by employee and month
    private Map<String, List<Transaction>> groupTransactionsByEmployeeAndMonth(List<Transaction> transactions) {
        Map<String, List<Transaction>> grouped = new LinkedHashMap<>();

        for (Transaction transaction : transactions) {
            String employeeName = getEmployeeName(transaction);
            String monthName = getMonthName(transaction);
            String key = employeeName + "|" + monthName;

            grouped.computeIfAbsent(key, k -> new ArrayList<>()).add(transaction);
        }

        return grouped;
    }

    // Helper methods to safely extract data
    private String getEmployeeName(Transaction transaction) {
        if (transaction.getMonthlyFee() != null && transaction.getMonthlyFee().getEmployee() != null) {
            return transaction.getMonthlyFee().getEmployee().getFullName();
        }
        return "N/A";
    }

    private String getStudentName(Transaction transaction) {
        if (transaction.getMonthlyFee() != null && transaction.getMonthlyFee().getStudentContract() != null) {
            return transaction.getMonthlyFee().getStudentContract().getStudentFullName();
        }
        return "N/A";
    }

    private String getMonthName(Transaction transaction) {
        if (transaction.getMonthlyFee() != null && transaction.getMonthlyFee().getMonths() != null) {
            return transaction.getMonthlyFee().getMonths().name();
        }
        return "N/A";
    }

    // Helper method to create styled cells
    private void createStyledCell(Row row, int columnIndex, Object value, CellStyle style) {
        Cell cell = row.createCell(columnIndex);

        if (value instanceof Number) {
            cell.setCellValue(((Number) value).doubleValue());
        } else if (value instanceof Boolean) {
            cell.setCellValue((Boolean) value);
        } else {
            cell.setCellValue(value != null ? value.toString() : "N/A");
        }

        cell.setCellStyle(style);
    }

    @Override
    public ApiResponse getAllTransactionsEmployeeId(Pageable pageable, TransactionEmployeeFilter filter) {
        Page<Transaction> page = this.transactionRepository.findAll(filter, pageable);
        if (page.isEmpty()) {
            return ApiResponse.builder()
                    .status(HttpStatus.OK)
                    .data(new ArrayList<>())
                    .build();
        }
        List<TransactionDto.ToFilterDto> dtoList = this.transactionMapper.toFilterDto(page.getContent());
        return ApiResponse.builder()
                .status(HttpStatus.OK)
                .message(RestConstants.SUCCESS)
                .data(dtoList)
                .build();

    }

    @Override
    public ApiResponse getAllAmountOverview(Integer year, LocalDate date) {
        int targetYear = (year != null) ? year : LocalDate.now().getYear();
        LocalDate targetDate = (date != null) ? date : LocalDate.now();

        TransactionDto.TransactionOverview overview = new TransactionDto.TransactionOverview();
        overview.setIncome(getIncomeExpenseByPeriods(TransactionType.INCOME, targetYear, targetDate));
        overview.setOutcome(getIncomeExpenseByPeriods(TransactionType.OUTCOME, targetYear, targetDate));
        overview.setYear(targetYear);

        return ApiResponse.builder()
                .status(HttpStatus.OK)
                .message(RestConstants.SUCCESS)
                .data(overview)
                .build();
    }

    private TransactionDto.PeriodBreakdown getIncomeExpenseByPeriods(
            TransactionType transactionType,
            Integer year,
            LocalDate date
    ) {
        return TransactionDto.PeriodBreakdown.builder()
                .daily(transactionRepository.getTodayAmount(transactionType.name(), date))
                .weekly(transactionRepository.getThisWeekAmount(transactionType.name(), date))
                .monthly(transactionRepository.getThisMonthAmount(transactionType.name(), year, date.getMonthValue()))
                .yearly(transactionRepository.getYearAmount(transactionType.name(), year))
                .build();
    }


    @Override
    public ApiResponse getHourlyAmountData(TransactionType type, LocalDate date, Integer year) {
        LocalDate today = date != null ? date : LocalDate.now();
        year = year != null ? year : LocalDate.now().getYear();
        List<TransactionDto.HourlyAmountData> hourlyAmount = this.getHourlyAmount(type.name(), today, year);

        return ApiResponse.builder()
                .status(HttpStatus.OK)
                .message(RestConstants.SUCCESS)
                .data(hourlyAmount)
                .build();
    }

    @Override
    public ApiResponse getWeeklyChartData(TransactionType type, Integer year, LocalDate fromDate, LocalDate toDate) {
        if (type == null) {
            return ApiResponse.builder()
                    .status(HttpStatus.BAD_REQUEST)
                    .message("Transaction type cannot be null")
                    .build();
        }

        // Enhanced date validation
        if (fromDate != null && toDate != null) {
            if (fromDate.isAfter(toDate)) {
                return ApiResponse.builder()
                        .status(HttpStatus.BAD_REQUEST)
                        .message("fromDate cannot be after toDate")
                        .build();
            }

            // Check if date range is too large (optional)
            long daysBetween = ChronoUnit.DAYS.between(fromDate, toDate);
            if (daysBetween > 365) {
                return ApiResponse.builder()
                        .status(HttpStatus.BAD_REQUEST)
                        .message("Date range cannot exceed 1 year")
                        .build();
            }
        }

        List<TransactionDto.ChartDataPoint> chartData = getWeeklyData(type, year, fromDate, toDate);

        return ApiResponse.builder()
                .status(HttpStatus.OK)
                .message(chartData.isEmpty() ? "No data found for the specified period" : RestConstants.SUCCESS)
                .data(chartData)
                .build();
    }

    @Override
    public ApiResponse getMonthlyChartData(TransactionType type, Integer year, LocalDate fromDate, LocalDate toDate) {
        List<TransactionDto.ChartDataPoint> chartData = getMonthlyData(type, year, fromDate, toDate);

        return ApiResponse.builder()
                .status(HttpStatus.OK)
                .message(RestConstants.SUCCESS)
                .data(chartData)
                .build();
    }

    @Override
    public ApiResponse getYearlyChartData(TransactionType type, Integer year) {
        List<TransactionDto.ChartDataPoint> chartData = getYearlyData(type, year);
        return ApiResponse.builder()
                .status(HttpStatus.OK)
                .message(RestConstants.SUCCESS)
                .data(chartData)
                .build();
    }

    @Override
    public ApiResponse uploadImage(MultipartFile image) {

        if (image == null || image.isEmpty()) {
            return ApiResponse.builder()
                    .status(HttpStatus.BAD_REQUEST)
                    .message("RASM TANLANMAGAN!")
                    .build();
        }
        if (!this.isValidImageFile(image)) {
            return ApiResponse.builder()
                    .status(HttpStatus.BAD_REQUEST)
                    .message("RASM YUKLASH MUMKIN EMAS!")
                    .build();
        }
        String uploadDir = System.getProperty("os.name").toLowerCase().contains("win")
                ? "C://fee-images/"
                : "/opt/fee-images/";

        try {

            // Generate unique file name
            String originalFilename = Paths.get(image.getOriginalFilename()).getFileName().toString();
            String imageName = UUID.randomUUID() + "_" + originalFilename;

            // Create directories if missing
            Path imagePath = Paths.get(uploadDir, imageName);
            Files.createDirectories(imagePath.getParent());

            // Save file to disk
            Files.write(imagePath, image.getBytes());

            // Build URL
            String fullUrl = baseURL + "/fee-images/" + imageName;

            // Save to DB
            Image imageEntity = new Image();
            imageEntity.setUrl(fullUrl);
            imageRepository.save(imageEntity);

            // Return response
            return ApiResponse.builder()
                    .status(HttpStatus.CREATED)
                    .message(RestConstants.SUCCESSFULLY_SAVED)
                    .data(fullUrl) // single URL
                    .build();

        } catch (IOException e) {
            throw new RuntimeException("RASM YUKLASHDA XATOLIK YUZ BERDI!", e);
        }
    }

    @Transactional
    @Override
    public ApiResponse removeImage(String imageUrl) {
        if (imageUrl == null || !imageUrl.contains("/fee-images/")) {
            return ApiResponse.builder()
                    .status(HttpStatus.BAD_REQUEST)
                    .message("Invalid image URL")
                    .build();
        }
        this.deleteOldImageFromDisk(imageUrl);
        return ApiResponse.builder()
                .status(HttpStatus.OK)
                .message(RestConstants.SUCCESSFULLY_DELETED)
                .build();
    }


    @Transactional
    public void deleteOldImageFromDisk(String imageUrl) {
        String uploadDir = getUploadDir();

        try {
            String fileName = imageUrl.substring(imageUrl.lastIndexOf("/") + 1);
            Path path = Paths.get(uploadDir, fileName);

            if (Files.exists(path)) {
                Files.delete(path);
                imageRepository.findByUrl(imageUrl).ifPresent(imageRepository::delete);
                System.out.println("Deleted file and DB record: " + fileName);
            } else {
                System.out.println("File not found: " + fileName);
            }
        } catch (IOException e) {
            System.err.println("Failed to delete old image: " + e.getMessage());
        }
    }

    private String getUploadDir() {
        return System.getProperty("os.name").toLowerCase().contains("win")
                ? "C://fee-images/"
                : "/opt/fee-images/";
    }

    // Helper methods
    private List<TransactionDto.ChartDataPoint> getWeeklyData(TransactionType type, Integer year, LocalDate fromDate, LocalDate toDate) {
        if (fromDate != null && toDate != null) {
            return this.getWeeklyDataForDateRange(type.name(), fromDate, toDate);
        } else if (year != null) {
            return this.getWeeklyDataForYear(type.name(), year);
        } else {
            // Current year
            return this.getWeeklyDataForYear(type.name(), LocalDate.now().getYear());
        }
    }

    private List<TransactionDto.ChartDataPoint> getMonthlyData(TransactionType type, Integer year, LocalDate fromDate, LocalDate toDate) {
        if (fromDate != null && toDate != null) {
            return this.getMonthlyDataForDateRange(type.name(), fromDate, toDate);
        } else if (year != null) {
            return this.getMonthlyDataForYear(type.name(), year);
        } else {
            // Current year
            return this.getMonthlyDataForYear(type.name(), LocalDate.now().getYear());
        }
    }

    private List<TransactionDto.ChartDataPoint> getYearlyData(TransactionType type, Integer year) {
        if (year == null) {
            year = LocalDate.now().getYear(); // Default to current year
        }
        return this.getYearlyDataForYear(type.name(), year);
    }

    // Updated getWeeklyDataForYear method
    private List<TransactionDto.ChartDataPoint> getWeeklyDataForYear(String type, Integer year) {
        List<Object[]> rawData = this.transactionRepository.getWeeklyDataForYearRaw(type, year);

        Map<Integer, TransactionDto.ChartDataPoint> dataMap = rawData.stream()
                .collect(Collectors.toMap(
                        row -> ((Number) row[0]).intValue(),
                        row -> TransactionDto.ChartDataPoint.builder()
                                .id(((Number) row[0]).intValue())
                                .label((String) row[1])
                                .amount(BigDecimal.valueOf(((Number) row[2]).doubleValue()))
                                .build()
                ));

        String[] dayNames = {"DUSHANBA", "SESHANBA", "CHORSHANBA", "PAYSHANBA", "JUMA", "SHANBA", "YAKSHANBA"};
        List<TransactionDto.ChartDataPoint> result = new ArrayList<>();

        for (int i = 1; i <= 7; i++) {
            TransactionDto.ChartDataPoint dataPoint = dataMap.getOrDefault(i,
                    TransactionDto.ChartDataPoint.builder()
                            .id(i)
                            .label(dayNames[i - 1])
                            .amount(BigDecimal.ZERO)
                            .build()
            );
            result.add(dataPoint);
        }

        return result;
    }

    // Updated getWeeklyDataForDateRange method
    private List<TransactionDto.ChartDataPoint> getWeeklyDataForDateRange(String type, LocalDate fromDate, LocalDate toDate) {
        // To'liq ma'lumot olish (barcha kunlar bilan)
        return getCompleteWeeklyData(type, fromDate, toDate);
    }

    // Service method - barcha kunlarni to'liq ko'rsatish uchun (0 summa bilan)
    private List<TransactionDto.ChartDataPoint> getCompleteWeeklyData(String type, LocalDate fromDate, LocalDate toDate) {
        // Ma'lumotlar bazasidan ma'lumot olish
        List<Object[]> rawData = this.transactionRepository.getWeeklyDataForDateRangeRaw(type, fromDate, toDate);

        // Ma'lumotlarni map ga o'tkazish
        Map<Integer, TransactionDto.ChartDataPoint> dataMap = rawData.stream()
                .collect(Collectors.toMap(
                        row -> ((Number) row[0]).intValue(),
                        row -> TransactionDto.ChartDataPoint.builder()
                                .id(((Number) row[0]).intValue())
                                .label((String) row[1])
                                .amount(BigDecimal.valueOf(((Number) row[2]).doubleValue()))
                                .build()
                ));

        // Barcha kunlarni yaratish
        String[] dayNames = {"DUSHANBA", "SESHANBA", "CHORSHANBA", "PAYSHANBA", "JUMA", "SHANBA", "YAKSHANBA"};
        List<TransactionDto.ChartDataPoint> result = new ArrayList<>();

        for (int i = 1; i <= 7; i++) {
            TransactionDto.ChartDataPoint dataPoint = dataMap.getOrDefault(i,
                    TransactionDto.ChartDataPoint.builder()
                            .id(i)
                            .label(dayNames[i - 1])
                            .amount(BigDecimal.ZERO)
                            .build()
            );
            result.add(dataPoint);
        }

        return result;
    }

    // Service implementations
    private List<TransactionDto.ChartDataPoint> getMonthlyDataForYearAndMonth(String type, Integer year, Integer month) {
        List<Object[]> rawData = this.transactionRepository.getMonthlyDataForYearAndMonthRaw(type, year, month);

        Map<Integer, TransactionDto.ChartDataPoint> dataMap = rawData.stream()
                .collect(Collectors.toMap(
                        row -> ((Number) row[0]).intValue(),
                        row -> TransactionDto.ChartDataPoint.builder()
                                .id(((Number) row[0]).intValue())
                                .label((String) row[1])
                                .amount(BigDecimal.valueOf(((Number) row[2]).doubleValue()))
                                .build()
                ));

        // O'sha oyning oxirgi kunini aniqlash
        LocalDate date = LocalDate.of(year, month, 1);
        int daysInMonth = date.lengthOfMonth();

        List<TransactionDto.ChartDataPoint> result = new ArrayList<>();

        for (int day = 1; day <= daysInMonth; day++) {
            TransactionDto.ChartDataPoint dataPoint = dataMap.getOrDefault(day,
                    TransactionDto.ChartDataPoint.builder()
                            .id(day)
                            .label(day + "-kun")
                            .amount(BigDecimal.ZERO)
                            .build()
            );
            result.add(dataPoint);
        }

        return result;
    }

    private List<TransactionDto.ChartDataPoint> getMonthlyDataForYear(String type, Integer year) {
        List<Object[]> rawData = this.transactionRepository.getMonthlyDataForYearRaw(type, year);

        Map<Integer, TransactionDto.ChartDataPoint> dataMap = rawData.stream()
                .collect(Collectors.toMap(
                        row -> ((Number) row[0]).intValue(),
                        row -> TransactionDto.ChartDataPoint.builder()
                                .id(((Number) row[0]).intValue())
                                .label((String) row[1])
                                .amount(BigDecimal.valueOf(((Number) row[2]).doubleValue()))
                                .build()
                ));

        // 1 dan 31 gacha barcha kunlarni yaratish
        List<TransactionDto.ChartDataPoint> result = new ArrayList<>();

        for (int day = 1; day <= 31; day++) {
            TransactionDto.ChartDataPoint dataPoint = dataMap.getOrDefault(day,
                    TransactionDto.ChartDataPoint.builder()
                            .id(day)
                            .label(day + "-kun")
                            .amount(BigDecimal.ZERO)
                            .build()
            );
            result.add(dataPoint);
        }

        return result;
    }

    private List<TransactionDto.ChartDataPoint> getMonthlyDataForDateRange(String type, LocalDate fromDate, LocalDate toDate) {
        List<Object[]> rawData = this.transactionRepository.getMonthlyDataForDateRangeRaw(type, fromDate, toDate);

        Map<Integer, TransactionDto.ChartDataPoint> dataMap = rawData.stream()
                .collect(Collectors.toMap(
                        row -> ((Number) row[0]).intValue(),
                        row -> TransactionDto.ChartDataPoint.builder()
                                .id(((Number) row[0]).intValue())
                                .label((String) row[1])
                                .amount(BigDecimal.valueOf(((Number) row[2]).doubleValue()))
                                .build()
                ));

        // Berilgan sanalar orasidagi barcha kunlarni yaratish
        List<TransactionDto.ChartDataPoint> result = new ArrayList<>();
        Set<Integer> addedDays = new HashSet<>(); // Takrorlanmaslik uchun

        LocalDate currentDate = fromDate;
        while (!currentDate.isAfter(toDate)) {
            int dayOfMonth = currentDate.getDayOfMonth();

            if (!addedDays.contains(dayOfMonth)) {
                TransactionDto.ChartDataPoint dataPoint = dataMap.getOrDefault(dayOfMonth,
                        TransactionDto.ChartDataPoint.builder()
                                .id(dayOfMonth)
                                .label(dayOfMonth + "-kun")
                                .amount(BigDecimal.ZERO)
                                .build()
                );
                result.add(dataPoint);
                addedDays.add(dayOfMonth);
            }

            currentDate = currentDate.plusDays(1);
        }

        // ID bo'yicha saralash
        result.sort(Comparator.comparing(TransactionDto.ChartDataPoint::getId));
        return result;
    }

    private List<TransactionDto.ChartDataPoint> getYearlyDataForYear(String type, Integer year) {
        List<Object[]> rawData = this.transactionRepository.getYearlyDataForYearRaw(type, year);

        Map<Integer, TransactionDto.ChartDataPoint> dataMap = rawData.stream()
                .collect(Collectors.toMap(
                        row -> ((Number) row[0]).intValue(),
                        row -> TransactionDto.ChartDataPoint.builder()
                                .id(((Number) row[0]).intValue())
                                .label((String) row[1])
                                .amount(BigDecimal.valueOf(((Number) row[2]).doubleValue()))
                                .build()
                ));

        // Create all 12 months
        String[] monthNames = {"YANVAR", "FEVRAL", "MART", "APREL", "MAY", "IYUN",
                "IYUL", "AVGUST", "SENTABR", "OKTABR", "NOYABR", "DEKABR"};
        List<TransactionDto.ChartDataPoint> result = new ArrayList<>();

        for (int month = 1; month <= 12; month++) {
            TransactionDto.ChartDataPoint dataPoint = dataMap.getOrDefault(month,
                    TransactionDto.ChartDataPoint.builder()
                            .id(month)
                            .label(monthNames[month - 1])
                            .amount(BigDecimal.ZERO)
                            .build()
            );
            result.add(dataPoint);
        }

        return result;
    }

    // Simple fix - just change the casting
    public List<TransactionDto.HourlyAmountData> getHourlyAmount(String type, LocalDate date, Integer year) {
        List<Object[]> rawData = this.transactionRepository.getHourlyAmountRaw(type, date, year);

        Map<Integer, TransactionDto.HourlyAmountData> hourlyMap = rawData.stream()
                .collect(Collectors.toMap(
                        row -> ((Number) row[0]).intValue(),
                        row -> TransactionDto.HourlyAmountData.builder()
                                .hour(((Number) row[0]).intValue())
                                .hourLabel(String.format("%02d:00", ((Number) row[0]).intValue()))
                                .amount(BigDecimal.valueOf(((Number) row[1]).doubleValue())) // Simple fix
                                .transactionCount(((Number) row[2]).intValue())
                                .build()
                ));

        List<TransactionDto.HourlyAmountData> result = new ArrayList<>();
        for (int hour = 0; hour < 24; hour++) {
            result.add(hourlyMap.getOrDefault(hour,
                    TransactionDto.HourlyAmountData.builder()
                            .hour(hour)
                            .hourLabel(String.format("%02d:00", hour))
                            .amount(BigDecimal.ZERO)
                            .transactionCount(0)
                            .build()
            ));
        }

        return result;
    }


    private void setBorders(CellStyle style) {
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
    }


}
