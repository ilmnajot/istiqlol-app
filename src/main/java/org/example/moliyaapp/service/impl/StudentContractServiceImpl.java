package org.example.moliyaapp.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.antlr.v4.runtime.misc.Pair;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.example.moliyaapp.dto.*;
import org.example.moliyaapp.entity.*;
import org.example.moliyaapp.enums.*;
import org.example.moliyaapp.exception.ResourceNotFoundException;
import org.example.moliyaapp.filter.ExpenseTransactionFilter;
import org.example.moliyaapp.filter.StudentContractFilter;
import org.example.moliyaapp.filter.StudentReminderFilter;
import org.example.moliyaapp.filter.TransactionFilter;
import org.example.moliyaapp.mapper.ReminderMapper;
import org.example.moliyaapp.mapper.StudentContractMapper;
import org.example.moliyaapp.mapper.TransactionMapper;
import org.example.moliyaapp.projection.*;
import org.example.moliyaapp.repository.*;
import org.example.moliyaapp.service.StudentContractService;
import org.example.moliyaapp.service.StudentMonthlyFeeGoogleSheet;
import org.example.moliyaapp.service.StudentTransactionGoogleSheet;
import org.example.moliyaapp.utils.DateUtils;
import org.example.moliyaapp.utils.RestConstants;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.OutputStream;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j
@RequiredArgsConstructor
@Service
public class StudentContractServiceImpl implements StudentContractService {

    private static final String STUDENT_INCOME = "O'QUVCHIDAN_KIRIM";

    private final StudentContractMapper studentContractMapper;
    private final CompanyRepository companyRepository;
    private final StudentContractRepository studentContractRepository;
    private final MonthlyFeeRepository monthlyFeeRepository;
    private final StudentTariffRepository studentTariffRepository;
    private final CategoryRepository categoryRepository;
    private final TransactionRepository transactionRepository;
    private final ReminderMapper reminderMapper;
    private final ReminderRepository reminderRepository;
    private final SmsServiceImpl smsServiceImpl;
    private final TransactionMapper transactionMapper;
    private final TariffChangeHistoryRepository tariffChangeHistoryRepository;
    private final GoogleSheetsOperations googleSheetsService;
    private final UserRepository userRepository;
    private final StudentTransactionGoogleSheet studentTransactionGoogleSheet;
    private final StudentMonthlyFeeGoogleSheet studentMonthlyFeeGoogleSheet;


    @Transactional
    @Override
    public ApiResponse createContract(StudentContractDto.CreateStudentContractDto dto) {

        long count = 0;
        String academicYear = dto.getAcademicYear();
        List<StudentContract> list = this.studentContractRepository.findAllByAcademicYear(academicYear);
        count = list.size();
        Optional<StudentContract> existingStudent = studentContractRepository.findByBCN(dto.getBCN(), dto.getAcademicYear());

        if (existingStudent.isPresent()) {
            return ApiResponse.builder()
                    .status(HttpStatus.BAD_REQUEST)
                    .message(RestConstants.STUDENT_ALREADY_EXIST)
                    .build();
        }
        Company company = this.companyRepository.findOne()
                .orElseThrow(() -> new ResourceNotFoundException(RestConstants.COMPANY_NOT_FOUND));

        StudentTariff studentTariff = studentTariffRepository.findById(dto.getTariffId())
                .orElseThrow(() -> new ResourceNotFoundException(RestConstants.TARIFF_NOT_FOUND));

        StudentContract contract = this.studentContractMapper.toStudentContract(dto);
        contract.setCompany(company);
        contract.setTariff(studentTariff);
        contract.setAmount(studentTariff.getAmount());
        contract.setStatus(true);
        contract.setUniqueId(String.valueOf(count + 1));

        TariffChangeHistory history = new TariffChangeHistory();
        history.setStudentContract(contract);
        history.setTariffStatus(studentTariff.getTariffStatus());
        history.setTariffAmount(studentTariff.getAmount());
        history.setFromDate(LocalDate.now());
        history.setToDate(null);
        history.setTariff(studentTariff);
        this.tariffChangeHistoryRepository.save(history);

        contract = this.studentContractRepository.save(contract);
        log.info("uniqueId {}", contract.getUniqueId());
        try {
            this.googleSheetsService.initializeSheet();
            this.googleSheetsService.recordStudentContract(contract, studentTariff, company);
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        StudentContractDto studentContractDtoResponse = this.studentContractMapper.toStudentContractDtoResponse(contract);
        log.info("uniqueId {}", studentContractDtoResponse.getUniqueId());
        return ApiResponse.builder()
                .data(studentContractDtoResponse)
                .status(HttpStatus.CREATED)
                .message(RestConstants.SUCCESS)
                .build();
    }

    @Override
    public ApiResponse getStudentContract(Long id) {
        StudentContract contract = this.studentContractRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException(RestConstants.CONTRACT_NOT_FOUND));
        StudentContractDto studentContractDtoResponse = this.studentContractMapper.toStudentContractDtoResponse(contract);
        return ApiResponse.builder()
                .message(RestConstants.SUCCESS)
                .status(HttpStatus.OK)
                .data(studentContractDtoResponse)
                .build();
    }

    @Override
    public ApiResponse getAllStudentContracts(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<StudentContract> contractList = this.studentContractRepository.findAll(pageable);
        Set<StudentContractDto> studentContractDtoSet = contractList
                .getContent()
                .stream()
                .map(this.studentContractMapper::toStudentContractDtoResponse)
                .collect(Collectors.toSet());
        return ApiResponse.builder()
                .status(HttpStatus.OK)
                .data(studentContractDtoSet)
                .elements(contractList.getTotalElements())
                .pages(contractList.getTotalPages())
                .message(RestConstants.SUCCESS)
                .build();
    }

    @Override
    public ApiResponse deleteContractStudent(Long id) {
        StudentContract contract = this.studentContractRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException(RestConstants.CONTRACT_NOT_FOUND));
        contract.setDeleted(true);
        contract.setStatus(false);
        this.studentContractRepository.save(contract);
        return ApiResponse.builder()
                .status(HttpStatus.OK)
                .message("Deleted...")
                .build();
    }

    @Transactional
    @Override
    public ApiResponse update(Long id, StudentContractDto.UpdateStudentContractDto dto) {
        StudentContract contract = this.studentContractRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException(RestConstants.CONTRACT_NOT_FOUND));

        StudentTariff studentTariff = this.studentTariffRepository.findById(dto.getTariffId())
                .orElseThrow(() -> new ResourceNotFoundException(RestConstants.TARIFF_NOT_FOUND));

        if (dto.getAcademicYear() != null) {
            String academicYear = dto.getAcademicYear();
            List<StudentContract> list = this.studentContractRepository.findAllByAcademicYear(academicYear);
            long count = list.size();
            contract.setUniqueId(String.valueOf(count + 1));
        }
        this.studentContractMapper.updateStudentContract(contract, dto);
        contract.setTariff(studentTariff);
        contract = this.studentContractRepository.save(contract);
        try {
            this.googleSheetsService.initializeSheet();
            this.googleSheetsService.updateStudentContractRow(contract);
        } catch (Exception e) {
            log.info("failed to update in google sheet!");
        }
        StudentContractDto studentContractDtoResponse = this.studentContractMapper.toStudentContractDtoResponse(contract);
        return ApiResponse.builder()
                .message(RestConstants.SUCCESS)
                .status(HttpStatus.OK)
                .data(studentContractDtoResponse)
                .build();
    }

    @Override
    public ApiResponse getStudentContractByDate(int page, int size, LocalDate fromDate, LocalDate toDate) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id"));
        Page<StudentContract> contractPage = this.studentContractRepository.getAllByDate(fromDate, toDate, pageable);
        Set<StudentContractDto> studentContractDtoSet = contractPage
                .getContent()
                .stream()
                .map(this.studentContractMapper::toStudentContractDtoResponse)
                .collect(Collectors.toSet());

        return ApiResponse.builder()
                .status(HttpStatus.OK)
                .message(RestConstants.SUCCESS)
                .data(studentContractDtoSet)
                .build();
    }

    @Override
    public ApiResponse getDailyStudentContract() {
        LocalDateTime start = DateUtils.startOfToday();
        LocalDateTime end = DateUtils.endOfToday();
        List<StudentContract> byCreatedAtBetween = this.studentContractRepository.findByCreatedAtBetween(start, end);
        Integer totalStudentContracts = this.studentContractRepository.getTotalStudentContracts(start, end);
        if (byCreatedAtBetween.isEmpty() || totalStudentContracts == 0) {

            return ApiResponse.builder()
                    .status(HttpStatus.OK)
                    .message(RestConstants.SUCCESS)
                    .data(new ArrayList<>())
                    .build();
        }
        return ApiResponse.builder()
                .status(HttpStatus.OK)
                .message(RestConstants.SUCCESS)
                .meta(Map.of("TotalNumberOfContracts", totalStudentContracts))
                .data(this.studentContractMapper.dtoList(byCreatedAtBetween))
                .build();
    }

    @Override
    public ApiResponse getWeeklyStudentContract() {
        LocalDateTime start = DateUtils.startOfWeek();
        LocalDateTime end = DateUtils.endOfToday();
        List<StudentContract> byCreatedAtBetween = this.studentContractRepository.findByCreatedAtBetween(start, end);
        Integer totalStudentContracts = this.studentContractRepository.getTotalStudentContracts(start, end);
        if (byCreatedAtBetween.isEmpty() || totalStudentContracts == 0) {

            return ApiResponse.builder()
                    .status(HttpStatus.OK)
                    .message(RestConstants.SUCCESS)
                    .data(new ArrayList<>())
                    .build();
        }
        return ApiResponse.builder()
                .status(HttpStatus.OK)
                .message(RestConstants.SUCCESS)
                .meta(Map.of("TotalNumberOfContracts", totalStudentContracts))
                .data(this.studentContractMapper.dtoList(byCreatedAtBetween))
                .build();
    }

    @Override
    public ApiResponse getMonthlyStudentContract() {
        LocalDateTime start = DateUtils.startOfMonth();
        LocalDateTime end = DateUtils.endOfToday();
        List<StudentContract> byCreatedAtBetween = this.studentContractRepository.findByCreatedAtBetween(start, end);
        Integer totalStudentContracts = this.studentContractRepository.getTotalStudentContracts(start, end);
        if (byCreatedAtBetween.isEmpty() || totalStudentContracts == 0) {

            return ApiResponse.builder()
                    .status(HttpStatus.OK)
                    .message(RestConstants.SUCCESS)
                    .data(new ArrayList<>())
                    .build();
        }
        return ApiResponse.builder()
                .status(HttpStatus.OK)
                .message(RestConstants.SUCCESS)
                .meta(Map.of("TotalNumberOfContracts", totalStudentContracts))
                .data(this.studentContractMapper.dtoList(byCreatedAtBetween))
                .build();
    }

    @Override
    public ApiResponse getMonthlyStudentContractsByYearAndYear(int year, Months month) {
        LocalDate start = LocalDate.of(year, month.ordinal() + 1, 1);
        LocalDate end = start.with(TemporalAdjusters.lastDayOfMonth());
        LocalDateTime s = start.atStartOfDay();
        LocalDateTime e = end.atTime(LocalTime.MAX);
        List<StudentContract> contractList = this.studentContractRepository.findByCreatedAtBetween(s, e);
        Integer totalStudentContracts = this.studentContractRepository.getTotalStudentContracts(s, e);
        if (contractList.isEmpty() || totalStudentContracts == 0) {
            return ApiResponse.builder()
                    .message(RestConstants.SUCCESS)
                    .data(new ArrayList<>())
                    .build();
        }
        return ApiResponse.builder()
                .status(HttpStatus.OK)
                .message(RestConstants.SUCCESS)
                .data(contractList)
                .meta(Map.of("totalStudentContracts", totalStudentContracts))
                .build();
    }

    @Override
    public ApiResponse deleteStudents(List<Long> ids) {
        List<Long> longList = this.studentContractRepository.findAllByIdsAndDeletedTure(ids);
        if (longList.isEmpty()) {
            return ApiResponse.builder()
                    .status(HttpStatus.OK)
                    .message("BO'M BO'SH")
                    .build();
        }
        this.studentContractRepository.deleteALlByIds(longList);
        return ApiResponse.builder()
                .status(HttpStatus.OK)
                .message("TANLANGAN O'QUVCHILAR RO'YXATI BUTUNLAYGA O'CHIRILDI!")
                .build();
    }

    @Override
    public ApiResponse getMonthlyContractStats(int month, int year) {
        // 1. Get the actual data from DB
        List<Object[]> rawResults = studentContractRepository.getDailyContractCountsForMonth(month, year);

        // 2. Convert results to a map: day -> count
        Map<Integer, Long> resultMap = rawResults.stream()
                .collect(Collectors.toMap(
                        row -> ((Number) row[0]).intValue(),  // Day of month
                        row -> ((Number) row[1]).longValue()  // Count
                ));

        // 3. Determine the number of days in the given month/year
        YearMonth yearMonth = YearMonth.of(year, month); // month: 1=January, 12=December
        int daysInMonth = yearMonth.lengthOfMonth();

        // 4. Create DTO list with all days, even if missing in DB results
        List<DailyContractCountDTO> dailyStats = IntStream.rangeClosed(1, daysInMonth)
                .mapToObj(day -> new DailyContractCountDTO(
                        day,
                        resultMap.getOrDefault(day, 0L) // Use 0 if no data for this day
                ))
                .collect(Collectors.toList());

        // 5. Return as ApiResponse
        return ApiResponse.builder()
                .status(HttpStatus.OK)
                .message("Success")
                .data(dailyStats)
                .build();
    }


    @Override
    public ApiResponse getContractStatistics(PeriodType periodType, Boolean status, LocalDate startDate, LocalDate endDate, Integer year) {
        LocalDateTime startDateTime;
        LocalDateTime endDateTime;
        LocalDate today = LocalDate.now();
        LocalDateTime startedOfToday = DateUtils.startOfToday();
        LocalDateTime endedOfToday = DateUtils.endOfToday();
        Integer contracts = 0;

        switch (periodType) {
            case DAILY -> {
                contracts = studentContractRepository.getTotalStudentContractsStatistics(startedOfToday, endedOfToday, status);
            }

            case WEEKLY -> {
                LocalDate startOfWeek = today.with(DayOfWeek.MONDAY);
                LocalDate endOfWeek = today.with(DayOfWeek.SUNDAY);
                startDateTime = startOfWeek.atStartOfDay();
                endDateTime = endOfWeek.atTime(LocalTime.MAX);
                contracts = this.studentContractRepository.getTotalStudentContractsStatistics(startDateTime, endDateTime, status);

            }

            case MONTHLY -> {
                LocalDate startOfMonth = today.withDayOfMonth(1);
                LocalDate endOfMonth = today.withDayOfMonth(today.lengthOfMonth());
                startDateTime = startOfMonth.atStartOfDay();
                endDateTime = endOfMonth.atTime(LocalTime.MAX);

                contracts = this.studentContractRepository.getTotalStudentContractsStatistics(startDateTime, endDateTime, status);

            }

            case YEARLY -> {
                int targetYear = (year != null) ? year : today.getYear();
                contracts = this.studentContractRepository.getAllByCurrentYear(status, targetYear);

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
                contracts = this.studentContractRepository.getTotalStudentContractsStatistics(startDateTime, endDateTime, status);

            }
        }
        return ApiResponse.builder()
                .status(HttpStatus.OK)
                .message(RestConstants.SUCCESS)
                .data(contracts)
                .build();
    }


    @Override
    public ApiResponse getYearlyStudentContract() {
        LocalDateTime start = DateUtils.startOfYear();
        LocalDateTime end = DateUtils.endOfToday();
        List<StudentContract> byCreatedAtBetween = this.studentContractRepository.findByCreatedAtBetween(start, end);
        Integer totalStudentContracts = this.studentContractRepository.getTotalStudentContracts(start, end);
        if (byCreatedAtBetween.isEmpty() || totalStudentContracts == 0) {

            return ApiResponse.builder()
                    .status(HttpStatus.OK)
                    .message(RestConstants.SUCCESS)
                    .data(new ArrayList<>())
                    .build();
        }
        return ApiResponse.builder()
                .status(HttpStatus.OK)
                .message(RestConstants.SUCCESS)
                .meta(Map.of("TotalNumberOfContracts", totalStudentContracts))
                .data(this.studentContractMapper.dtoList(byCreatedAtBetween))
                .build();
    }

    @Override
    public ApiResponse getAllStudents(int page, int size, Boolean status) {
        Pageable pageable = PageRequest.of(page, size);
        Page<StudentContract> allByStatus = this.studentContractRepository.findAllByStatus(status, pageable);
        Set<StudentContractDto> studentContractDtoSet = allByStatus
                .getContent()
                .stream()
                .map(this.studentContractMapper::toStudentContractDtoResponse)
                .collect(Collectors.toSet());
        return ApiResponse.builder()
                .status(HttpStatus.OK)
                .message(RestConstants.SUCCESS)
                .elements(allByStatus.getTotalElements())
                .pages(allByStatus.getTotalPages())
                .data(studentContractDtoSet)
                .build();
    }

    @Override
    public ApiResponse getDeletedStudents(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<StudentContract> contractPage = this.studentContractRepository.findAllByDeletedIsTrue(pageable);
        Set<StudentContractDto> studentContractDtoSet = contractPage
                .getContent()
                .stream()
                .map(this.studentContractMapper::toStudentContractDtoResponse)
                .collect(Collectors.toSet());
        return ApiResponse.builder()
                .status(HttpStatus.OK)
                .message(RestConstants.SUCCESS)
                .elements(contractPage.getTotalElements())
                .pages(contractPage.getTotalPages())
                .data(studentContractDtoSet)
                .build();
    }


    @Override
    public ApiResponse filter(StudentContractFilter filter, Pageable pageable) {
        Months months = filter.getMonths();
        List<StudentContract> all = this.studentContractRepository.findAll();
        Long count = all
                .stream()
                .count();

        Page<StudentContract> page = this.studentContractRepository.findAll(filter, pageable);

        List<StudentContract> list = this.studentContractRepository.findAll(filter);

        Double amount = 0.0;
        MonthlyFee fee = null;
        if (!page.isEmpty()) {
            for (StudentContract studentContract : list) {
                List<MonthlyFee> feeList = this.monthlyFeeRepository.findAllByStudentContractIdAndMonthsAndAcademicYear(
                        studentContract.getId(),
                        filter.getMonths(),
                        studentContract.getAcademicYear()
                );
                if (!feeList.isEmpty()) {
                    fee = feeList.get(feeList.size() - 1);
                    amount += fee.getRemainingBalance();
                } else {
                    switch (studentContract.getTariff().getTariffStatus()) {
                        case QUARTERLY -> {
                            amount += studentContract.getAmount() / 2.50;
                        }
                        case YEARLY -> {
                            amount += studentContract.getAmount() / 10.0;
                        }
                        case MONTHLY -> {
                            amount += studentContract.getAmount();
                        }
                    }
                }
            }
            Double remian = 0.0;

            List<StudentContractDto> dtoList = this.studentContractMapper.dtoList(page.getContent());
            for (StudentContractDto dto : dtoList) {
                StudentContract studentContract = this.studentContractRepository.findById(dto.getId())
                        .orElse(null);
                if (studentContract != null) {
                    List<MonthlyFee> feeOptional = this.monthlyFeeRepository.findAllByStudentContractIdAndMonthsAndAcademicYear(studentContract.getId(), months, dto.getAcademicYear());
                    if (!feeOptional.isEmpty()) {
                        fee = feeOptional.get(feeOptional.size() - 1);
                        remian = fee.getRemainingBalance();
                    } else {
                        switch (studentContract.getTariff().getTariffStatus()) {
                            case QUARTERLY -> {
                                remian = studentContract.getAmount() / 2.50;
                            }
                            case YEARLY -> {
                                remian = studentContract.getAmount() / 10.0;
                            }
                            case MONTHLY -> {
                                remian = studentContract.getAmount();
                            }
                        }
                    }
                    dto.setRemain(remian);
                }
            }
            return ApiResponse.builder()
                    .status(HttpStatus.OK)
                    .pages(page.getTotalPages())
                    .elements(page.getTotalElements())
                    .data(dtoList)
                    .meta(
                            Map.of("amount", amount,
                                    "all", count)
                    )
                    .build();
        }
        return ApiResponse.builder()
                .status(HttpStatus.OK)
                .data(new ArrayList<>())
                .build();
    }


    private Double parseDouble(Cell cell) {
        if (cell == null) {
            return 0.0;
        }

        try {
            switch (cell.getCellType()) {
                case NUMERIC:
                    return cell.getNumericCellValue();
                case STRING:
                    String stringValue = cell.getStringCellValue().trim();
                    if (stringValue.isEmpty()) {
                        return 0.0;
                    }
                    return Double.parseDouble(stringValue);
                case FORMULA:
                    return cell.getNumericCellValue();
                case BLANK:
                    return 0.0;
                default:
                    return 0.0;
            }
        } catch (NumberFormatException | IllegalStateException e) {
            log.warn("Could not parse double value from cell: {}", cell.toString());
            return 0.0;
        }
    }

    private Boolean parseStatus(String value) {
        if (value == null || value.trim().isEmpty()) {
            return true; // Default to active if not specified
        }
        String normalizedValue = value.trim().toLowerCase();
        return "active".equals(normalizedValue) || "faol".equals(normalizedValue) || "1".equals(normalizedValue);
    }

    private <T extends Enum<T>> T parseEnum(Class<T> enumClass, String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return Enum.valueOf(enumClass, value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("Could not parse enum {} for value: {}", enumClass.getSimpleName(), value);
            return null;
        }
    }

    private String validateAcademicYear(String academicYear) {
        if (academicYear == null || academicYear.trim().isEmpty()) {
            throw new RuntimeException("Academic year is required");
        }
        return academicYear.trim();
    }

    private String validatePhone(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            return null;
        }
        return phone.trim();
    }


    // Updated main upload method to handle grouped rows
    @Transactional
    @Override
    public void uploadExcel(MultipartFile file) {
        Company company = this.companyRepository.findOne()
                .orElseThrow(() -> new ResourceNotFoundException(RestConstants.COMPANY_NOT_FOUND));
        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);

            // Skip header row and group rows by student contract ID
            Map<Long, List<Row>> rowsByContract = new HashMap<>();

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                Long contractId = parseLong(row.getCell(0)); // Contract ID column
                if (contractId != null) {
                    rowsByContract.computeIfAbsent(contractId, k -> new ArrayList<>()).add(row);
                }
            }


            // Process each contract with its associated rows
            for (Map.Entry<Long, List<Row>> entry : rowsByContract.entrySet()) {
                List<Row> contractRows = entry.getValue();
                Long contractIdFromExcel = entry.getKey();
                if (contractRows.isEmpty()) continue;

                // Use first row to create the contract (ignore the ID from Excel)
                Row firstRow = contractRows.get(0);

                try {
                    // Always create new contract (ignore Excel ID)
                    StudentContract contract = createStudentContract(firstRow, company);
                    StudentContract savedContract = studentContractRepository.save(contract);

                    // Process monthly fees and transactions for this contract
                    processMonthlyFeesAndTransactions(contractRows, savedContract);

                } catch (Exception e) {
                    log.error("Error processing contract at row {}: {}", firstRow.getRowNum() + 1, e.getMessage());
                    throw new RuntimeException("Error processing contract at row " + (firstRow.getRowNum() + 1), e);
                }
            }

        } catch (IOException e) {
            log.error("Error reading Excel file: {}", e.getMessage());
            throw new RuntimeException("Error reading Excel file", e);
        }
    }

    private void updateMonthlyFeeFromRow(MonthlyFee monthlyFee, Row row, StudentContract contract) {
        Double debt = parseDouble(row.getCell(28)); // Debt column
        Double cutAmount = parseDouble(row.getCell(29)); // Cut amount column
        String reason = getCellValue(row.getCell(30)); // Reason column

        // Update monthly fee data
        if (debt != null) {
            monthlyFee.setRemainingBalance(debt);
        }

        if (cutAmount != null && cutAmount > 0) {
            monthlyFee.setCutAmount(cutAmount);
            monthlyFee.setReason(reason);
            // Adjust total fee
            monthlyFee.setTotalFee(contract.getAmount() - cutAmount);
        }

        // Calculate amount paid based on total - remaining - discount
        double totalRequired = contract.getAmount() - (cutAmount != null ? cutAmount : 0.0);
        double remaining = debt != null ? debt : monthlyFee.getRemainingBalance();
        double amountPaid = Math.max(0, totalRequired - remaining);
        monthlyFee.setAmountPaid(amountPaid);

        // Update payment status
        if (remaining <= 0) {
            monthlyFee.setStatus(PaymentStatus.FULLY_PAID);
        } else if (amountPaid > 0) {
            monthlyFee.setStatus(PaymentStatus.PARTIALLY_PAID);
        } else {
            monthlyFee.setStatus(PaymentStatus.UNPAID);
        }
    }

    private Months parseMonthFromUzbek(String monthName) {
        if (monthName == null) return null;

        return switch (monthName.toUpperCase().trim()) {
            case "SENTABR" -> Months.SENTABR;
            case "OKTABR" -> Months.OKTABR;
            case "NOYABR" -> Months.NOYABR;
            case "DEKABR" -> Months.DEKABR;
            case "YANVAR" -> Months.YANVAR;
            case "FEVRAL" -> Months.FEVRAL;
            case "MART" -> Months.MART;
            case "APREL" -> Months.APREL;
            case "MAY" -> Months.MAY;
            case "IYUN" -> Months.IYUN;
            default -> null;
        };
    }

    private MonthlyFee createNewMonthlyFee(StudentContract contract, Months month, LocalDateTime createdAt) {
        MonthlyFee newFee = new MonthlyFee();
        newFee.setStudentContract(contract);
        newFee.setMonths(month);
        newFee.setCompany(contract.getCompany());
        newFee.setTotalFee(contract.getAmount());
        newFee.setAmountPaid(0.0);
        newFee.setRemainingBalance(contract.getAmount());
        newFee.setCutAmount(0.0);
        newFee.setStatus(PaymentStatus.UNPAID);
        newFee.setCreatedAt(createdAt);

        String tariffName = contract.getTariff() != null ? contract.getTariff().getName() : "N/A";
        newFee.setTariffName(tariffName);

        Category category = this.categoryRepository.findByName(STUDENT_INCOME).orElse(null);
        newFee.setCategory(category);

        return newFee;
    }

    // Helper method to parse Long values
    private Long parseLong(Cell cell) {
        if (cell == null) return null;

        try {
            return switch (cell.getCellType()) {
                case NUMERIC -> (long) cell.getNumericCellValue();
                case STRING -> {
                    String value = cell.getStringCellValue().trim();
                    yield value.isEmpty() ? null : Long.parseLong(value);
                }
                default -> null;
            };
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private StudentContract createStudentContract(Row row, Company company) {
        StudentContract contract = new StudentContract();
        contract.setCompany(company);

        // Set basic contract information
        LocalDate contractedDate = parseLocalDate(row.getCell(1));
        contract.setContractedDate(contractedDate);
        contract.setAcademicYear(validateAcademicYear(getCellValue(row.getCell(2))));
        contract.setStudentFullName(getCellValue(row.getCell(3)));
        contract.setBirthDate(parseLocalDate(row.getCell(4)));
        contract.setGender(parseEnum(Gender.class, getCellValue(row.getCell(5))));
        if (contractedDate != null) {
            contract.setCreatedAt(contractedDate.atStartOfDay().plusMinutes(60));
        } else {
            contract.setCreatedAt(LocalDateTime.now());
        }
        // ✅ Grade parsing with validation
        String gradeValue = getCellValue(row.getCell(6));
        if (gradeValue == null || !gradeValue.contains("-")) {
            throw new IllegalArgumentException("❌ Noto'g'ri sinf formati: '" + gradeValue + "'. Format: '1-A'");
        }

        String[] parts = gradeValue.split("-");
        if (parts.length != 2) {
            throw new IllegalArgumentException("❌ Sinf formati noto'g'ri: '" + gradeValue + "'. Format: '1-A'");
        }

        String gradePart = parts[0].trim();
        String letterPart = parts[1].trim();

        try {
            contract.setGrade(StudentGrade.valueOf("GRADE_" + gradePart));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("❌ Yaroqsiz raqamli sinf: 'GRADE_" + gradePart + "'. 1 dan 12 gacha bo'lishi kerak.");
        }

        try {
            contract.setStGrade(Grade.valueOf(letterPart.toUpperCase()));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("❌ Yaroqsiz harfli sinf: '" + letterPart + "'. A-Z oralig'ida bo'lishi kerak.");
        }
        contract.setLanguage(parseEnum(StudyLanguage.class, getCellValue(row.getCell(7))));
        contract.setGuardianFullName(getCellValue(row.getCell(8)));
        contract.setGuardianType(parseEnum(GuardianType.class, getCellValue(row.getCell(9))));
        contract.setPhone1(validatePhone(getCellValue(row.getCell(10))));
        contract.setPhone2(validatePhone(getCellValue(row.getCell(11))));
        contract.setPassportId(getCellValue(row.getCell(12)));
        contract.setPassportIssuedBy(getCellValue(row.getCell(13)));
        contract.setAddress(getCellValue(row.getCell(14)));

        // Handle tariff
        String tariffName = getCellValue(row.getCell(15));
        StudentTariff tariff = resolveTariff(tariffName, row.getRowNum() + 1);
        contract.setTariff(tariff);

        // Set remaining fields
        contract.setGuardianJSHSHIR(getCellValue(row.getCell(16)));
        contract.setComment(getCellValue(row.getCell(17)));
        contract.setContractStartDate(parseLocalDate(row.getCell(18)));
        contract.setContractEndDate(parseLocalDate(row.getCell(19)));
        contract.setStatus(parseStatus(getCellValue(row.getCell(20))));
        contract.setInactiveDate(parseLocalDate(row.getCell(21)));
        contract.setClientType(parseEnum(ClientType.class, getCellValue(row.getCell(22))));
        contract.setBCN(getCellValue(row.getCell(23)));
        contract.setAmount(parseDouble(row.getCell(25)));

        return contract;
    }


    private StudentTariff resolveTariff(String tariffName, int rowNum) {
        if (tariffName == null || tariffName.trim().isEmpty()) {
            log.warn("Row {}: Tariff not specified, using default", rowNum);
            return studentTariffRepository.findByName(tariffName)
                    .orElseThrow(() -> new RuntimeException("Row " + rowNum + ": Tariff not found"));
        }

        tariffName = tariffName.trim();
        String finalTariffName = tariffName;
        return studentTariffRepository.findByNameIgnoreCase(tariffName)
                .orElseThrow(() -> {
                    List<String> availableTariffs = studentTariffRepository.findAll()
                            .stream()
                            .map(StudentTariff::getName)
                            .toList();
                    return new RuntimeException("Row " + rowNum + ": Tariff '" + finalTariffName + "' not found. Available tariffs: " + availableTariffs);

                });
    }

    // ✅ REPLACE this method in your code
    private void processMonthlyFeesAndTransactions(List<Row> contractRows, StudentContract contract) {
        // Group rows by month to handle multiple transactions per month
        Map<String, List<Row>> rowsByMonth = new HashMap<>();

        // Process all rows for this contract
        for (Row row : contractRows) {
            String monthName = getCellValue(row.getCell(26)); // Month column
            if (monthName != null && !monthName.equals("N/A") && !monthName.trim().isEmpty()) {
                rowsByMonth.computeIfAbsent(monthName, k -> new ArrayList<>()).add(row);
            }
        }

        // 🔥 ADD THIS LOG to see how many months per contract
        log.info("Processing {} months for contract {}", rowsByMonth.size(), contract.getId());

        // Process each month's data
        for (Map.Entry<String, List<Row>> entry : rowsByMonth.entrySet()) {
            String monthName = entry.getKey();
            List<Row> monthRows = entry.getValue();

            // Convert month name to enum
            Months month = parseMonthFromUzbek(monthName);
            if (month == null) {
                log.warn("Invalid month name: {}", monthName);
                continue;
            }
            LocalDateTime monthlyFeeCreatedAt = monthRows.stream()
                    .map(row -> parseCellToLocalDateTime(row.getCell(33))) // Replace XX with your time column
                    .filter(Objects::nonNull)
                    .min(LocalDateTime::compareTo) // Use earliest date
                    .orElse(LocalDateTime.now());

            // Get or create monthly fee
            MonthlyFee monthlyFee = monthlyFeeRepository
                    .findByStudentContractIdAndMonthsAndAcademicYear(
                            contract.getId(), month, contract.getAcademicYear()
                    )
                    .orElseGet(() -> createNewMonthlyFee(contract, month, monthlyFeeCreatedAt));

            // Process the first row to get monthly fee data (debt, cut amount, reason)
            Row firstRow = monthRows.get(0);
            updateMonthlyFeeFromRow(monthlyFee, firstRow, contract);

            MonthlyFee savedFee = monthlyFeeRepository.save(monthlyFee);

            // Process all transactions for this month
            for (Row row : monthRows) {
                Double paidAmount = parseDouble(row.getCell(27)); // Payment amount column
                PaymentType paymentType = parseEnum(PaymentType.class, getCellValue(row.getCell(31))); // Payment type column
                LocalDateTime transactionDate = parseCellToLocalDateTime(row.getCell(32));
                // Only create transaction if there's actual payment
                if (paidAmount != null && paidAmount > 0) {
                    createTransaction(savedFee, paidAmount, paymentType, month.name() + " oyi uchun to'lov", transactionDate);
                }
            }
        }
    }

    private void createTransaction(MonthlyFee monthlyFee, Double amount, PaymentType paymentType, String description, LocalDateTime createdAt) {
        Company company = this.companyRepository.findOne()
                .orElseThrow(() -> new ResourceNotFoundException(RestConstants.COMPANY_NOT_FOUND));
        // Only update cash balance for cash payments
        if (paymentType == PaymentType.NAQD) {
            Double cashBalance = company.getCashBalance() != null ? company.getCashBalance() : 0.0;
            company.setCashBalance(cashBalance + amount);
        } else if (paymentType == PaymentType.BANK || paymentType == PaymentType.TERMINAL || paymentType == PaymentType.CLICK || paymentType == PaymentType.PAYMEE) {
            Double card = company.getCardBalance() != null ? company.getCardBalance() : 0.0;
            company.setCardBalance(card + amount);
        }

        this.companyRepository.save(company);

        Transaction transaction = new Transaction();
        transaction.setTransactionType(TransactionType.INCOME);
        transaction.setPaymentType(paymentType != null ? paymentType : PaymentType.NAQD);
        transaction.setMonthlyFee(monthlyFee);
        transaction.setAmount(amount);
        transaction.setCompany(company);
        transaction.setDescription(description);
        transaction.setCreatedBy(getCurrentUsername());
        transaction.setCreatedAt(createdAt != null ? createdAt : LocalDateTime.now());

        transactionRepository.save(transaction);
    }

    private Long getCurrentUsername() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() instanceof User user) {
                return user.getId();
            }
            return 1L; // Default user ID if authentication fails
        } catch (Exception e) {
            log.warn("Error getting current user: {}", e.getMessage());
            return 1L; // Default user ID
        }
    }

    private LocalDateTime parseCellToLocalDateTime(Cell cell) {
        if (cell == null) return null;

        switch (cell.getCellType()) {
            case NUMERIC:
                // Check if it's a date formatted cell
                if (DateUtil.isCellDateFormatted(cell)) {
                    // Excel stores dates as numeric values
                    Date date = cell.getDateCellValue();
                    return date.toInstant()
                            .atZone(ZoneId.systemDefault())
                            .toLocalDateTime();
                } else {
                    // If it's just a number, treat it as Excel date serial number
                    try {
                        double numericValue = cell.getNumericCellValue();
                        // Check if this could be a valid Excel date
                        if (DateUtil.isValidExcelDate(numericValue)) {
                            Date date = DateUtil.getJavaDate(numericValue);
                            return date.toInstant()
                                    .atZone(ZoneId.systemDefault())
                                    .toLocalDateTime();
                        }
                    } catch (Exception e) {
                        log.warn("Could not parse numeric cell as date: " + cell.getNumericCellValue());
                    }
                    return null;
                }
            case STRING:
                String dateStr = cell.getStringCellValue().trim();
                if (dateStr.isEmpty() || dateStr.equals("N/A")) {
                    return null;
                }
                try {
                    // Try common date-time formats
                    DateTimeFormatter[] formatters = {
                            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
                            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),
                            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"),
                            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"),
                            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
                            DateTimeFormatter.ofPattern("dd/MM/yyyy")
                    };

                    for (DateTimeFormatter formatter : formatters) {
                        try {
                            // If date only (no time), set time to 00:00
                            if (dateStr.matches("\\d{4}-\\d{2}-\\d{2}$") || dateStr.matches("\\d{2}/\\d{2}/\\d{4}$")) {
                                LocalDate localDate = LocalDate.parse(dateStr, formatter);
                                return localDate.atStartOfDay();
                            } else {
                                return LocalDateTime.parse(dateStr, formatter);
                            }
                        } catch (DateTimeParseException ignored) {
                            // Try next formatter
                        }
                    }

                    log.error("Could not parse date string: " + dateStr);
                    return null;
                } catch (Exception e) {
                    log.error("Error parsing date string: " + dateStr, e);
                    return null;
                }
            case FORMULA:
                // Handle formula cells that might evaluate to dates
                try {
                    CellValue cellValue = cell.getSheet().getWorkbook()
                            .getCreationHelper().createFormulaEvaluator()
                            .evaluate(cell);

                    if (cellValue.getCellType() == CellType.NUMERIC) {
                        if (DateUtil.isValidExcelDate(cellValue.getNumberValue())) {
                            Date date = DateUtil.getJavaDate(cellValue.getNumberValue());
                            return date.toInstant()
                                    .atZone(ZoneId.systemDefault())
                                    .toLocalDateTime();
                        }
                    }
                } catch (Exception e) {
                    log.warn("Could not evaluate formula cell as date", e);
                }
                return null;
            default:
                log.warn("Unsupported cell type for date: " + cell.getCellType());
                return null;
        }
    }

    @Override
    public void downloadExcel(StudentContractFilter filter, OutputStream outputStream) {
        List<StudentContract> allByStatus = this.studentContractRepository.findAll(filter);

        log.info("Found {} students to export", allByStatus.size());
        if (allByStatus.isEmpty()) {
            throw new ResourceNotFoundException("No Students found by filter criteria");
        }

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Students");


            // Define columns based on isTransactions
            String[] columns = Boolean.TRUE.equals(filter.getTransactions()) ?
                    new String[]{
                            "ID", "SHARTNOMA TUZILGAN VAQT", "O'QUV YILI", "O'QUVCHINING F.I.SH", "TUG'ILGAN KUN",
                            "JINSI", "SINF", "O'QUV TILI", "VASIY F.I.SH", "VASIY TURI",
                            "TEL 1", "TEL 2", "PASPORT ID", "KIM TOMONIDAN BERILGAN", "MANZIL",
                            "TARIF", "VASIY JSHSHIR", "IZOH", "SHARTNOMA BOSHLANISH SANASI", "SHARTNOMA TUGASH SANASI",
                            "STATUS", "SHARTNOMA BEKOR QILINGAN KUN", "MIJOZ TURI", "GUVOHNOMA SERIYASI", "TASHKILOT",
                            "OYLIK MIQDOR"
                    } :
                    new String[]{
                            "ID", "SHARTNOMA TUZILGAN VAQT", "O'QUV YILI", "O'QUVCHINING F.I.SH", "TUG'ILGAN KUN",
                            "JINSI", "SINF", "O'QUV TILI", "VASIY F.I.SH", "VASIY TURI",
                            "TEL 1", "TEL 2", "PASPORT ID", "KIM TOMONIDAN BERILGAN", "MANZIL",
                            "TARIF", "VASIY JSHSHIR", "IZOH", "SHARTNOMA BOSHLANISH SANASI", "SHARTNOMA TUGASH SANASI",
                            "STATUS", "SHARTNOMA BEKOR QILINGAN KUN", "MIJOZ TURI", "GUVOHNOMA SERIYASI", "TASHKILOT",
                            "OYLIK MIQDOR", "OY", "TO'LANGAN MIQDOR", "QARZ", "CHEGIRILGAN", "SABAB", "TO'LOV TURI", "TRANSAKSIYA VAQTI"
                    };

            // Create styles
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle dataStyle = createDataStyle(workbook);

            // Create header row
            createHeaderRow(sheet, columns, headerStyle);

            // Fill data rows
            int rowIdx = 1;
            for (StudentContract contract : allByStatus) {
                rowIdx = processContractRows(sheet, contract, rowIdx, dataStyle, Boolean.TRUE.equals(filter.getTransactions()));
            }

            // Auto-size columns
            autoSizeColumns(sheet, columns.length);
            workbook.write(outputStream);
            outputStream.flush();
        } catch (IOException e) {
            log.error("Error exporting to Excel: {}", e.getMessage());
            throw new RuntimeException("Error exporting to Excel", e);
        }
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
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
        return headerStyle;
    }

    private CellStyle createDataStyle(Workbook workbook) {
        CellStyle dataStyle = workbook.createCellStyle();
        Font dataFont = workbook.createFont();
        dataFont.setFontHeightInPoints((short) 10);
        dataStyle.setFont(dataFont);
        dataStyle.setAlignment(HorizontalAlignment.CENTER);
        dataStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        dataStyle.setFillForegroundColor(IndexedColors.WHITE.getIndex());
        dataStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        setBorders(dataStyle);
        return dataStyle;
    }

    private void createHeaderRow(Sheet sheet, String[] columns, CellStyle headerStyle) {
        Row headerRow = sheet.createRow(0);
        headerRow.setHeightInPoints(20);
        for (int i = 0; i < columns.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(columns[i]);
            cell.setCellStyle(headerStyle);
        }
    }

    private int processContractRows(Sheet sheet, StudentContract contract, int startRowIdx, CellStyle dataStyle, boolean isTransactions) {
        if (isTransactions) {
            // Only process basic student data
            Row row = sheet.createRow(startRowIdx++);
            fillBasicStudentData(row, contract, null, dataStyle);
            return startRowIdx;
        }

        // Get all months with their Uzbek names
        Map<Months, String> monthNames = createMonthNamesMap();

        // Get all monthly fees for this contract
        List<MonthlyFee> monthlyFees = getAllMonthlyFees(contract);

        boolean hasAnyData = false;
        int currentRowIdx = startRowIdx;

        // Process each monthly fee
        for (MonthlyFee monthlyFee : monthlyFees) {
            if (monthlyFee == null) continue;

            List<Transaction> transactions = getTransactions(monthlyFee.getId());

            if (transactions.isEmpty()) {
                // Create row for monthly fee without transactions
                Row row = sheet.createRow(currentRowIdx++);
                fillBasicStudentData(row, contract, monthlyFee, dataStyle);
                fillMonthlyFeeData(row, monthlyFee, monthNames.get(monthlyFee.getMonths()), dataStyle);
                hasAnyData = true;
            } else {
                // Create separate row for each transaction with merging
                int monthStartRow = currentRowIdx;
                for (int i = 0; i < transactions.size(); i++) {
                    Transaction transaction = transactions.get(i);
                    Row row = sheet.createRow(currentRowIdx++);
                    fillBasicStudentData(row, contract, monthlyFee, dataStyle);
                    fillMonthlyFeeDataWithMerging(row, monthlyFee, transaction,
                            monthNames.get(monthlyFee.getMonths()), dataStyle, i == 0);
                    hasAnyData = true;
                }

                // Merge cells for month, debt, cut amount, and reason if multiple transactions
                if (transactions.size() > 1) {
                    mergeMonthlyFeeCells(sheet, monthStartRow, currentRowIdx - 1);
                }
            }
        }

        // If no monthly fees exist, create at least one row for the student
        if (!hasAnyData) {
            Row row = sheet.createRow(currentRowIdx++);
            fillBasicStudentData(row, contract, null, dataStyle);
            fillEmptyMonthlyData(row, dataStyle);
        }

        return currentRowIdx;
    }

    private Map<Months, String> createMonthNamesMap() {
        Map<Months, String> monthNames = new HashMap<>();
        monthNames.put(Months.SENTABR, "SENTABR");
        monthNames.put(Months.OKTABR, "OKTABR");
        monthNames.put(Months.NOYABR, "NOYABR");
        monthNames.put(Months.DEKABR, "DEKABR");
        monthNames.put(Months.YANVAR, "YANVAR");
        monthNames.put(Months.FEVRAL, "FEVRAL");
        monthNames.put(Months.MART, "MART");
        monthNames.put(Months.APREL, "APREL");
        monthNames.put(Months.MAY, "MAY");
        monthNames.put(Months.IYUN, "IYUN");
        return monthNames;
    }

//    private List<MonthlyFee> getAllMonthlyFees(StudentContract contract) {
//        List<MonthlyFee> monthlyFees = new ArrayList<>();
//        Months[] allMonths = {
//                Months.SENTABR, Months.OKTABR, Months.NOYABR, Months.DEKABR, Months.YANVAR,
//                Months.FEVRAL, Months.MART, Months.APREL, Months.MAY, Months.IYUN
//        };
//
//        for (Months month : allMonths) {
//            Optional<MonthlyFee> fee = monthlyFeeRepository
//                    .findByStudentContractIdAndMonthsAndAcademicYear(
//                            contract.getId(), month, contract.getAcademicYear());
//            monthlyFees.add(fee.orElse(null));
//        }
//
//        return monthlyFees;
//    }

    private List<MonthlyFee> getAllMonthlyFees(StudentContract contract) {
        List<MonthlyFee> monthlyFees = new ArrayList<>();
        Months[] allMonths = {
                Months.SENTABR, Months.OKTABR, Months.NOYABR, Months.DEKABR, Months.YANVAR,
                Months.FEVRAL, Months.MART, Months.APREL, Months.MAY, Months.IYUN
        };

        for (Months month : allMonths) {
            List<MonthlyFee> fees = monthlyFeeRepository
                    .findAllByStudentContractIdAndMonthsAndAcademicYear(
                            contract.getId(), month, contract.getAcademicYear());

            // Take the first one, or handle multiple records as needed
            monthlyFees.add(fees.isEmpty() ? null : fees.get(0));
        }

        return monthlyFees;
    }
    private List<Transaction> getTransactions(Long monthlyFeeId) {
        try {
            return transactionRepository.findAllByMonthlyFeeId(monthlyFeeId);
        } catch (Exception e) {
            log.warn("Error fetching transactions for monthly fee {}: {}", monthlyFeeId, e.getMessage());
            return Collections.emptyList();
        }
    }

    private void fillBasicStudentData(Row row, StudentContract contract, MonthlyFee monthlyFee, CellStyle dataStyle) {
        int colIdx = 0;

        String tariffName;
        if (monthlyFee != null && monthlyFee.getTariffName() != null) {
            tariffName = monthlyFee.getTariffName();
        } else if (contract.getTariff() != null) {
            tariffName = contract.getTariff().getName();
        } else {
            tariffName = "N/A";
        }
        double amount;
        if (monthlyFee != null && monthlyFee.getTotalFee() != null) {
            amount = monthlyFee.getTotalFee();
        } else {
            amount = safeDouble(contract.getAmount() != null ? contract.getAmount() : 0.0);
        }

        // Fill first 26 columns with student data
        createStyledCell(row, colIdx++, contract.getUniqueId(), dataStyle);
        createStyledCell(row, colIdx++, formatDate(contract.getContractedDate()), dataStyle);
        createStyledCell(row, colIdx++, safeString(contract.getAcademicYear()), dataStyle);
        createStyledCell(row, colIdx++, safeString(contract.getStudentFullName()), dataStyle);
        createStyledCell(row, colIdx++, formatDate(contract.getBirthDate()), dataStyle);
        createStyledCell(row, colIdx++, safeEnum(contract.getGender()), dataStyle);
        createStyledCell(row, colIdx++, formatGrade(contract), dataStyle);
        createStyledCell(row, colIdx++, safeEnum(contract.getLanguage()), dataStyle);
        createStyledCell(row, colIdx++, safeString(contract.getGuardianFullName()), dataStyle);
        createStyledCell(row, colIdx++, safeEnum(contract.getGuardianType()), dataStyle);
        createStyledCell(row, colIdx++, safeString(contract.getPhone1()), dataStyle);
        createStyledCell(row, colIdx++, safeString(contract.getPhone2()), dataStyle);
        createStyledCell(row, colIdx++, safeString(contract.getPassportId()), dataStyle);
        createStyledCell(row, colIdx++, safeString(contract.getPassportIssuedBy()), dataStyle);
        createStyledCell(row, colIdx++, safeString(contract.getAddress()), dataStyle);
        createStyledCell(row, colIdx++, tariffName, dataStyle);
        createStyledCell(row, colIdx++, safeString(contract.getGuardianJSHSHIR()), dataStyle);
        createStyledCell(row, colIdx++, safeString(contract.getComment()), dataStyle);
        createStyledCell(row, colIdx++, formatDate(contract.getContractStartDate()), dataStyle);
        createStyledCell(row, colIdx++, formatDate(contract.getContractEndDate()), dataStyle);
        createStyledCell(row, colIdx++, contract.getStatus() ? "Active" : "Passive", dataStyle);
        createStyledCell(row, colIdx++, formatDate(contract.getInactiveDate()), dataStyle);
        createStyledCell(row, colIdx++, safeEnum(contract.getClientType()), dataStyle);
        createStyledCell(row, colIdx++, safeString(contract.getBCN()), dataStyle);
        createStyledCell(row, colIdx++, safeCompanyId(contract.getCompany()), dataStyle);
        createStyledCell(row, colIdx++, amount, dataStyle);
    }

    private void fillMonthlyFeeDataWithMerging(Row row, MonthlyFee monthlyFee, Transaction transaction,
                                               String monthName, CellStyle dataStyle, boolean isFirstRow) {
        int colIdx = 26; // Start after basic student data

        // Only fill these cells in the first row, leave empty in subsequent rows for merging
        if (isFirstRow) {
            createStyledCell(row, colIdx++, monthName, dataStyle);
            createStyledCell(row, colIdx++, transaction != null ? transaction.getAmount() : 0.0, dataStyle);
            createStyledCell(row, colIdx++, safeDouble(monthlyFee.getRemainingBalance()), dataStyle);
            createStyledCell(row, colIdx++, safeDouble(monthlyFee.getCutAmount()), dataStyle);
            createStyledCell(row, colIdx++, safeString(monthlyFee.getReason()), dataStyle);
        } else {
            // Skip month, debt, cut amount, and reason columns - they'll be merged
            createStyledCell(row, colIdx++, "", dataStyle); // Empty month
            createStyledCell(row, colIdx++, transaction != null ? transaction.getAmount() : 0.0, dataStyle);
            createStyledCell(row, colIdx++, "", dataStyle); // Empty debt
            createStyledCell(row, colIdx++, "", dataStyle); // Empty cut amount
            createStyledCell(row, colIdx++, "", dataStyle); // Empty reason
        }

        // Always show payment type and transaction date for each transaction
        createStyledCell(row, colIdx++, transaction != null ? safeEnum(transaction.getPaymentType()) : "N/A", dataStyle);
        createStyledCell(row, colIdx++, transaction != null ? formatDateTime(transaction.getCreatedAt()) : "N/A", dataStyle);
    }

    private void mergeMonthlyFeeCells(Sheet sheet, int startRow, int endRow) {
        try {
            // Merge month name (column 26)
            sheet.addMergedRegion(new CellRangeAddress(startRow, endRow, 26, 26));

            // Merge remaining balance/debt (column 28)
            sheet.addMergedRegion(new CellRangeAddress(startRow, endRow, 28, 28));

            // Merge cut amount (column 29)
            sheet.addMergedRegion(new CellRangeAddress(startRow, endRow, 29, 29));

            // Merge reason (column 30)
            sheet.addMergedRegion(new CellRangeAddress(startRow, endRow, 30, 30));

        } catch (Exception e) {
            log.warn("Error merging cells for rows {}-{}: {}", startRow, endRow, e.getMessage());
        }
    }

    private void fillMonthlyFeeData(Row row, MonthlyFee monthlyFee, String monthName, CellStyle dataStyle) {
        int colIdx = 26; // Start after basic student data

        createStyledCell(row, colIdx++, monthName, dataStyle);
        createStyledCell(row, colIdx++, null != null ? ((Transaction) null).getAmount() : 0.0, dataStyle);
        createStyledCell(row, colIdx++, safeDouble(monthlyFee.getRemainingBalance()), dataStyle);
        createStyledCell(row, colIdx++, safeDouble(monthlyFee.getCutAmount()), dataStyle);
        createStyledCell(row, colIdx++, safeString(monthlyFee.getReason()), dataStyle);
        createStyledCell(row, colIdx++, null != null ? safeEnum(((Transaction) null).getPaymentType()) : "N/A", dataStyle);
        createStyledCell(row, colIdx++, null != null ? formatDateTime(((Transaction) null).getCreatedAt()) : "N/A", dataStyle);
    }

    private void fillEmptyMonthlyData(Row row, CellStyle dataStyle) {
        int colIdx = 26;
        createStyledCell(row, colIdx++, "N/A", dataStyle);
        createStyledCell(row, colIdx++, 0.0, dataStyle);
        createStyledCell(row, colIdx++, 0.0, dataStyle);
        createStyledCell(row, colIdx++, 0.0, dataStyle);
        createStyledCell(row, colIdx++, "N/A", dataStyle);
        createStyledCell(row, colIdx++, "N/A", dataStyle);
        createStyledCell(row, colIdx++, "N/A", dataStyle);
    }

    private void autoSizeColumns(Sheet sheet, int columnCount) {
        for (int i = 0; i < columnCount; i++) {
            boolean hasData = false;

            // Check if column has any non-null cells
            for (Row row : sheet) {
                Cell cell = row.getCell(i);
                if (cell != null && cell.getCellType() != CellType.BLANK) {
                    hasData = true;
                    break;
                }
            }

            if (!hasData) continue;

            if (isMergedColumn(sheet, i)) {
                // Set a fixed width for merged columns
                sheet.setColumnWidth(i, 8000); // ~30 characters wide
            } else {
                try {
                    sheet.autoSizeColumn(i);
                } catch (Exception e) {
                    log.warn("Could not auto-size column {}: {}", i, e.getMessage());
                }
            }
        }
    }

    // Utility method to detect if any cell in the column is part of a merged region
    private boolean isMergedColumn(Sheet sheet, int columnIndex) {
        for (int i = 0; i < sheet.getNumMergedRegions(); i++) {
            CellRangeAddress region = sheet.getMergedRegion(i);
            if (region.getFirstColumn() <= columnIndex && region.getLastColumn() >= columnIndex) {
                return true;
            }
        }
        return false;
    }


    // Helper methods for safe data extraction
    private String safeString(String value) {
        return value != null ? value : "N/A";
    }

    private String safeEnum(Enum<?> enumValue) {
        return enumValue != null ? enumValue.name() : "N/A";
    }

    private String formatDate(LocalDate date) {
        return date != null ? date.toString() : "N/A";
    }

    private String formatDateTime(LocalDateTime dateTime) {
        return dateTime != null
                ? dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd | HH:mm"))
                : "N/A";
    }

    private String formatGrade(StudentContract contract) {
        if (contract.getGrade() != null && contract.getStGrade() != null) {
            return contract.getGrade().name().replace("GRADE_", "") + "-" + contract.getStGrade().name();
        }
        return "N/A";
    }

    private String safeTariffName(StudentTariff tariff) {
        return tariff != null ? tariff.getName() : "N/A";
    }

    private Object safeCompanyId(Company company) {
        return company != null ? company.getId() : 1;
    }

    private Double safeDouble(Double value) {
        return value != null ? value : 0.0;
    }

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


    private void setBorders(CellStyle style) {
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
    }

    @Override
    public ApiResponse getAllStudentContractsList(StudentGrade grade) {
        List<StudentContract> contractList = this.studentContractRepository.findByGrade(grade);
        List<StudentContractDto> studentContractDtos = this.studentContractMapper.dtoList(contractList);
        return ApiResponse.builder()
                .status(HttpStatus.OK)
                .message(RestConstants.SUCCESS)
                .data(studentContractDtos)
                .build();
    }


    private String getCellValue(Cell cell) {
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> {
                double numericValue = cell.getNumericCellValue();
                yield (numericValue == (long) numericValue) ? String.valueOf((long) numericValue) : String.valueOf(numericValue);
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> {
                try {
                    yield String.valueOf(cell.getNumericCellValue());
                } catch (Exception e) {
                    yield cell.getCellFormula();
                }
            }
            default -> "";
        };
    }


    private LocalDate parseLocalDate(Cell cell) {
        if (cell == null) return null;
        try {
            if (cell.getCellType() == CellType.NUMERIC) {
                return cell.getLocalDateTimeCellValue().toLocalDate();
            } else if (cell.getCellType() == CellType.STRING) {
                String dateString = cell.getStringCellValue().trim();
                DateTimeFormatter[] formatters = {
                        DateTimeFormatter.ofPattern("yyyy-MM-dd"),
                        DateTimeFormatter.ofPattern("dd/MM/yyyy")
                };
                for (DateTimeFormatter formatter : formatters) {
                    try {
                        return LocalDate.parse(dateString, formatter);
                    } catch (Exception ignored) {
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Invalid date format: {}", cell);
        }
        return null;
    }


    @Override
    public ApiResponse getData(String academicYear, Boolean status) {
        List<GetStudentsByGender> allByGender = this.studentContractRepository.findAllByGender(academicYear, status);
        List<StudentsByGender> students = new ArrayList<>();

        for (GetStudentsByGender studentsByGender : allByGender) {
            students.add(new StudentsByGender(
                    studentsByGender.getGender(),
                    studentsByGender.getCount()));
        }
        return ApiResponse.builder()
                .status(HttpStatus.OK)
                .message(RestConstants.SUCCESS)
                .data(students)
                .build();
    }

    @Override
    public ApiResponse getDataWithTariff(String academicYear) {
        List<GetAmountAndCount> allByTariff = this.studentContractRepository.findAllByTariff(academicYear);
        List<StudentsByTariff> students = new ArrayList<>();
        for (GetAmountAndCount getAmountAndCount : allByTariff) {
            students.add(new StudentsByTariff(
                    getAmountAndCount.getTariffName(),
                    getAmountAndCount.getAmount(),
                    getAmountAndCount.getCount()));
        }
        return ApiResponse.builder()
                .status(HttpStatus.OK)
                .message(RestConstants.SUCCESS)
                .data(students)
                .build();
    }

    //!!!depricated
    @Transactional
    @Override
    public ApiResponse payForStudent(Long studentContractId, StudentContractDto.StudentPaymentDto dto) {

        StudentContract studentContract = this.studentContractRepository.findById(studentContractId)
                .orElseThrow(() -> new ResourceNotFoundException(RestConstants.STUDENT_NOT_FOUND));

        Company company = this.companyRepository.findOne()
                .orElseThrow(() -> new ResourceNotFoundException(RestConstants.COMPANY_NOT_FOUND));

        StudentTariff tariff = studentContract.getTariff();
        MonthlyFee fee;

        fee = this.monthlyFeeRepository.findByStudentContractIdAndMonths(studentContractId, dto.getMonth())
                .orElse(null);

        Category category = this.categoryRepository.findByName(STUDENT_INCOME)// o'quvchidan_kirim
                .orElseThrow(() -> new ResourceNotFoundException(RestConstants.CATEGORY_NOT_FOUND));

        if (dto.getAmountPaid() == null || dto.getAmountPaid() < 0) {
            return ApiResponse.builder()
                    .status(HttpStatus.BAD_REQUEST)
                    .message(RestConstants.INVALID_AMOUNT)
                    .build();
        }
//        company.setTotalIncome(company.getTotalIncome() + dto.getAmountPaid());
        company = this.companyRepository.save(company);


        if (fee != null) {
            fee.setStudentContract(studentContract);
            fee.setCompany(company);
            fee.setMonths(dto.getMonth());
            fee.setCutAmount(fee.getCutAmount());
            fee.setRemainingBalance(fee.getRemainingBalance() - dto.getAmountPaid());
            if (fee.getRemainingBalance() - dto.getAmountPaid() == 0 || fee.getRemainingBalance() - dto.getAmountPaid() < 0) {
                fee.setStatus(PaymentStatus.FULLY_PAID);
            } else {
                fee.setStatus(PaymentStatus.PARTIALLY_PAID);
            }
            fee.setAmountPaid(fee.getAmountPaid() + dto.getAmountPaid());
            fee.setTotalFee(fee.getTotalFee());
            fee.setCategory(category);
            this.monthlyFeeRepository.save(fee);

        } else {

            fee = new MonthlyFee();

            fee.setStudentContract(studentContract);
            fee.setCompany(company);
            fee.setCategory(category);
            fee.setCutAmount(null); // Keep existing cut info
            fee.setAmountPaid(dto.getAmountPaid()); // Add to previous
            fee.setRemainingBalance(tariff.getAmount() - dto.getAmountPaid());
            fee.setReason(null);
            fee.setTotalFee(tariff.getAmount());
            fee.setTariffName(tariff.getName());
            fee.setMonths(dto.getMonth());
            fee = this.monthlyFeeRepository.save(fee);

            if (Objects.equals(fee.getTotalFee(), dto.getAmountPaid())) {
                fee.setStatus(PaymentStatus.FULLY_PAID);
            } else if (dto.getAmountPaid() == null || dto.getAmountPaid() == 0) {
                fee.setStatus(PaymentStatus.UNPAID);
            } else {
                fee.setStatus(PaymentStatus.PARTIALLY_PAID);
            }
            fee = this.monthlyFeeRepository.save(fee);
        }

        Transaction transaction = new Transaction();
        transaction.setTransactionType(TransactionType.INCOME);
        transaction.setPaymentType(dto.getPaymentType());
        transaction.setDescription(dto.getComment());
        transaction.setAmount(dto.getAmountPaid());
        transaction.setMonthlyFee(fee);
        transaction.setCompany(company);
        this.transactionRepository.save(transaction);

        return ApiResponse.builder()
                .status(HttpStatus.OK)
                .message(RestConstants.SUCCESSFULLY_PAID)
                .data(this.studentContractMapper.toStudentContractDtoResponse(studentContract))
                .build();
    }

    @Override
    public ApiResponse getPaymentInfoByContractId(Long contractId, String academicYear) {

        List<MonthlyPaymentInfo> projections = monthlyFeeRepository.getMonthlyPaymentInfosByContractId(contractId, academicYear);
        List<MonthlyPaymentInfoDto> list = projections
                .stream()
                .map(info ->
                        new MonthlyPaymentInfoDto(
                                info.getMonths(),
                                info.getTariffName(),
                                info.getTotalFee(),
                                info.getAmountPaid(),
                                info.getRemainingBalance()))
                .toList();

        return ApiResponse.builder()
                .status(HttpStatus.OK)
                .message(RestConstants.SUCCESS)
                .data(list)
                .build();
    }

    @Override
    public List<MonthlyPaymentInfoDto> getAllMonthsPaymentInfo(Long contractId) {
        List<MonthlyPaymentInfo> actualPayments = monthlyFeeRepository.getMonthlyPaymentInfosByContractId(contractId, "");

        Map<Months, MonthlyPaymentInfo> monthToPaymentMap = actualPayments
                .stream()
                .collect(Collectors.toMap(
                        MonthlyPaymentInfo::getMonths,
                        Function.identity(),
                        (a, b) -> a));

        List<MonthlyPaymentInfoDto> result = new ArrayList<>();
        for (Months month : Months.values()) {
            MonthlyPaymentInfo info = monthToPaymentMap.get(month);
            if (info != null) {
                result.add(new MonthlyPaymentInfoDto(
                        month,
                        info.getTariffName(),
                        info.getAmountPaid(),
                        info.getTotalFee(),
                        info.getRemainingBalance()
                ));
            } else {
                result.add(new MonthlyPaymentInfoDto(
                        month,
                        "-",
                        0.0,
                        0.0,
                        0.0
                ));
            }
        }
        return result;
    }

    @Transactional
    @Override
    public ApiResponse payStudentTuition(Long studentContractId, StudentContractDto.StudentPaymentDto dto) {
        // Validate and retrieve entities
        StudentContract studentContract = studentContractRepository.findById(studentContractId)
                .orElseThrow(() -> new ResourceNotFoundException(RestConstants.STUDENT_NOT_FOUND));

        String academicYear = studentContract.getAcademicYear(); // Get from StudentContract

        Company company = companyRepository.findOne()
                .orElseThrow(() -> new ResourceNotFoundException(RestConstants.COMPANY_NOT_FOUND));

        StudentTariff tariff = studentContract.getTariff();
        double amountToDistribute = dto.getAmountPaid();

        if (amountToDistribute <= 0) {
            return ApiResponse.builder()
                    .status(HttpStatus.BAD_REQUEST)
                    .message(RestConstants.INVALID_AMOUNT)
                    .build();
        }

        // Handle balances (with null safety)
        double cashBalance = company.getCashBalance() != null ? company.getCashBalance() : 0.0;
        double cardBalance = company.getCardBalance() != null ? company.getCardBalance() : 0.0;

        // Update company balance based on payment type
        if (dto.getPaymentType() == PaymentType.NAQD) {
            cashBalance += dto.getAmountPaid();
            company.setCashBalance(cashBalance);
        } else if (isCardPayment(dto.getPaymentType())) {
            cardBalance += dto.getAmountPaid();
            company.setCardBalance(cardBalance);
        }
        company = companyRepository.save(company);

        Category category = categoryRepository.findByName(STUDENT_INCOME)
                .orElseThrow(() -> new ResourceNotFoundException(RestConstants.CATEGORY_NOT_FOUND));

        // Parse current month from DTO
        Months currentMonth;
        try {
            currentMonth = Months.valueOf(dto.getMonth().name());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Noto'g'ri oy: " + dto.getMonth());
        }

        List<MonthlyFee> updatedFees;
        List<Transaction> transactionList = null;

        if (tariff.getTariffStatus() == TariffStatus.YEARLY) {
            // For yearly tariff, distribute across all academic year months
            updatedFees = applyYearlyPaymentDistribution(
                    studentContract, company, category, academicYear, amountToDistribute, dto);
        } else if (tariff.getTariffStatus() == TariffStatus.QUARTERLY) {
            // For quarterly tariff, distribute across quarter months
            updatedFees = applyQuarterlyPaymentDistribution(
                    studentContract, company, category, academicYear, amountToDistribute, dto);
        } else {
            Optional<MonthlyFee> existingFeeOpt = monthlyFeeRepository
                    .findByStudentContractAndMonthsAndAcademicYear(studentContract.getId(), currentMonth, academicYear);

            if (existingFeeOpt.isPresent()) {
                MonthlyFee existingFee = existingFeeOpt.get();
                double remainingDebt = existingFee.getTotalFee() - existingFee.getAmountPaid();

                if (remainingDebt <= 0) {
                    return ApiResponse.builder()
                            .status(HttpStatus.BAD_REQUEST)
                            .message(currentMonth.name() + " oyi uchun to'lov allaqachon to'liq amalga oshirilgan. Boshqa oyni tanlang.")
                            .build();
                }
            }
            // For monthly tariff, apply to selected month only (existing logic)
            PaymentDistributionResult result = applyNewPaymentDistribution(
                    studentContract, company, category, currentMonth, academicYear, amountToDistribute, dto);
            updatedFees = result.getUpdatedFees();
            transactionList = result.getNewTransactions();
        }

        // Save all updated fees
        List<MonthlyFee> feeList = monthlyFeeRepository.saveAll(updatedFees);
        for (MonthlyFee fee : feeList) {
            try {
                this.studentMonthlyFeeGoogleSheet.initializeSheet();
                this.studentMonthlyFeeGoogleSheet.recordMonthlyFee(fee);
            } catch (Exception e) {
                log.warn("Google Sheet ga yozishda xatolik: {}", e.getMessage());
            }
        }
        for (Transaction transaction : transactionList) {
            try {
                this.studentTransactionGoogleSheet.initializeSheet();
                this.studentTransactionGoogleSheet.recordStudentTransactions(transaction);
            } catch (Exception e) {
                log.warn("Google Sheet ga yozishda xatolik: {}", e.getMessage());
            }
        }


        // Send SMS notification
        String smsMessage = String.format(
                "\"IFTIXOR\" xususiy maktabidagi Farzandingiz %s hisobiga %.0f so'm to'lov qabul qilindi.",
                studentContract.getStudentFullName(),
                dto.getAmountPaid()
        );

        try {
            if (studentContract.getPhone1() != null && !studentContract.getPhone1().isEmpty()) {
                smsServiceImpl.sendAfterPaymentDone(studentContract.getPhone1(), smsMessage);
            }
        } catch (Exception e) {
            log.warn("SMS yuborishda xatolik: {}", e.getMessage());
        }

        return ApiResponse.builder()
                .status(HttpStatus.OK)
                .message(RestConstants.SUCCESSFULLY_PAID)
                .data(studentContractMapper.toStudentContractDtoResponse(studentContract))
                .meta(Map.of(
                        "createdAt", LocalDateTime.now().toString(),
                        "updatedAt", LocalDateTime.now().toString()
                ))
                .build();
    }


    private List<MonthlyFee> applyQuarterlyPaymentDistribution(
            StudentContract studentContract, Company company, Category category, String academicYear, double amountToDistribute,
            StudentContractDto.StudentPaymentDto dto) {

        // Define academic year months in order
        List<Months> academicYearMonths = Arrays.asList(
                Months.SENTABR, Months.OKTABR, Months.NOYABR, Months.DEKABR,
                Months.YANVAR, Months.FEVRAL, Months.MART, Months.APREL,
                Months.MAY, Months.IYUN
        );

        List<MonthlyFee> updatedFees = new ArrayList<>();
        double remainingAmount = amountToDistribute;

        // Get or create monthly fees for all academic year months
        for (Months month : academicYearMonths) {
            if (remainingAmount <= 0) break;

            MonthlyFee monthlyFee = getOrCreateMonthlyFee(studentContract, month, academicYear, studentContract.getTariff());

            double remainingDebt = monthlyFee.getTotalFee() - monthlyFee.getAmountPaid();
            if (remainingDebt <= 0) {
                continue;
            }

            if (remainingDebt > 0) {
                double paymentForThisMonth = Math.min(remainingAmount, remainingDebt);

                // Update monthly fee
                monthlyFee.setAmountPaid(monthlyFee.getAmountPaid() + paymentForThisMonth);
                monthlyFee.setRemainingBalance(monthlyFee.getTotalFee() - monthlyFee.getAmountPaid());
                monthlyFee.setCategory(category);
                monthlyFee.setCompany(company);
                monthlyFee.setTariffName(studentContract.getTariff().getName());

                if (monthlyFee.getRemainingBalance() <= 0) {
                    monthlyFee.setStatus(PaymentStatus.FULLY_PAID);
                } else {
                    monthlyFee.setStatus(PaymentStatus.PARTIALLY_PAID);
                }

                // Create transaction for this month
                createTransactionForPayment(company, category, monthlyFee, paymentForThisMonth, dto);

                updatedFees.add(monthlyFee);
                remainingAmount -= paymentForThisMonth;
            }
        }

        return updatedFees;
    }

    /**
     * Distributes yearly payment across all academic year months (September to June)
     */
    private List<MonthlyFee> applyYearlyPaymentDistribution(
            StudentContract studentContract, Company company, Category category, String academicYear, double amountToDistribute,
            StudentContractDto.StudentPaymentDto dto) {

        // Define academic year months in order
        List<Months> academicYearMonths = Arrays.asList(
                Months.SENTABR, Months.OKTABR, Months.NOYABR, Months.DEKABR,
                Months.YANVAR, Months.FEVRAL, Months.MART, Months.APREL,
                Months.MAY, Months.IYUN
        );

        List<MonthlyFee> updatedFees = new ArrayList<>();
        double remainingAmount = amountToDistribute;

        // Get or create monthly fees for all academic year months
        for (Months month : academicYearMonths) {
            if (remainingAmount <= 0) break;

            MonthlyFee monthlyFee = getOrCreateMonthlyFee(studentContract, month, academicYear, studentContract.getTariff());

            // Calculate monthly tariff amount (yearly amount / 10 months)
            double remainingDebt = monthlyFee.getTotalFee() - monthlyFee.getAmountPaid();

            if (remainingDebt <= 0) {
                continue;
            }
            if (remainingDebt > 0) {
                double paymentForThisMonth = Math.min(remainingAmount, remainingDebt);

                // Update monthly fee
                monthlyFee.setAmountPaid(monthlyFee.getAmountPaid() + paymentForThisMonth);
                monthlyFee.setRemainingBalance(monthlyFee.getTotalFee() - monthlyFee.getAmountPaid());
                monthlyFee.setCompany(company);
                monthlyFee.setTariffName(studentContract.getTariff().getName());
                monthlyFee.setCategory(category);

                if (monthlyFee.getRemainingBalance() <= 0) {
                    monthlyFee.setStatus(PaymentStatus.FULLY_PAID);
                } else {
                    monthlyFee.setStatus(PaymentStatus.PARTIALLY_PAID);
                }

                // Create transaction for this month
                createTransactionForPayment(company, category, monthlyFee, paymentForThisMonth, dto);

                updatedFees.add(monthlyFee);
                remainingAmount -= paymentForThisMonth;
            }
        }

        return updatedFees;
    }

    /**
     * Helper method to get or create monthly fee
     */
    private MonthlyFee getOrCreateMonthlyFee(StudentContract studentContract, Months month, String academicYear, StudentTariff studentTariff) {
        Optional<MonthlyFee> existingFee = monthlyFeeRepository
                .findByStudentContractAndMonthsAndAcademicYear(studentContract.getId(), month, academicYear);
        TariffStatus tariffStatus = studentTariff.getTariffStatus();

        if (existingFee.isPresent()) {
            MonthlyFee fee = existingFee.get();
            fee.setTotalFee(fee.getTotalFee());
            return fee;
        }
        double amount = 0.0;
        if (tariffStatus == TariffStatus.YEARLY) {
            amount = studentTariff.getAmount() / 10.0; // Yearly divided by 10 months
        } else if (tariffStatus == TariffStatus.QUARTERLY) {
            amount = studentTariff.getAmount() / 2.5; // Quarterly divided by 4 months
        } else if (tariffStatus == TariffStatus.MONTHLY) {
            amount = studentTariff.getAmount(); // Monthly tariff
        }

        // Create new monthly fee
        MonthlyFee newFee = new MonthlyFee();
        newFee.setStudentContract(studentContract);
        newFee.setMonths(month);
        newFee.setTotalFee(amount);
        newFee.setAmountPaid(0.0);

        // Calculate expected amount based on tariff status
        double expectedAmount = studentContract.getTariff().getAmount() / 10.0; // Yearly divided by 10 months

        newFee.setRemainingBalance(expectedAmount);
        newFee.setStatus(PaymentStatus.UNPAID);

        return monthlyFeeRepository.save(newFee);
    }

    /**
     * Helper method to create transaction for payment
     */
    private void createTransactionForPayment(Company company, Category category, MonthlyFee monthlyFee,
                                             double amount, StudentContractDto.StudentPaymentDto dto) {
        if (amount <= 0) {
            return;
        }

        Transaction transaction = new Transaction();
        transaction.setCompany(company);
        transaction.setMonthlyFee(monthlyFee);
        transaction.setAmount(amount);
        transaction.setTransactionType(TransactionType.INCOME);
        transaction.setPaymentType(dto.getPaymentType());
        transaction.setDescription(dto.getComment());

        transaction = transactionRepository.save(transaction);
        try {
            this.studentTransactionGoogleSheet.initializeSheet();
            this.studentTransactionGoogleSheet.recordStudentTransactions(transaction);
        } catch (Exception e) {
            log.error("Xatolik: {}", e.getMessage());
        }
    }

    @Override
    public ApiResponse terminateContract(Long id) {
        StudentContract contract = this.studentContractRepository.findByIdAndDeletedFalseAndStatusTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException(RestConstants.STUDENT_NOT_FOUND));
        contract.setStatus(false);
        contract.setInactiveDate(LocalDate.now());
        this.studentContractRepository.save(contract);
        return ApiResponse.builder()
                .status(HttpStatus.OK)
                .message(RestConstants.CONTRACT_TERMINATED)
                .build();
    }

    @Override
    public ApiResponse addReminder(Long studentContractId, ReminderDto.ReminderCreateAndUpdateDto reminderDto) {
        StudentContract studentContract = this.studentContractRepository.findByIdAndDeletedFalse(studentContractId)
                .orElseThrow(() -> new ResourceNotFoundException(RestConstants.STUDENT_NOT_FOUND));
        Reminder reminder = this.reminderMapper.toEntity(reminderDto);
        reminder.setStudentContract(studentContract);
        reminder = this.reminderRepository.save(reminder);
        ReminderDto mapperDto = this.reminderMapper.toDto(reminder);
        return ApiResponse.builder()
                .status(HttpStatus.CREATED)
                .message(RestConstants.SUCCESSFULLY_SAVED)
                .data(mapperDto)
                .build();
    }

    @Override
    public ApiResponse getStudentReminders(Long studentContractId) {
        StudentContract studentContract = this.studentContractRepository.findByIdAndDeletedFalse(studentContractId)
                .orElseThrow(() -> new ResourceNotFoundException(RestConstants.STUDENT_NOT_FOUND));
        List<Reminder> reminders = this.reminderRepository.findByStudentContractId(studentContract.getId());
        return ApiResponse.builder()
                .status(HttpStatus.OK)
                .message(RestConstants.SUCCESS)
                .data(this.reminderMapper.toDto(reminders))
                .build();
    }

    @Override
    public ApiResponse getInfo(String academicYear, Boolean status, LocalDate from, LocalDate to) {
        LocalDateTime start = from != null ? from.atStartOfDay() : null;
        LocalDateTime end = to != null ? to.atTime(LocalTime.MAX) : null;
        List<GetStudentsByGrade> gradeList = this.studentContractRepository.findAllByGrade(academicYear, status, start, end);
        if (gradeList.isEmpty()) {
            return ApiResponse.builder()
                    .status(HttpStatus.OK)
                    .data(new ArrayList<>())
                    .build();
        }

        List<GetStudentsByGradeDto> list = new ArrayList<>();
        for (GetStudentsByGrade byGrade : gradeList) {
            list.add(new GetStudentsByGradeDto(
                    byGrade.getStudentGrade(),
                    byGrade.getGrade(),
                    byGrade.getMaleCount(),
                    byGrade.getFemaleCount(),
                    byGrade.getOldCount(),
                    byGrade.getNewCount(),
                    byGrade.getStudyLanguage()

            ));
        }
        return ApiResponse.builder()
                .status(HttpStatus.OK)
                .message(RestConstants.SUCCESS)
                .data(list)
                .build();
    }

    @Transactional
    @Override
    public ApiResponse transferStudents(

            List<Long> studentIds,
            StudentGrade studentGrade,
            Grade grade,
            String academicYear,
            Long tariffId,
            LocalDate contractStartDate,
            LocalDate contractEndDate) {
        List<StudentContract> contractList = this.studentContractRepository.findAllByIdsAndDeletedFalse(studentIds);
        for (StudentContract oldStudent : contractList) {

            oldStudent.setStatus(false);
            oldStudent.setInactiveDate(LocalDate.now());
            oldStudent.setUniqueId(oldStudent.getUniqueId());
            this.studentContractRepository.save(oldStudent);

            if (studentIds == null
                    || studentGrade == null
                    || grade == null
                    || academicYear == null
                    || tariffId == null
                    || contractStartDate == null
                    || contractEndDate == null) {
                return ApiResponse.builder()
                        .status(HttpStatus.BAD_REQUEST)
                        .message(RestConstants.MISSING_DATA)
                        .build();
            }
            List<StudentContract> studentContractList = this.studentContractRepository.findAllByAcademicYear(academicYear);
            long count = studentContractList.size();

            StudentTariff studentTariff = this.studentTariffRepository.findById(tariffId)
                    .orElseThrow(() -> new ResourceNotFoundException(RestConstants.TARIFF_NOT_FOUND));
            Optional<StudentContract> optional = this.studentContractRepository.findByBCN(oldStudent.getBCN(), academicYear);

            if (optional.isPresent() && optional.get().getStatus() == true) {
                return ApiResponse.builder()
                        .status(HttpStatus.BAD_REQUEST)
                        .message("Bu o'quvchi allaqachon aktiv!")
                        .build();
            }
            StudentContract newContract = new StudentContract();
            newContract.setContractedDate(LocalDate.now());
            newContract.setStudentFullName(oldStudent.getStudentFullName());
            newContract.setBirthDate(oldStudent.getBirthDate());
            newContract.setGender(oldStudent.getGender());
            newContract.setGrade(studentGrade);
            newContract.setStGrade(grade);
            newContract.setLanguage(oldStudent.getLanguage());
            newContract.setGuardianFullName(oldStudent.getGuardianFullName());
            newContract.setGuardianType(oldStudent.getGuardianType());
            newContract.setPassportId(oldStudent.getPassportId());
            newContract.setGuardianJSHSHIR(oldStudent.getGuardianJSHSHIR());
            newContract.setPassportIssuedBy(oldStudent.getPassportIssuedBy());
            newContract.setAcademicYear(academicYear);
            newContract.setPhone1(oldStudent.getPhone1());
            newContract.setPhone2(oldStudent.getPhone2());
            newContract.setAddress(oldStudent.getAddress());
            newContract.setTariff(studentTariff);
            newContract.setAmount(studentTariff.getAmount());
            newContract.setComment(oldStudent.getComment());
            newContract.setAddress(oldStudent.getAddress());
            newContract.setContractStartDate(contractStartDate);
            newContract.setContractEndDate(contractEndDate);
            newContract.setClientType(ClientType.OLD);
            newContract.setStatus(true);
            newContract.setInactiveDate(null);
            newContract.setBCN(oldStudent.getBCN());
            newContract.setCompany(oldStudent.getCompany());
            newContract.setUniqueId(String.valueOf(count + 1));
            this.studentContractRepository.save(newContract);

        }

        return ApiResponse.builder()
                .status(HttpStatus.OK)
                .message(RestConstants.CONTRACT_UPDATED)
                .build();
    }

    public String generateUpdatedDescription(Months month, String comment) {
        return String.format("%s OY UCHUN TO'LOV TRANSAKSIYASI. IZOH: %s",
                month.name(), comment != null
                        ? comment
                        : "");
    }

    private Months[] getAcademicMonths() {
        return new Months[]{
                Months.SENTABR, Months.OKTABR, Months.NOYABR, Months.DEKABR,
                Months.YANVAR, Months.FEVRAL, Months.MART, Months.APREL,
                Months.MAY, Months.IYUN
        };
    }

    private boolean isCardPayment(PaymentType paymentType) {
        return paymentType == PaymentType.BANK ||
                paymentType == PaymentType.CLICK ||
                paymentType == PaymentType.PAYMEE ||
                paymentType == PaymentType.TERMINAL;
    }

    private PaymentDistributionResult applyNewPaymentDistribution(StudentContract studentContract, Company company,
                                                                  Category category, Months startMonth, String academicYear, Double amountToDistribute,
                                                                  StudentContractDto.StudentPaymentDto dto) {

        StudentTariff tariff = studentContract.getTariff();
        final Months[] ACADEMIC_MONTHS = getAcademicMonths();

        List<MonthlyFee> updatedFees = new ArrayList<>();
        List<Transaction> newTransactions = new ArrayList<>(); // Store new transactions


        double remainingAmount = amountToDistribute;
        int currentMonthIndex = findMonthIndex(ACADEMIC_MONTHS, startMonth);

        if (currentMonthIndex == -1) {
            currentMonthIndex = 0; // Start from September if invalid month
        }

        // Process current academic year first
        for (int i = currentMonthIndex; i < ACADEMIC_MONTHS.length && remainingAmount > 0; i++) {
            Months month = ACADEMIC_MONTHS[i];
            MonthlyFee fee = findOrCreateMonthlyFee(studentContract, company, category, tariff, month, academicYear);

            double amountToApply = Math.min(remainingAmount, fee.getRemainingBalance());
            applyPaymentToFee(fee, amountToApply);

            // Create a new transaction for this MonthlyFee
            Transaction newTransaction = new Transaction();
            newTransaction.setTransactionType(TransactionType.INCOME);
            newTransaction.setPaymentType(dto.getPaymentType());
            newTransaction.setAmount(amountToApply);
            newTransaction.setMonthlyFee(fee);
            newTransaction.setCompany(company);
            newTransaction.setDescription(generateUpdatedDescription(month, dto.getComment()));

            newTransactions.add(newTransaction);
            updatedFees.add(fee);
            remainingAmount -= amountToApply;
        }

        // If there's remaining amount, process next academic year
        if (remainingAmount > 0) {
            String nextAcademicYear = getNextAcademicYear(academicYear);
            for (Months month : ACADEMIC_MONTHS) {
                if (remainingAmount <= 0) break;

                MonthlyFee fee = findOrCreateMonthlyFee(studentContract, company, category, tariff, month, nextAcademicYear);

                double amountToApply = Math.min(remainingAmount, fee.getRemainingBalance());
                applyPaymentToFee(fee, amountToApply);

                // Create a new transaction for this MonthlyFee
                Transaction newTransaction = new Transaction();
                newTransaction.setTransactionType(TransactionType.INCOME);
                newTransaction.setPaymentType(dto.getPaymentType());
                newTransaction.setAmount(amountToApply);
                newTransaction.setMonthlyFee(fee);
                newTransaction.setCompany(company);
                newTransaction.setDescription(generateUpdatedDescription(month, dto.getComment()));

                newTransactions.add(newTransaction);
                updatedFees.add(fee);
                remainingAmount -= amountToApply;
            }
        }
        // Save new transactions
        newTransactions = transactionRepository.saveAll(newTransactions);

        return new PaymentDistributionResult(updatedFees, newTransactions);
    }


    @Transactional
    @Override
    public ApiResponse updateStudentTuition(Long transactionId, StudentContractDto.StudentPaymentDto dto) {
        // Find the original transaction
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found"));

        // Validate transaction type
        if (transaction.getTransactionType() != TransactionType.INCOME) {
            return ApiResponse.builder()
                    .status(HttpStatus.BAD_REQUEST)
                    .message("Only INCOME transactions can be updated")
                    .build();
        }

        MonthlyFee monthlyFee = transaction.getMonthlyFee();
        if (monthlyFee == null) {
            return ApiResponse.builder()
                    .status(HttpStatus.BAD_REQUEST)
                    .message("No monthly fee associated with the transaction")
                    .build();
        }

        StudentContract studentContract = monthlyFee.getStudentContract();
        if (studentContract == null) {
            return ApiResponse.builder()
                    .status(HttpStatus.BAD_REQUEST)
                    .message("Student contract not found")
                    .build();
        }

        Company company = companyRepository.findOne()
                .orElseThrow(() -> new ResourceNotFoundException("Company not found"));

        // Validate new amount
        if (dto.getAmountPaid() <= 0) {
            return ApiResponse.builder()
                    .status(HttpStatus.BAD_REQUEST)
                    .message("Invalid payment amount")
                    .build();
        }

        // Validate payment type
        if (!isValidPaymentType(dto.getPaymentType())) {
            return ApiResponse.builder()
                    .status(HttpStatus.BAD_REQUEST)
                    .message("Invalid payment type")
                    .build();
        }

        // Validate month
        Months targetMonth;
        try {
            targetMonth = Months.valueOf(dto.getMonth().name());
        } catch (IllegalArgumentException e) {
            return ApiResponse.builder()
                    .status(HttpStatus.BAD_REQUEST)
                    .message("Invalid month: " + dto.getMonth())
                    .build();
        }

        // Ensure the monthly fee's month matches the target month
        if (monthlyFee.getMonths() != targetMonth) {
            return ApiResponse.builder()
                    .status(HttpStatus.BAD_REQUEST)
                    .message("Transaction month does not match the provided month")
                    .build();
        }

        Double originalAmount = transaction.getAmount();
        Double newAmount = dto.getAmountPaid();
        Double amountDifference = newAmount - originalAmount;

        // Update company balance
        updateCompanyBalanceForPaymentUpdate(company, transaction.getPaymentType(),
                dto.getPaymentType(), originalAmount, newAmount);

        updateMonthlyFeeForPayment(monthlyFee, originalAmount, newAmount);

        // Update transaction
        transaction.setAmount(newAmount);
        transaction.setPaymentType(dto.getPaymentType());
        transaction.setDescription(dto.getComment());

        // Save changes (monthlyFee is already saved in updateMonthlyFeeForPayment if needed)
        monthlyFee = monthlyFeeRepository.save(monthlyFee);
        try {
            this.studentMonthlyFeeGoogleSheet.initializeSheet();
            this.studentMonthlyFeeGoogleSheet.updateMonthlyFee(monthlyFee);
        } catch (Exception e) {
            log.warn("Google Sheet ga yozishda xatolik yuz berdi: {}", e.getMessage());
        }

        companyRepository.save(company);
        transaction = transactionRepository.save(transaction);
        try {
            this.studentTransactionGoogleSheet.initializeSheet();
            this.studentTransactionGoogleSheet.updateStudentTransaction(transaction);
        } catch (Exception e) {
            log.warn("Google Sheet ga yozishda xatolik yuz berdi: {}", e.getMessage());
        }

        return ApiResponse.builder()
                .status(HttpStatus.OK)
                .message("Payment updated successfully")
                .data(studentContractMapper.toStudentContractDtoResponse(studentContract))
                .meta(Map.of(
                        "originalAmount", originalAmount,
                        "newAmount", newAmount,
                        "difference", amountDifference,
                        "updatedAt", LocalDateTime.now().toString()
                ))
                .build();
    }

    private boolean isValidPaymentType(PaymentType paymentType) {
        return paymentType == PaymentType.NAQD || isCardPayment(paymentType);
    }

    private void updateMonthlyFeeForPayment(MonthlyFee monthlyFee, Double originalAmount, Double newAmount) {
        StudentContract studentContract = monthlyFee.getStudentContract();

        // Step 1: Reverse all effects of the original payment
        if (originalAmount > monthlyFee.getTotalFee()) {
            // Original payment exceeded this month's fee, so we need to reverse distributed amounts
            reverseExcessDistribution(studentContract, monthlyFee.getMonths(), originalAmount - monthlyFee.getTotalFee());

            // Reset current month to the state before any payment
            monthlyFee.setAmountPaid(monthlyFee.getAmountPaid() - monthlyFee.getTotalFee());
            monthlyFee.setRemainingBalance(monthlyFee.getTotalFee() - monthlyFee.getAmountPaid());
        } else {
            // Original payment was within this month's limit
            monthlyFee.setAmountPaid(monthlyFee.getAmountPaid() - originalAmount);
            monthlyFee.setRemainingBalance(monthlyFee.getTotalFee() - monthlyFee.getAmountPaid());
        }

        // Step 2: Apply the new payment amount
        if (newAmount > monthlyFee.getTotalFee()) {
            // New payment exceeds current month's fee
            double excessAmount = newAmount - monthlyFee.getTotalFee();

            // Fully pay current month
            monthlyFee.setAmountPaid(monthlyFee.getTotalFee());
            monthlyFee.setRemainingBalance(0.0);
            monthlyFee.setStatus(PaymentStatus.FULLY_PAID);

            // Distribute excess to next months
            distributeExcessToNextMonths(studentContract, monthlyFee.getMonths(), excessAmount);
        } else {
            // New payment fits within current month
            monthlyFee.setAmountPaid(monthlyFee.getAmountPaid() + newAmount);
            monthlyFee.setRemainingBalance(monthlyFee.getTotalFee() - monthlyFee.getAmountPaid());

            // Update payment status
            if (monthlyFee.getAmountPaid() <= 0) {
                monthlyFee.setStatus(PaymentStatus.UNPAID);
            } else if (monthlyFee.getAmountPaid() < monthlyFee.getTotalFee()) {
                monthlyFee.setStatus(PaymentStatus.PARTIALLY_PAID);
            } else {
                monthlyFee.setStatus(PaymentStatus.FULLY_PAID);
            }
        }
    }

    // ADD this new method to reverse excess distribution:
    private void reverseExcessDistribution(StudentContract studentContract, Months currentMonth, double excessToReverse) {
        // Define academic year months in order
        List<Months> academicYearMonths = Arrays.asList(
                Months.SENTABR, Months.OKTABR, Months.NOYABR, Months.DEKABR,
                Months.YANVAR, Months.FEVRAL, Months.MART, Months.APREL,
                Months.MAY, Months.IYUN
        );

        String academicYear = studentContract.getAcademicYear();

        // Find the index of current month
        int currentIndex = academicYearMonths.indexOf(currentMonth);
        if (currentIndex == -1) return;

        double remainingToReverse = excessToReverse;

        // Start from the next month after current month and reverse in order
        for (int i = currentIndex + 1; i < academicYearMonths.size() && remainingToReverse > 0; i++) {
            Months nextMonth = academicYearMonths.get(i);

            Optional<MonthlyFee> nextFeeOpt = monthlyFeeRepository
                    .findByStudentContractAndMonthsAndAcademicYear(studentContract.getId(), nextMonth, academicYear);

            if (nextFeeOpt.isPresent()) {
                MonthlyFee nextFee = nextFeeOpt.get();
                double paidAmount = nextFee.getAmountPaid();

                if (paidAmount > 0) {
                    // Calculate how much to reverse from this month
                    double amountToReverse = Math.min(remainingToReverse, paidAmount);

                    nextFee.setAmountPaid(paidAmount - amountToReverse);
                    nextFee.setRemainingBalance(nextFee.getTotalFee() - nextFee.getAmountPaid());

                    // Update status
                    if (nextFee.getAmountPaid() <= 0) {
                        nextFee.setStatus(PaymentStatus.UNPAID);
                    } else if (nextFee.getAmountPaid() < nextFee.getTotalFee()) {
                        nextFee.setStatus(PaymentStatus.PARTIALLY_PAID);
                    } else {
                        nextFee.setStatus(PaymentStatus.FULLY_PAID);
                    }

                    monthlyFeeRepository.save(nextFee);
                    remainingToReverse -= amountToReverse;
                }
            }
        }
    }

    private void distributeExcessToNextMonths(StudentContract studentContract, Months currentMonth,
                                              double excessAmount) {
        // Define academic year months in order
        List<Months> academicYearMonths = Arrays.asList(
                Months.SENTABR, Months.OKTABR, Months.NOYABR, Months.DEKABR,
                Months.YANVAR, Months.FEVRAL, Months.MART, Months.APREL,
                Months.MAY, Months.IYUN
        );

        // Get academic year from student contract
        String academicYear = studentContract.getAcademicYear();

        // Find the index of current month
        int currentIndex = academicYearMonths.indexOf(currentMonth);
        if (currentIndex == -1) return; // Current month not found

        double remainingExcess = excessAmount;

        // Start from the next month after current month
        for (int i = currentIndex + 1; i < academicYearMonths.size() && remainingExcess > 0; i++) {
            Months nextMonth = academicYearMonths.get(i);

            Optional<MonthlyFee> nextFeeOpt = monthlyFeeRepository
                    .findByStudentContractAndMonthsAndAcademicYear(studentContract.getId(), nextMonth, academicYear);

            MonthlyFee nextFee;
            if (nextFeeOpt.isPresent()) {
                nextFee = nextFeeOpt.get();
            } else {
                // Create new monthly fee if doesn't exist
                nextFee = getOrCreateMonthlyFee(studentContract, nextMonth, academicYear, studentContract.getTariff());
            }

            double remainingDebt = nextFee.getTotalFee() - nextFee.getAmountPaid();

            if (remainingDebt > 0) {
                double paymentForThisMonth = Math.min(remainingExcess, remainingDebt);

                nextFee.setAmountPaid(nextFee.getAmountPaid() + paymentForThisMonth);
                nextFee.setRemainingBalance(nextFee.getTotalFee() - nextFee.getAmountPaid());

                // Update status
                if (nextFee.getRemainingBalance() <= 0) {
                    nextFee.setStatus(PaymentStatus.FULLY_PAID);
                } else {
                    nextFee.setStatus(PaymentStatus.PARTIALLY_PAID);
                }

                monthlyFeeRepository.save(nextFee);
                remainingExcess -= paymentForThisMonth;
            }
        }
    }

    public void updateCompanyBalanceForPaymentUpdate(Company company, PaymentType oldPaymentType,
                                                     PaymentType newPaymentType, Double oldAmount, Double newAmount) {
        double cashBalance = company.getCashBalance() != null ? company.getCashBalance() : 0.0;
        double cardBalance = company.getCardBalance() != null ? company.getCardBalance() : 0.0;

        // Reverse old payment type effect
        if (oldPaymentType == PaymentType.NAQD) {
            cashBalance -= oldAmount;
        } else if (isCardPayment(oldPaymentType)) {
            cardBalance -= oldAmount;
        }

        // Apply new payment type effect
        if (newPaymentType == PaymentType.NAQD) {
            cashBalance += newAmount;
        } else if (isCardPayment(newPaymentType)) {
            cardBalance += newAmount;
        }

        // Update company balances
        company.setCashBalance(cashBalance);
        company.setCardBalance(cardBalance);
    }


    @Override
    public void reminderFilter(StudentReminderFilter filter, OutputStream outputStream) {
        List<Reminder> allByStatus = this.reminderRepository.findAll(filter);

        log.info(allByStatus.toString());
        if (allByStatus.isEmpty()) {
            throw new ResourceNotFoundException("Eslatmalar topilmadi!");
        }

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Students");

            // Column headers
            String[] columns = {
                    "ID", "O'QUVCHI ID", "O'QUVCHI F.I.SH",
                    "SINF", "O'QUVCHI HOLATI", "O'QUVCHI YILI",
                    "OY", "ESLATILDIMI?!", "TAHMINIY TO'LOV VAQTI", "IZOH",
                    "KIM ESLATDI?!",
                    "QOCHON ESLATILDI?!"
            };

            // Header Style (Bold + Centered + Light Blue)
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerFont.setFontHeightInPoints((short) 12); // 🔹 Bigger Font
            headerStyle.setFont(headerFont);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setFillForegroundColor(IndexedColors.TEAL.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            setBorders(headerStyle);


            // Data Style (Centered + Light Yellow)
            CellStyle dataStyle = workbook.createCellStyle();
            Font dataFont = workbook.createFont();
            dataStyle.setAlignment(HorizontalAlignment.CENTER);
            dataFont.setFontHeightInPoints((short) 10); // 🔹 Bigger Font
            dataStyle.setFillForegroundColor(IndexedColors.WHITE.getIndex());
            dataStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            setBorders(dataStyle);


            // 🔹 Create Header Row with increased height
            Row headerRow = sheet.createRow(0);
            headerRow.setHeightInPoints(20); // 🔹 Bigger Row Height
            for (int i = 0; i < columns.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columns[i]);
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, 5000); // 🔹 Increase Column Width
                sheet.autoSizeColumn(i); // Auto-size columns
            }

            // Fill data rows
            int rowIdx = 1;
            for (Reminder contract : allByStatus) {
                Row row = sheet.createRow(rowIdx++);
                row.setHeightInPoints(18); // 🔹 Increase Row Height
                Cell cell0 = row.createCell(0);
                cell0.setCellValue(contract.getId());
                cell0.setCellStyle(dataStyle);

                Cell cell1 = row.createCell(1);
                StudentContract studentContract = contract.getStudentContract() != null ? contract.getStudentContract() : null;
                cell1.setCellValue(studentContract != null ? studentContract.getId() : 0L);
                cell1.setCellStyle(dataStyle);

                row.createCell(2).setCellValue(contract.getStudentContract().getStudentFullName() != null
                        ? contract.getStudentContract().getStudentFullName()
                        : "N/A");
                row.createCell(3).setCellValue(contract.getStudentContract().getGrade() != null
                        ? contract.getStudentContract().getGrade().name().replace("GRADE_", "") + "-" + contract.getStudentContract().getStGrade().name()
                        : "N/A");
                row.createCell(4).setCellValue(contract.getStudentContract().getStatus()
                        ? "Active"
                        : "Passive");
                row.createCell(5).setCellValue(contract.getStudentContract() != null
                        ? contract.getStudentContract().getAcademicYear()
                        : "N/A");
                row.createCell(6).setCellValue(contract.getMonth() != null
                        ? contract.getMonth().name()
                        : "N/A");
                row.createCell(7).setCellValue(contract.getIsReminded()
                        ? "HA"
                        : "YO'Q");
                row.createCell(8).setCellValue(contract.getEstimatedTime() != null
                        ? contract.getEstimatedTime().toString()
                        : "N/A");
                row.createCell(9).setCellValue(contract.getComment() != null
                        ? contract.getComment()
                        : "N/A");
                Long createdBy;
                User user = null;
                createdBy = contract.getCreatedBy();
                if (createdBy != null) {
                    user = this.userRepository.findByIdAndDeletedFalse(createdBy)
                            .orElse(null);
                }
                row.createCell(10).setCellValue(contract.getCreatedBy() != null
                        ? user != null ? user.getFullName() : null
                        : "N/A");
                row.createCell(11).setCellValue(contract.getCreatedAt() != null
                        ? contract.getCreatedAt().toString()
                        : null);
                // Apply data style to all cells in the row
                for (int i = 0; i < columns.length; i++) {
                    Cell cell = row.getCell(i);
                    if (cell != null) {
                        cell.setCellStyle(dataStyle);
                    }
                }
            }

            // Auto-size columns
            for (int i = 0; i < columns.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(outputStream);
            outputStream.flush();

        } catch (IOException e) {
            throw new RuntimeException("Error exporting to Excel", e);
        }

    }

    @Override
    public ApiResponse transactionFilter(TransactionFilter filter, Pageable pageable) {
        Page<Transaction> transactionPage = this.transactionRepository.findAll(filter, pageable);
        List<Transaction> transactions = this.transactionRepository.findAll(filter);
        if (transactionPage.isEmpty()) {
            return ApiResponse.builder()
                    .status(HttpStatus.OK)
                    .message(HttpStatus.NO_CONTENT.getReasonPhrase())
                    .data(new ArrayList<>())
                    .build();
        }
        double amount = 0.0;

        List<TransactionDto.ToFilterDto> transactionDtoList = transactionMapper.toFilterDto(transactionPage.getContent());
//        for (TransactionDto.ToFilterDto dto : transactionDtoList) {
//            amount +=dto.getAmount();
//        }
        amount = transactions
                .stream()
                .mapToDouble(Transaction::getAmount)
                .sum();
        log.info("amount {}", amount);
        return ApiResponse.builder()
                .status(HttpStatus.OK)
                .message(HttpStatus.OK.getReasonPhrase())
                .data(transactionDtoList)
                .meta(Map.of("amount", amount))
                .elements(transactionPage.getTotalElements())
                .pages(transactionPage.getTotalPages())
                .build();
    }

    @Transactional
    @Override
    public ApiResponse updateStudentTariff(Long studentContractId, Long tariffId, String reason) {
        StudentContract studentContract = this.studentContractRepository.findByIdAndDeletedFalseAndStatusTrue(studentContractId)
                .orElseThrow(() -> new ResourceNotFoundException("Student contract not found"));

        StudentTariff newTariff = this.studentTariffRepository.findByIdAndDeletedFalse(tariffId)
                .orElseThrow(() -> new ResourceNotFoundException("Tariff not found"));

        // Agar yangi tarif eski bilan bir xil bo‘lsa
        if (studentContract.getTariff().getName().equals(newTariff.getName())) {
            return ApiResponse.builder()
                    .status(HttpStatus.BAD_REQUEST)
                    .message("Bu tariff allaqachon qo'llanilgan!")
                    .build();
        }

        // Eski aktiv tarixni topamiz
        Optional<TariffChangeHistory> activeHistoryOpt = tariffChangeHistoryRepository
                .findActiveTariffByContractId(studentContractId);

        if (activeHistoryOpt.isPresent()) {
            TariffChangeHistory currentHistory = activeHistoryOpt.get();
            currentHistory.setToDate(LocalDate.now().minusDays(1)); // Bugungi kundan oldingi kun
            tariffChangeHistoryRepository.save(currentHistory);
        }

        // Yangi tarix yozamiz
        TariffChangeHistory newHistory = new TariffChangeHistory();
        newHistory.setTariffAmount(newTariff.getAmount());
        newHistory.setTariffStatus(newTariff.getTariffStatus());
        newHistory.setFromDate(LocalDate.now());
        newHistory.setToDate(null); // aktiv
        newHistory.setStudentContract(studentContract);
        newHistory.setReason(reason);
        newHistory.setTariff(newTariff);
        tariffChangeHistoryRepository.save(newHistory);

        // Student contractda ham yangilaymiz
        studentContract.setAmount(newTariff.getAmount());
        studentContract.setTariff(newTariff);
        studentContract = studentContractRepository.save(studentContract);

        try {
            this.googleSheetsService.initializeSheet();
            this.googleSheetsService.updateStudentContractRow(studentContract);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return ApiResponse.builder()
                .status(HttpStatus.CREATED)
                .message(RestConstants.SUCCESS)
                .build();
    }


    @Override
    public ApiResponse getAllTariffHistory(Long studentContractId) {
        List<TariffChangeHistory> historyList = this.tariffChangeHistoryRepository.findAllByStudentContractId(studentContractId);
        if (historyList.isEmpty()) {
            return ApiResponse.builder()
                    .status(HttpStatus.OK)
                    .data(new ArrayList<>())
                    .build();
        }
        List<TariffChangeHistoryDto> dtoList = new ArrayList<>();
        for (TariffChangeHistory history : historyList) {

            TariffChangeHistoryDto dto = new TariffChangeHistoryDto();
            dto.setId(history.getId());
            dto.setStudentContractId(history.getStudentContract().getId());
            dto.setTariffStatus(history.getTariffStatus());
            dto.setTariffAmount(history.getTariffAmount());
            dto.setFromDate(history.getFromDate());
            dto.setToDate(history.getToDate());
            dto.setTariffId(history.getTariff().getId());
            dto.setReason(history.getReason());
            dto.setCreatedAt(history.getCreatedAt());
            dto.setUpdatedAt(history.getUpdatedAt());
            dto.setCreatedBy(history.getCreatedBy());
            dto.setUpdatedBy(history.getUpdatedBy());
            dto.setDeleted(history.getDeleted());

            dtoList.add(dto);
        }
        return ApiResponse.builder()
                .status(HttpStatus.OK)
                .message(RestConstants.SUCCESS)
                .data(dtoList)
                .build();

    }

    @Override
    public ApiResponse expenseTransactionFilter(ExpenseTransactionFilter filter, Pageable pageable) {
        Page<Transaction> transactionPage = this.transactionRepository.findAll(filter, pageable);
        List<Transaction> transactions = this.transactionRepository.findAll(filter);
        if (transactionPage.isEmpty()) {
            return ApiResponse.builder()
                    .status(HttpStatus.OK)
                    .data(new ArrayList<>())
                    .build();
        }
        double amount;

        List<TransactionDto.ToFilterDto> transactionDtoList = transactionMapper.toFilterDto(transactionPage.getContent());

        amount = transactions
                .stream()
                .mapToDouble(Transaction::getAmount)
                .sum();
        log.info("amount {}", amount);
        return ApiResponse.builder()
                .status(HttpStatus.OK)
                .message(HttpStatus.OK.getReasonPhrase())
                .data(transactionDtoList)
                .meta(Map.of("amount", amount))
                .elements(transactionPage.getTotalElements())
                .pages(transactionPage.getTotalPages())
                .build();

    }


    // Helper methods
    private MonthlyFee findOrCreateMonthlyFee(StudentContract contract, Company company,
                                              Category category, StudentTariff tariff,
                                              Months month, String academicYear) {
        return monthlyFeeRepository.findByContractMonthAndAcademicYear(
                        contract.getId(), month, academicYear)
                .orElseGet(() -> {
                    MonthlyFee fee = new MonthlyFee();
                    fee.setStudentContract(contract);
                    fee.setCompany(company);
                    fee.setCategory(category);
                    fee.setTotalFee(tariff.getAmount());
                    fee.setTariffName(tariff.getName());
                    fee.setMonths(month);
                    fee.setAmountPaid(0.0);
                    fee.setRemainingBalance(tariff.getAmount());
                    fee.setStatus(PaymentStatus.UNPAID);
                    fee.setCreatedAt(LocalDateTime.now());
                    fee.setUpdatedAt(LocalDateTime.now());
                    return fee;
                });
    }

    private String getNextAcademicYear(String currentAcademicYear) {
        String[] years = currentAcademicYear.split("-");
        int start = Integer.parseInt(years[0]);
        int end = Integer.parseInt(years[1]);
        return (start + 1) + "-" + (end + 1);
    }

    private int findMonthIndex(Months[] academicMonths, Months target) {
        for (int i = 0; i < academicMonths.length; i++) {
            if (academicMonths[i] == target) {
                return i;
            }
        }
        return -1;
    }

    private void applyPaymentToFee(MonthlyFee fee, double amount) {
        fee.setAmountPaid(fee.getAmountPaid() + amount);
        fee.setRemainingBalance(fee.getRemainingBalance() - amount);

        if (fee.getRemainingBalance() <= 0) {
            fee.setStatus(PaymentStatus.FULLY_PAID);
        } else if (fee.getAmountPaid() > 0) {
            fee.setStatus(PaymentStatus.PARTIALLY_PAID);
        }
        fee.setUpdatedAt(LocalDateTime.now());
    }


    @Scheduled(cron = "0 0 9 10 * *") // Every day at midnight
    public void deactivateExpiredContracts() {
        List<StudentContract> expiredContracts = this.studentContractRepository
                .findExpiredContracts(LocalDate.now());
        for (StudentContract contract : expiredContracts) {
            contract.setStatus(false);
            contract.setInactiveDate(LocalDate.now());
        }
        this.studentContractRepository.saveAll(expiredContracts);
    }
}


