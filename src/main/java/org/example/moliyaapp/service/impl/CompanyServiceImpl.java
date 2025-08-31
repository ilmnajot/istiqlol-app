package org.example.moliyaapp.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.moliyaapp.dto.ApiResponse;
import org.example.moliyaapp.dto.CompanyDto;
import org.example.moliyaapp.entity.Company;
import org.example.moliyaapp.entity.MonthlyFee;
import org.example.moliyaapp.entity.StudentContract;
import org.example.moliyaapp.entity.TariffChangeHistory;
import org.example.moliyaapp.enums.Months;
import org.example.moliyaapp.enums.TariffStatus;
import org.example.moliyaapp.exception.ResourceNotFoundException;
import org.example.moliyaapp.projection.*;
import org.example.moliyaapp.repository.CompanyRepository;
import org.example.moliyaapp.repository.MonthlyFeeRepository;
import org.example.moliyaapp.repository.StudentContractRepository;
import org.example.moliyaapp.repository.TariffChangeHistoryRepository;
import org.example.moliyaapp.service.CompanyService;
import org.example.moliyaapp.utils.RestConstants;
import org.springdoc.webmvc.ui.SwaggerIndexTransformer;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

import static org.example.moliyaapp.utils.RestConstants.COMPANY_NOT_FOUND;
import static org.example.moliyaapp.utils.RestConstants.SUCCESS;

@Slf4j
@Service
@RequiredArgsConstructor
public class CompanyServiceImpl implements CompanyService {

    private final CompanyRepository companyRepository;
    private final MonthlyFeeRepository monthlyFeeRepository;
    private final TariffChangeHistoryRepository tariffChangeHistoryRepository;
    private final StudentContractRepository studentContractRepository;


    @Override
    public ApiResponse getBalance(Long id, LocalDate fromDate, LocalDate toDate) {

        Company company = this.companyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(COMPANY_NOT_FOUND));

        List<TransactionSummary> transactionSummaries;

        if (fromDate == null && toDate == null) {
            transactionSummaries = companyRepository.getTransactionSummaryByCompanyWithId(company.getId());

        } else if (fromDate != null && toDate == null) {
            transactionSummaries = companyRepository.getTransactionSummaryByCompanyWithDate(company.getId(), fromDate.atStartOfDay());
        } else if (fromDate == null && toDate != null) {
            transactionSummaries = companyRepository.getTransactionSummaryByCompanyTo(company.getId(), toDate.atTime(LocalTime.MAX));
        } else {
            transactionSummaries = companyRepository.getTransactionSummaryByCompany(
                    company.getId(),
                    fromDate.atStartOfDay(),
                    toDate.atTime(LocalTime.MAX)
            );
        }
        List<CompanyDto> companyDtoList = new ArrayList<>();
        for (TransactionSummary summary : transactionSummaries) {
            companyDtoList.add(new CompanyDto(
                    summary.getTransactionType(),
                    summary.getAmount(),
                    summary.getCashAmount(),
                    summary.getCardAmount()));
        }
        return ApiResponse.builder()
                .status(HttpStatus.OK)
                .message(SUCCESS)
                .data(companyDtoList)
                .build();
    }


    @Override
    public ApiResponse getAllByMonths(String academicYear) {
        // Validate input
        if (academicYear == null || academicYear.trim().isEmpty()) {
            return ApiResponse.builder()
                    .status(HttpStatus.BAD_REQUEST)
                    .message("O'QUV YILI MAJBURIY")
                    .build();
        }
        // Get data from repository
        List<GetAmountByMonth> monthlyStatistics = monthlyFeeRepository.getAcademicYearStats(academicYear); //buni emas

        // Create a map of existing data for quick lookup
        Map<String, GetAmountByMonth> statsMap = monthlyStatistics.stream()
                .collect(Collectors.toMap(
                        stat -> stat.getMonth().toUpperCase(),
                        stat -> stat,
                        (existing, replacement) -> existing
                ));

        // Transform to response DTOs using enum values
        List<GetAmountByMonthResponse> responses = Arrays.stream(Months.values())
                .map(month -> {
                    String monthName = month.name();
                    GetAmountByMonth stats = statsMap.get(monthName);

                    return new GetAmountByMonthResponse(
                            month,
                            stats != null && stats.getTotalAmount() != null ? stats.getTotalAmount() : 0.0,
                            stats != null && stats.getPaidAmount() != null ? stats.getPaidAmount() : 0.0,
                            stats != null && stats.getUnPaidAmount() != null ? stats.getUnPaidAmount() : 0.0,
                            stats != null && stats.getCuttingAmount() != null ? stats.getCuttingAmount() : 0.0
                    );
                })
                .collect(Collectors.toList());

        return ApiResponse.builder()
                .status(HttpStatus.OK)
                .message(SUCCESS)
                .data(responses)
                .build();
    }
    @Override
    public ApiResponse getAllByMonthsEmployee(LocalDate from, LocalDate to) {

        List<GetEmployeeAmountByMonth> monthlyStatistics;
        LocalDateTime startDate = from != null ? from.atStartOfDay() : null;
        LocalDateTime endDate = to != null ? to.atTime(LocalTime.MAX) : null; // includes entire day

        if (startDate == null && endDate == null) {
            monthlyStatistics = monthlyFeeRepository.getAcademicYearStatsForEmployeeWithoutDateRange();
        } else if (startDate != null && endDate == null) {
            monthlyStatistics = this.monthlyFeeRepository.getAcademicYearStatsForEmployeeWithFromDate(startDate);
        } else if (startDate == null && endDate != null) {
            monthlyStatistics = this.monthlyFeeRepository.getAcademicYearStatsForEmployeeWithToDate(endDate);
        } else {
            monthlyStatistics = monthlyFeeRepository.getAcademicYearStatsForEmployee(startDate, endDate);
        }
        Map<String, GetEmployeeAmountByMonth> statsMap = monthlyStatistics.stream()
                .collect(Collectors.toMap(
                        stat -> stat.getMonth().name().toUpperCase(),
                        stat -> stat,
                        (existing, replacement) -> existing
                ));

        // Transform endDate response DTOs using enum values
        List<GetEmployeeAmountByMonthResponse> responses = Arrays.stream(Months.values())
                .map(month -> {
                    String monthName = month.name();
                    GetEmployeeAmountByMonth stats = statsMap.get(monthName);

                    return new GetEmployeeAmountByMonthResponse(
                            month,
                            stats != null && stats.getTotalAmount() != null ? stats.getTotalAmount() : 0.0,
                            stats != null && stats.getFinalAmount() != null ? stats.getFinalAmount() : 0.0,
                            stats != null && stats.getPaidAmount() != null ? stats.getPaidAmount() : 0.0,
                            stats != null && stats.getUnPaidAmount() != null ? stats.getUnPaidAmount() : 0.0,
                            stats != null && stats.getBonusAmount() != null ? stats.getBonusAmount() : 0.0
                    );
                })
                .collect(Collectors.toList());

        return ApiResponse.builder()
                .status(HttpStatus.OK)
                .message(SUCCESS)
                .data(responses)
                .build();
    }

//    // Constants for tariff calculations (based on 10-month academic year)
//    private static final double YEARLY_TARIFF_MONTHS = 10.0; // September to June
//    private static final double QUARTERLY_TARIFF_MONTHS = 2.5; // 10 months / 4 quarters

//    @Override
//    public ApiResponse getAllByTariff(String year, TariffStatus tariffStatus) {
//        if (year == null || year.trim().isEmpty()) {
//            return ApiResponse.builder()
//                    .status(HttpStatus.BAD_REQUEST)
//                    .message("O'QUV YILI MAJBURIY!")
//                    .build();
//        }
//        if (tariffStatus == null) {
//            return ApiResponse.builder()
//                    .status(HttpStatus.BAD_REQUEST)
//                    .message("TARIF TANLASH MAJBURIY!")
//                    .build();
//        }
//
//        // Get students who had this tariff status at any point during the academic year
//        List<StudentContract> studentContractList = getStudentsWithTariffStatusInYear(tariffStatus, year);
//
//        if (studentContractList.isEmpty()) {
//            return ApiResponse.builder()
//                    .status(HttpStatus.NOT_FOUND)
//                    .message("Ma'lumot topilmadi!")
//                    .build();
//        }
//
//        return switch (tariffStatus) {
//            case YEARLY -> this.getYearlyStatistics(studentContractList, year);
//            case QUARTERLY -> this.getQuarterlyStatistics(studentContractList, year);
//            case MONTHLY -> this.getMonthlyStatistics(studentContractList, year);
//        };
//    }

//
//    // Helper method to get students who had specific tariff status during the academic year
//    private List<StudentContract> getStudentsWithTariffStatusInYear(TariffStatus tariffStatus, String year) {
//        // Parse academic year to get date range
//        LocalDate[] yearRange = parseAcademicYear(year);
//        LocalDate startDate = yearRange[0];
//        LocalDate endDate = yearRange[1];
//
//        return tariffChangeHistoryRepository.findStudentContractsWithTariffStatusInPeriod(
//                tariffStatus, startDate, endDate);
//    }
//
//    // Helper method to get active tariff for a student at a specific date
//    private TariffChangeHistory getActiveTariffAtDate(Long studentContractId, LocalDate date) {
//        return tariffChangeHistoryRepository.findActiveTariffAtDate(studentContractId, date)
//                .orElse(null);
//    }

//    // Helper method to get active tariff for a student in a specific month of academic year
//    private TariffChangeHistory getActiveTariffForMonth(Long studentContractId, Months month, String academicYear) {
//        LocalDate monthDate = getDateForMonthInAcademicYear(month, academicYear);
//        if (monthDate == null) return null; // Skip vacation months
//        return getActiveTariffAtDate(studentContractId, monthDate);
//    }

//    private LocalDate getDateForMonthInAcademicYear(Months month, String academicYear) {
//        // Convert academic year and month to actual date
//        // Academic year format: "2023-2024"
//        String[] years = academicYear.split("-");
//        int startYear = Integer.parseInt(years[0]);
//        int endYear = Integer.parseInt(years[1]);
//
//        // Map months to actual calendar months (only 10 academic months)
//        return switch (month) {
//            case SENTABR -> LocalDate.of(startYear, 9, 1);
//            case OKTABR -> LocalDate.of(startYear, 10, 1);
//            case NOYABR -> LocalDate.of(startYear, 11, 1);
//            case DEKABR -> LocalDate.of(startYear, 12, 1);
//            case YANVAR -> LocalDate.of(endYear, 1, 1);
//            case FEVRAL -> LocalDate.of(endYear, 2, 1);
//            case MART -> LocalDate.of(endYear, 3, 1);
//            case APREL -> LocalDate.of(endYear, 4, 1);
//            case MAY -> LocalDate.of(endYear, 5, 1);
//            case IYUN -> LocalDate.of(endYear, 6, 1);
//            // IYUL and AVGUST are vacation months - students don't pay
//            case IYUL, AVGUST -> null; // Return null for vacation months
//        };
//    }

//    private LocalDate[] parseAcademicYear(String academicYear) {
//        String[] years = academicYear.split("-");
//        int startYear = Integer.parseInt(years[0]);
//        int endYear = Integer.parseInt(years[1]);
//
//        LocalDate startDate = LocalDate.of(startYear, 9, 1); // September 1st
//        LocalDate endDate = LocalDate.of(endYear, 6, 30);    // June 30th (10 months total)
//
//        return new LocalDate[]{startDate, endDate};
//    }

//    private ApiResponse getQuarterlyStatistics(List<StudentContract> contracts, String year) {
//        Map<String, Double> quarterlyTotals = new HashMap<>();
//        Map<String, Double> quarterlyPaid = new HashMap<>();
//        Map<String, Double> quarterlyCut = new HashMap<>();
//
//        // Define quarters with proper 2.5 month distribution
//        // Each quarter = 2.5 months, some months are split between quarters
//        Map<String, Map<Months, Double>> quarterDistribution = Map.of(
//                "I-QUARTER", Map.of(
//                        Months.SENTABR, 1.0,   // Full September
//                        Months.OKTABR, 1.0,    // Full October
//                        Months.NOYABR, 0.5     // Half November
//                ),
//                "II-QUARTER", Map.of(
//                        Months.NOYABR, 0.5,    // Half November
//                        Months.DEKABR, 1.0,    // Full December
//                        Months.YANVAR, 1.0     // Full January
//                ),
//                "III-QUARTER", Map.of(
//                        Months.FEVRAL, 1.0,    // Full February
//                        Months.MART, 1.0,      // Full March
//                        Months.APREL, 0.5      // Half April
//                ),
//                "IV-QUARTER", Map.of(
//                        Months.APREL, 0.5,     // Half April
//                        Months.MAY, 1.0,       // Full May
//                        Months.IYUN, 1.0       // Full June
//                )
//        );
//
//        // Initialize
//        quarterDistribution.keySet().forEach(quarter -> {
//            quarterlyTotals.put(quarter, 0.0);
//            quarterlyPaid.put(quarter, 0.0);
//            quarterlyCut.put(quarter, 0.0);
//        });
//
//        for (StudentContract contract : contracts) {
//            List<MonthlyFee> fees = monthlyFeeRepository.findAllByStudentContractIdAndYear(
//                    contract.getId(), year);
//
//            Map<Months, List<MonthlyFee>> feesByMonth = fees.stream()
//                    .collect(Collectors.groupingBy(MonthlyFee::getMonths));
//
//            for (Map.Entry<String, Map<Months, Double>> quarterEntry : quarterDistribution.entrySet()) {
//                String quarter = quarterEntry.getKey();
//                Map<Months, Double> monthsWithProportion = quarterEntry.getValue();
//
//                double quarterTotal = 0;
//                double quarterPaid = 0;
//                double quarterCut = 0;
//
//                for (Map.Entry<Months, Double> monthEntry : monthsWithProportion.entrySet()) {
//                    Months month = monthEntry.getKey();
//                    Double proportion = monthEntry.getValue(); // 1.0 for full month, 0.5 for half month
//
//                    // Skip vacation months
//                    LocalDate monthDate = getDateForMonthInAcademicYear(month, year);
//                    if (monthDate == null) continue; // Skip July and August
//
//                    // Get active tariff for this month
//                    TariffChangeHistory activeTariff = getActiveTariffForMonth(contract.getId(), month, year);
//
//                    if (activeTariff != null && activeTariff.getTariffStatus() == TariffStatus.QUARTERLY) {
//                        List<MonthlyFee> monthFees = feesByMonth.getOrDefault(month, Collections.emptyList());
//
//                        // Calculate expected amount based on historical tariff
//                        double expectedMonthlyAmount = calculateMonthlyTariff(
//                                activeTariff.getTariffStatus(),
//                                activeTariff.getTariffAmount()
//                        );
//
//                        // Apply proportion (1.0 for full month, 0.5 for half month)
//                        quarterTotal += expectedMonthlyAmount * proportion;
//                        quarterPaid += monthFees.stream().mapToDouble(MonthlyFee::getAmountPaid).sum() * proportion;
//                        quarterCut += monthFees.stream().mapToDouble(MonthlyFee::getCutAmount).sum() * proportion;
//                    }
//                }
//
//                quarterlyTotals.merge(quarter, quarterTotal, Double::sum);
//                quarterlyPaid.merge(quarter, quarterPaid, Double::sum);
//                quarterlyCut.merge(quarter, quarterCut, Double::sum);
//            }
//        }
//
//        // Build response
//        List<GetAmountByTariffResponse> responseList = quarterDistribution.keySet()
//                .stream()
//                .map(quarter -> {
//                    double total = quarterlyTotals.get(quarter);
//                    double paid = quarterlyPaid.get(quarter);
//                    double cut = quarterlyCut.get(quarter);
//                    double unpaid = total - paid - cut;
//
//                    return new GetAmountByTariffResponse(
//                            quarter,
//                            total,
//                            paid,
//                            Math.max(0, unpaid),
//                            cut
//                    );
//                })
//                .collect(Collectors.toList());
//
//        return ApiResponse.builder()
//                .status(HttpStatus.OK)
//                .message(RestConstants.SUCCESS)
//                .data(responseList)
//                .meta(Map.of(
//                        "TYPE", "QUARTERLY_BREAKDOWN",
//                        "ACADEMIC_YEAR", year,
//                        "TOTAL_STUDENTS", contracts.size()
//                ))
//                .build();
//    }
//
//    //    private ApiResponse getMonthlyStatistics(List<StudentContract> studentContractList, String academicYear) {

    /// /        Map<Months, Double> monthlyTotals = new HashMap<>();
    /// /        Map<Months, Double> monthlyPaid = new HashMap<>();
    /// /        Map<Months, Double> monthlyUnPaid = new HashMap<>();
    /// /        Map<Months, Double> monthlyCutting = new HashMap<>();
    /// /
    /// /        // Initialize only academic months (exclude July and August vacation months)
    /// /        Months[] academicMonths = {
    /// /                Months.SENTABR, Months.OKTABR, Months.NOYABR, Months.DEKABR,
    /// /                Months.YANVAR, Months.FEVRAL, Months.MART, Months.APREL,
    /// /                Months.MAY, Months.IYUN
    /// /        };
    /// /        for (Months month : academicMonths) {
    /// /            monthlyTotals.put(month, 0.0);
    /// /            monthlyPaid.put(month, 0.0);
    /// /            monthlyCutting.put(month, 0.0);
    /// /            monthlyUnPaid.put(month, 0.0);
    /// /        }
    /// /
    /// /        for (StudentContract contract : studentContractList) {
    /// /            List<MonthlyFee> feeList = monthlyFeeRepository.findAllByStudentContractIdAndYear(
    /// /                    contract.getId(), academicYear);
    /// /
    /// /            // Group by month
    /// /            Map<Months, List<MonthlyFee>> feesByMonth = feeList.stream()
    /// /                    .collect(Collectors.groupingBy(MonthlyFee::getMonths));
    /// /
    /// /            for (Months month : academicMonths) {
    /// /                // Get active tariff for this specific month
    /// /                TariffChangeHistory activeTariff = getActiveTariffForMonth(contract.getId(), month, academicYear);
    /// /
    /// /                if (activeTariff != null && activeTariff.getTariffStatus() == TariffStatus.MONTHLY) {
    /// /                    List<MonthlyFee> monthFees = feesByMonth.getOrDefault(month, Collections.emptyList());
    /// /
    /// /                    // Use historical tariff amount instead of current tariff
    /// /                    double expectedAmount = activeTariff.getTariffAmount();
    /// /
    /// /                    double monthCut = monthFees.stream()
    /// /                            .mapToDouble(MonthlyFee::getCutAmount)
    /// /                            .sum();
    /// /
    /// /                    double monthPaid = monthFees.stream()
    /// /                            .mapToDouble(MonthlyFee::getAmountPaid)
    /// /                            .sum();
    /// /
    /// /                    monthlyTotals.merge(month, expectedAmount - monthCut, Double::sum);
    /// /                    monthlyPaid.merge(month, monthPaid, Double::sum);
    /// /                    monthlyCutting.merge(month, monthCut, Double::sum);
    /// /                }
    /// /            }
    /// /        }
    /// /
    /// /        // Only calculate unpaid where monthly tariff was active
    /// /        for (Months month : academicMonths) {
    /// /            double total = monthlyTotals.get(month);
    /// /            if (total > 0) { // Means tariff was monthly in that month
    /// /                double unpaid = total - monthlyPaid.get(month);
    /// /                monthlyUnPaid.put(month, Math.max(0, unpaid));
    /// /            } else {
    /// /                monthlyUnPaid.put(month, 0.0); // No monthly expected, so no unpaid
    /// /            }
    /// /        }
    /// /
    /// /
    /// /        // Build response (only for academic months)
    /// /        List<GetAmountByMonthResponse> responseList = Arrays.stream(academicMonths)
    /// /                .map(month -> new GetAmountByMonthResponse(
    /// /                        month,
    /// /                        monthlyTotals.get(month),
    /// /                        monthlyPaid.get(month),
    /// /                        monthlyUnPaid.get(month),
    /// /                        monthlyCutting.get(month)
    /// /                ))
    /// /                .collect(Collectors.toList());
    /// /
    /// /        return ApiResponse.builder()
    /// /                .status(HttpStatus.OK)
    /// /                .message(RestConstants.SUCCESS)
    /// /                .data(responseList)
    /// /                .meta(Map.of(
    /// /                        "type", "MONTHLY_BREAKDOWN",
    /// /                        "totalStudents", studentContractList.size()))
    /// /                .build();
    /// /    }
//    private ApiResponse getMonthlyStatistics(List<StudentContract> studentContractList, String academicYear) {
//        Map<Months, Double> monthlyTotals = new HashMap<>();
//        Map<Months, Double> monthlyPaid = new HashMap<>();
//        Map<Months, Double> monthlyUnPaid = new HashMap<>();
//        Map<Months, Double> monthlyCutting = new HashMap<>();
//
//        // Initialize only academic months (exclude July and August vacation months)
//        Months[] academicMonths = {
//                Months.SENTABR, Months.OKTABR, Months.NOYABR, Months.DEKABR,
//                Months.YANVAR, Months.FEVRAL, Months.MART, Months.APREL,
//                Months.MAY, Months.IYUN
//        };
//        for (Months month : academicMonths) {
//            monthlyTotals.put(month, 0.0);
//            monthlyPaid.put(month, 0.0);
//            monthlyCutting.put(month, 0.0);
//            monthlyUnPaid.put(month, 0.0);
//        }
//
//        for (StudentContract contract : studentContractList) {
//            List<MonthlyFee> feeList = monthlyFeeRepository.findAllByStudentContractIdAndYear(
//                    contract.getId(), academicYear);
//
//            // Group by month
//            Map<Months, List<MonthlyFee>> feesByMonth = feeList.stream()
//                    .collect(Collectors.groupingBy(MonthlyFee::getMonths));
//
//            for (Months month : academicMonths) {
//                // Get active tariff for this specific month
//                TariffChangeHistory activeTariff = getActiveTariffForMonth(contract.getId(), month, academicYear);
//
//                if (activeTariff != null && activeTariff.getTariffStatus() == TariffStatus.MONTHLY) {
//                    List<MonthlyFee> monthFees = feesByMonth.getOrDefault(month, Collections.emptyList());
//
//                    // Use historical tariff amount instead of current tariff
//                    double expectedAmount = activeTariff.getTariffAmount();
//
//                    double monthCut = monthFees.stream()
//                            .mapToDouble(MonthlyFee::getCutAmount)
//                            .sum();
//
//                    double monthPaid = monthFees.stream()
//                            .mapToDouble(MonthlyFee::getAmountPaid)
//                            .sum();
//
//                    monthlyTotals.merge(month, expectedAmount - monthCut, Double::sum);
//                    monthlyPaid.merge(month, monthPaid, Double::sum);
//                    monthlyCutting.merge(month, monthCut, Double::sum);
//                }
//            }
//        }
//
//        // Only calculate unpaid where monthly tariff was active
//        for (Months month : academicMonths) {
//            double total = monthlyTotals.get(month);
//            if (total > 0) { // Means tariff was monthly in that month
//                double unpaid = total - monthlyPaid.get(month);
//                monthlyUnPaid.put(month, Math.max(0, unpaid));
//            } else {
//                monthlyUnPaid.put(month, 0.0); // No monthly expected, so no unpaid
//            }
//        }
//
//
//        // Build response (only for academic months)
//        List<GetAmountByMonthResponse> responseList = Arrays.stream(academicMonths)
//                .map(month -> new GetAmountByMonthResponse(
//                        month,
//                        monthlyTotals.get(month),
//                        monthlyPaid.get(month),
//                        monthlyUnPaid.get(month),
//                        monthlyCutting.get(month)
//                ))
//                .collect(Collectors.toList());
//
//        return ApiResponse.builder()
//                .status(HttpStatus.OK)
//                .message(RestConstants.SUCCESS)
//                .data(responseList)
//                .meta(Map.of(
//                        "type", "MONTHLY_BREAKDOWN",
//                        "totalStudents", studentContractList.size()))
//                .build();
//    }
//
//    private ApiResponse getYearlyStatistics(List<StudentContract> studentContractList, String academicYear) {
//        double totalAmount = 0.0;
//        double totalPaid = 0.0;
//        double totalUnPaid = 0.0;
//        double totalCutting = 0.0;
//
//        LocalDate[] yearRange = parseAcademicYear(academicYear);
//        LocalDate startDate = yearRange[0];
//        LocalDate endDate = yearRange[1];
//
//        for (StudentContract studentContract : studentContractList) {
//            List<MonthlyFee> feeList = this.monthlyFeeRepository.findAllByStudentContractIdAndYear(
//                    studentContract.getId(), academicYear);
//
//            // Get all tariff changes for this student during the academic year
//            List<TariffChangeHistory> tariffHistory = tariffChangeHistoryRepository
//                    .findByStudentContractIdAndDateRange(studentContract.getId(), startDate, endDate);
//
//            double yearlyAmount = 0.0;
//
//            // Calculate prorated amounts based on tariff changes
//            for (TariffChangeHistory history : tariffHistory) {
//                if (history.getTariffStatus() == TariffStatus.YEARLY) {
//                    LocalDate periodStart = history.getFromDate().isBefore(startDate) ? startDate : history.getFromDate();
//                    LocalDate periodEnd = (history.getToDate() == null || history.getToDate().isAfter(endDate))
//                            ? endDate : history.getToDate();
//
//                    // Calculate the proportion of the year this tariff was active
//                    long totalDays = ChronoUnit.DAYS.between(startDate, endDate) + 1;
//                    long activeDays = ChronoUnit.DAYS.between(periodStart, periodEnd) + 1;
//                    double proportion = (double) activeDays / totalDays;
//
//                    yearlyAmount += history.getTariffAmount() * proportion;
//                }
//            }
//
//            double cutAmount = feeList.stream().mapToDouble(MonthlyFee::getCutAmount).sum();
//            double finalYearlyAmount = yearlyAmount - cutAmount;
//            double paid = feeList.stream().mapToDouble(MonthlyFee::getAmountPaid).sum();
//
//            totalAmount += finalYearlyAmount;
//            totalPaid += paid;
//            totalUnPaid += Math.max(0, finalYearlyAmount - paid);
//            totalCutting += cutAmount;
//        }
//
//        GetAmountByMonthResponse response = new GetAmountByMonthResponse();
//        response.setTotalAmount(totalAmount);
//        response.setPaidAmount(totalPaid);
//        response.setUnPaidAmount(totalUnPaid);
//        response.setCuttingAmount(totalCutting);
//
//        return ApiResponse.builder()
//                .status(HttpStatus.OK)
//                .message(RestConstants.SUCCESS)
//                .data(response)
//                .meta(Map.of(
//                        "TYPE", "YEARLY",
//                        "ACADEMIC_YEAR", academicYear,
//                        "TOTAL_STUDENTS", studentContractList.size()
//                ))
//                .build();
//    }

//    private double calculateMonthlyTariff(TariffStatus tariffStatus, double tariffAmount) {
//        return switch (tariffStatus) {
//            case YEARLY -> tariffAmount / YEARLY_TARIFF_MONTHS;
//            case QUARTERLY -> tariffAmount / QUARTERLY_TARIFF_MONTHS;
//            default -> tariffAmount;
//        };
//    }
    @Override
    public ApiResponse getAllAmountByTariff(String year, TariffStatus tariffStatus) {
        if (year == null || year.trim().isEmpty()) {
            return ApiResponse.builder()
                    .status(HttpStatus.BAD_REQUEST)
                    .message("O'quv yili tanlanmagan")
                    .build();
        }
        if (tariffStatus == null) {
            return ApiResponse.builder()
                    .status(HttpStatus.BAD_REQUEST)
                    .message("Tarif turo tanlanmagan!")
                    .build();
        }
        switch (tariffStatus) {
            case MONTHLY -> {
                List<GetAmountByMonth> monthList = monthlyFeeRepository.getStatsByTariffMonthly(year, tariffStatus.name());
                List<GetAmountByMonthResponse> monthlyResponse = this.createMonthlyResponse(monthList);
                return ApiResponse.builder()
                        .status(HttpStatus.OK)
                        .message(SUCCESS)
                        .data(monthlyResponse)
                        .build();
            }
            case QUARTERLY -> {
                List<GetAmountByQuarter> quarterList = this.monthlyFeeRepository.getStatsByTariffQuarterly(year, tariffStatus.name());
                return ApiResponse.builder()
                        .status(HttpStatus.OK)
                        .message(SUCCESS)
                        .data(quarterList)
                        .build();
            }
            case YEARLY -> {
                Map<Months, Double> map = new HashMap<>();
                List<StudentContract> contractList = studentContractRepository.findAllByStatus(true);
                Months[] months = Months.values();
                double totalAmount = 0.0;
                double paidAmount = 0.0;
                double cutAmount = 0.0;
                double unPaidAmount = 0.0;
                MonthlyFee fee;
                for (Months month : months) {
                    for (StudentContract contract : contractList) {
                        fee = this.monthlyFeeRepository.findByStudentContractIdAndMonthsAndAcademicYear(contract.getId(), month, contract.getAcademicYear())
                                .orElseThrow(null);
                        if (fee != null) {
                            totalAmount += fee.getTotalFee() - fee.getCutAmount();
                            paidAmount += fee.getAmountPaid();
                            cutAmount += fee.getCutAmount();
                            unPaidAmount += fee.getRemainingBalance();
                        } else {
                            Double amount = contract.getTariff().getAmount();
                            totalAmount = amount;
                            unPaidAmount = amount;
                        }

                    }
                }
//                GetAmountByYear amountByYear = this.monthlyFeeRepository.getStatsByTariffYearly(year, tariffStatus.name());
                log.debug("amount: {}", totalAmount);
                return ApiResponse.builder()
                        .status(HttpStatus.OK)
                        .message(SUCCESS)
                        .meta(
                                Map.of(
                                        "total", totalAmount,
                                        "paid", paidAmount,
                                        "cut", cutAmount,
                                        "rest", unPaidAmount
                                )
                        )
                        .build();
            }
        }
        return ApiResponse
                .builder()
                .status(HttpStatus.BAD_REQUEST)
                .message("Tarif noto'g'ri!")
                .build();
    }

    private List<GetAmountByMonthResponse> createMonthlyResponse(List<GetAmountByMonth> monthList) {
        Map<String, GetAmountByMonth> map = monthList.stream()
                .collect(Collectors.toMap(
                        stat -> stat.getMonth().toUpperCase(),
                        stat -> stat,
                        (existing, replacement) -> existing
                ));
        return Arrays.stream(Months.values())
                .filter(month -> !month.equals(Months.IYUL) && !month.equals(Months.AVGUST)) // O'quv yili oylari
                .map(month -> {
                    String monthName = month.name();
                    GetAmountByMonth stats = map.get(monthName);

                    return new GetAmountByMonthResponse(
                            month,
                            stats != null && stats.getTotalAmount() != null ? stats.getTotalAmount() : 0.0,
                            stats != null && stats.getPaidAmount() != null ? stats.getPaidAmount() : 0.0,
                            stats != null && stats.getUnPaidAmount() != null ? stats.getUnPaidAmount() : 0.0,
                            stats != null && stats.getCuttingAmount() != null ? stats.getCuttingAmount() : 0.0
                    );
                })
                .collect(Collectors.toList());

    }
}

//    @Override
//    public ApiResponse getAllByMonths(String academicYear) {
//        // Validate input
//        if (academicYear == null || academicYear.trim().isEmpty()) {
//            return ApiResponse.builder()
//                    .status(HttpStatus.BAD_REQUEST)
//                    .message("O'QUV YILI MAJBURIY")
//                    .build();
//        }
//
//        // Get all student contracts for the academic year
//        List<StudentContract> allContracts = this.studentContractRepository.findAllByAcademicYear(academicYear);
//        if (allContracts.isEmpty()) {
//            return ApiResponse.builder()
//                    .status(HttpStatus.NOT_FOUND)
//                    .message("Ma'lumot topilmadi!")
//                    .build();
//        }
//
//        // Use the existing monthly statistics method logic
//        Map<Months, Double> monthlyTotals = new HashMap<>();
//        Map<Months, Double> monthlyPaid = new HashMap<>();
//        Map<Months, Double> monthlyUnPaid = new HashMap<>();
//        Map<Months, Double> monthlyCutting = new HashMap<>();
//
//        for (Months months : Months.values()) {
//            monthlyTotals.put(months, 0.0);
//            monthlyPaid.put(months, 0.0);
//            monthlyCutting.put(months, 0.0);
//            monthlyUnPaid.put(months, 0.0);
//        }
//
//        for (StudentContract studentContract : allContracts) {
//            List<MonthlyFee> feeList = this.monthlyFeeRepository.findAllByStudentContractIdAndYear(
//                    studentContract.getId(), academicYear);
//
//            // Group fees by month
//            Map<Months, List<MonthlyFee>> feesByMonth = feeList.stream()
//                    .collect(Collectors.groupingBy(MonthlyFee::getMonths));
//
//            for (Months month : Months.values()) {
//                List<MonthlyFee> monthFees = feesByMonth.getOrDefault(month, new ArrayList<>());
//                double monthTariffAmount;
//                double monthCutAmount = 0.0;
//                double monthPaidAmount = 0.0;
//
//                if (!monthFees.isEmpty()) {
//                    monthCutAmount = monthFees
//                            .stream()
//                            .mapToDouble(MonthlyFee::getCutAmount)
//                            .sum();
//                    log.debug("Month: {}, Cut Amount: {}", month, monthCutAmount);
//                    monthPaidAmount = monthFees
//                            .stream()
//                            .mapToDouble(MonthlyFee::getAmountPaid)
//                            .sum();
//
//                    log.debug("Month: {}, Paid Amount: {}", month, monthPaidAmount);
//                    monthTariffAmount = monthFees
//                            .stream()
//                            .mapToDouble(MonthlyFee::getTotalFee)
//                            .findFirst()
//                            .orElse(calculateMonthlyTariff(studentContract.getTariff().getTariffStatus(),
//                                    studentContract.getTariff().getAmount()));
//                    log.debug("Month: {}, Tariff Amount: {}", month, monthTariffAmount);
//                } else {
//                    monthTariffAmount = calculateMonthlyTariff(studentContract.getTariff().getTariffStatus(),
//                            studentContract.getTariff().getAmount());
//                    log.debug("Month: {}, Tariff Amount: {}", month, monthTariffAmount);
//                }
//
//                monthlyTotals.put(month, monthlyTotals.get(month) + monthTariffAmount - monthCutAmount);
//                log.debug("Month: {}, Total Amount: {}", month, monthlyTotals.get(month));
//                monthlyPaid.put(month, monthlyPaid.get(month) + monthPaidAmount);
//                log.debug("Month: {}, Paid Amount: {}", month, monthlyPaid.get(month));
//                monthlyCutting.put(month, monthlyCutting.get(month) + monthCutAmount);
//                log.debug("Month: {}, Cutting Amount: {}", month, monthlyCutting.get(month));
//            }
//        }
//
//        // Calculate unpaid amounts
//        for (Months month : Months.values()) {
//            double unpaidAmount = monthlyTotals.get(month) - monthlyPaid.get(month);
//            monthlyUnPaid.put(month, Math.max(0, unpaidAmount));
//        }
//
//        // Build response
//        List<GetAmountByMonthResponse> responses = Arrays.stream(Months.values())
//                .map(month -> new GetAmountByMonthResponse(
//                        month,
//                        monthlyTotals.get(month),
//                        monthlyPaid.get(month),
//                        monthlyUnPaid.get(month),
//                        monthlyCutting.get(month)
//
//                ))
//                .collect(Collectors.toList());
//        log.debug("Responses: {}", responses);
//        return ApiResponse.builder()
//                .status(HttpStatus.OK)
//                .message(SUCCESS)
//                .data(responses)
//                .build();
//    }


//    // Constants for tariff calculations
//    private static final double YEARLY_TARIFF_MONTHS = 10.0;
//    private static final double QUARTERLY_TARIFF_MONTHS = 2.5;
//
//    @Override
//    public ApiResponse getAllByTariff(String year, TariffStatus tariffStatus) {
//        if (year == null || year.trim().isEmpty()) {
//            return ApiResponse.builder()
//                    .status(HttpStatus.BAD_REQUEST)
//                    .message("O'QUV YILI MAJBURIY!")
//                    .build();
//        }
//        if (tariffStatus == null) {
//            return ApiResponse.builder()
//                    .status(HttpStatus.BAD_REQUEST)
//                    .message("TARIF TANLASH MAJBURIY!")
//                    .build();
//        }
//
//        List<StudentContract> studentContractList = this.studentContractRepository.findAllByStudentTariffStatus(tariffStatus, year);
//        if (studentContractList.isEmpty()) {
//            return ApiResponse.builder()
//                    .status(HttpStatus.NOT_FOUND)
//                    .message("Ma'lumot topilmadi!")
//                    .build();
//        }
//        return switch (tariffStatus) {
//            case YEARLY -> this.getYearlyStatistics(studentContractList, year);
//
//            case QUARTERLY -> this.getQuarterlyStatistics(studentContractList, year);
//
//            case MONTHLY -> this.getMonthlyStatistics(studentContractList, year);
//        };
//
//    }
//
//    private ApiResponse getQuarterlyStatistics(List<StudentContract> contracts, String year) {
//        Map<String, Double> quarterlyTotals = new HashMap<>();
//        Map<String, Double> quarterlyPaid = new HashMap<>();
//        Map<String, Double> quarterlyCut = new HashMap<>();
//
//        // Define quarters
//        Map<String, List<Months>> quarters = Map.of(
//                "I-QUARTER", List.of(Months.SENTABR, Months.OKTABR, Months.NOYABR),
//                "II-QUARTER", List.of(Months.DEKABR, Months.YANVAR, Months.FEVRAL),
//                "III-QUARTER", List.of(Months.MART, Months.APREL, Months.MAY),
//                "IV-QUARTER", List.of(Months.IYUN, Months.IYUL, Months.AVGUST)
//        );
//
//        // Initialize
//        quarters.keySet().forEach(quarter -> {
//            quarterlyTotals.put(quarter, 0.0);
//            quarterlyPaid.put(quarter, 0.0);
//            quarterlyCut.put(quarter, 0.0);
//        });
//
//        for (StudentContract contract : contracts) {
//            List<MonthlyFee> fees = monthlyFeeRepository.findAllByStudentContractIdAndYear(
//                    contract.getId(), year);
//
//            Map<Months, List<MonthlyFee>> feesByMonth = fees.stream()
//                    .collect(Collectors.groupingBy(MonthlyFee::getMonths));
//
//            for (Map.Entry<String, List<Months>> entry : quarters.entrySet()) {
//                String quarter = entry.getKey();
//                List<Months> months = entry.getValue();
//
//                double quarterTotal = 0;
//                double quarterPaid = 0;
//                double quarterCut = 0;
//
//                for (Months month : months) {
//                    List<MonthlyFee> monthFees = feesByMonth.getOrDefault(month, Collections.emptyList());
//                    quarterTotal += monthFees.stream().mapToDouble(MonthlyFee::getTotalFee).sum();
//                    quarterPaid += monthFees.stream().mapToDouble(MonthlyFee::getAmountPaid).sum();
//                    quarterCut += monthFees.stream().mapToDouble(MonthlyFee::getCutAmount).sum();
//                }
//
//                quarterlyTotals.merge(quarter, quarterTotal, Double::sum);
//                quarterlyPaid.merge(quarter, quarterPaid, Double::sum);
//                quarterlyCut.merge(quarter, quarterCut, Double::sum);
//            }
//        }
//        // Build response
//        List<GetAmountByTariffResponse> responseList = quarters.keySet().stream()
//                .map(quarter -> {
//                    double total = quarterlyTotals.get(quarter);
//                    double paid = quarterlyPaid.get(quarter);
//                    double cut = quarterlyCut.get(quarter);
//                    double unpaid = total - paid - cut;
//
//                    return new GetAmountByTariffResponse(
//                            quarter,
//                            total,
//                            paid,
//                            Math.max(0, unpaid),
//                            cut
//                    );
//                })
//                .collect(Collectors.toList());
//
//        return ApiResponse.builder()
//                .status(HttpStatus.OK)
//                .message(RestConstants.SUCCESS)
//                .data(responseList)
//                .meta(Map.of(
//                        "TYPE", "QUARTERLY_BREAKDOWN",
//                        "ACADEMIC_YEAR", year,
//                        "TOTAL_STUDENTS", contracts.size()
//                ))
//                .build();
//    }
//
//
//    private double calculateMonthlyTariff(TariffStatus tariffStatus, double quarterlyTariffAmount) {
//        return switch (tariffStatus) {
//            case YEARLY -> quarterlyTariffAmount / YEARLY_TARIFF_MONTHS;
//            case QUARTERLY -> quarterlyTariffAmount / QUARTERLY_TARIFF_MONTHS;
//            default -> quarterlyTariffAmount;
//        };
//    }
//
//
//    private ApiResponse getMonthlyStatistics(List<StudentContract> studentContractList, String academicYear) {
//        Map<Months, Double> monthlyTotals = new HashMap<>();
//        Map<Months, Double> monthlyPaid = new HashMap<>();
//        Map<Months, Double> monthlyUnPaid = new HashMap<>();
//        Map<Months, Double> monthlyCutting = new HashMap<>();
//
//        // Initialize all months
//        for (Months month : Months.values()) {
//            monthlyTotals.put(month, 0.0);
//            monthlyPaid.put(month, 0.0);
//            monthlyCutting.put(month, 0.0);
//            monthlyUnPaid.put(month, 0.0);
//        }
//
//        for (StudentContract contract : studentContractList) {
//            List<MonthlyFee> feeList = monthlyFeeRepository.findAllByStudentContractIdAndYear(
//                    contract.getId(), academicYear);
//
//            // Group by month
//            Map<Months, List<MonthlyFee>> feesByMonth = feeList.stream()
//                    .collect(Collectors.groupingBy(MonthlyFee::getMonths));
//
//            for (Months month : Months.values()) {
//                List<MonthlyFee> monthFees = feesByMonth.getOrDefault(month, Collections.emptyList());
//
//                double monthTotal = monthFees.stream()
//                        .mapToDouble(MonthlyFee::getTotalFee)
//                        .sum();
//
//                double monthCut = monthFees.stream()
//                        .mapToDouble(MonthlyFee::getCutAmount)
//                        .sum();
//
//                double monthPaid = monthFees.stream()
//                        .mapToDouble(MonthlyFee::getAmountPaid)
//                        .sum();
//
//                monthlyTotals.merge(month, monthTotal - monthCut, Double::sum);
//                monthlyPaid.merge(month, monthPaid, Double::sum);
//                monthlyCutting.merge(month, monthCut, Double::sum);
//            }
//        }
//
//        // Calculate unpaid amounts
//        for (Months month : Months.values()) {
//            double unpaid = monthlyTotals.get(month) - monthlyPaid.get(month);
//            monthlyUnPaid.put(month, Math.max(0, unpaid));
//        }
//
//        // Build response
//        List<GetAmountByMonthResponse> responseList = Arrays.stream(Months.values())
//                .map(month -> new GetAmountByMonthResponse(
//                        month,
//                        monthlyTotals.get(month),
//                        monthlyPaid.get(month),
//                        monthlyUnPaid.get(month),
//                        monthlyCutting.get(month)
//                ))
//                .collect(Collectors.toList());
//
//        return ApiResponse.builder()
//                .status(HttpStatus.OK)
//                .message(RestConstants.SUCCESS)
//                .data(responseList)
//                .meta(Map.of(
//                        "type", "MONTHLY_BREAKDOWN",
//                        "totalStudents", studentContractList.size()))
//                .build();
//    }
//
//
//
//    private ApiResponse getYearlyStatistics(List<StudentContract> studentContractList, String academicYear) {
//        //yearly
//        double totalAmount = 0.0;
//        double totalPaid = 0.0;
//        double totalUnPaid = 0.0;
//        double totalCutting = 0.0;
//
//        for (StudentContract studentContract : studentContractList) {
//            List<MonthlyFee> feeList = this.monthlyFeeRepository.findAllByStudentContractIdAndYear(
//                    studentContract.getId(), academicYear);
//
//
//            double yearlyAmount = studentContract.getTariff().getAmount();
//            double cutAmount = feeList.stream().mapToDouble(MonthlyFee::getCutAmount).sum();
//            double finalYearlyAmount = yearlyAmount - cutAmount;
//            double paid = feeList.stream().mapToDouble(MonthlyFee::getAmountPaid).sum();
//
//            totalAmount += finalYearlyAmount;
//            totalPaid += paid;
//            totalUnPaid += finalYearlyAmount - paid;
//            totalCutting += cutAmount;
//        }
//        GetAmountByMonthResponse response = new GetAmountByMonthResponse();
//        response.setTotalAmount(totalAmount);
//        response.setPaidAmount(totalPaid);
//        response.setUnPaidAmount(totalUnPaid);
//        response.setCuttingAmount(totalCutting);
//
//        return ApiResponse.builder()
//                .status(HttpStatus.OK)
//                .message(SUCCESS)
//                .data(response)
//                .meta(Map.of(
//                        "TYPE", "YEARLY",
//                        "ACADEMIC_YEAR", academicYear,
//                        "TOTAL_STUDENTS", studentContractList.size()
//                ))
//                .build();
//    }
//    private ApiResponse getMonthlyStatistics(List<StudentContract> studentContractList, String academicYear) {
//
//        Map<Months, Double> monthlyTotals = new HashMap<>();
//        Map<Months, Double> monthlyPaid = new HashMap<>();
//        Map<Months, Double> monthlyUnPaid = new HashMap<>();
//        Map<Months, Double> monthlyCutting = new HashMap<>();
//
//        // Initialize all months with 0.0
//        for (Months months : Months.values()) {
//            monthlyTotals.put(months, 0.0);
//            monthlyPaid.put(months, 0.0);
//            monthlyCutting.put(months, 0.0);
//            monthlyUnPaid.put(months, 0.0);
//        }
//        for (StudentContract studentContract : studentContractList) {
//            List<MonthlyFee> feeList = this.monthlyFeeRepository.findAllByStudentContractIdAndYear(
//                    studentContract.getId(), academicYear);
//
//            double monthlyTariffAmount = studentContract.getTariff().getAmount();
//
//            // Group fees by month
//            Map<Months, List<MonthlyFee>> feesByMonth = feeList.stream()
//                    .collect(Collectors.groupingBy(MonthlyFee::getMonths));
//
//            for (Months month : Months.values()) {
//                List<MonthlyFee> monthFees = feesByMonth.getOrDefault(month, new ArrayList<>());
//
//                double monthCutAmount = monthFees.stream().mapToDouble(MonthlyFee::getCutAmount).sum();
//                double monthPaidAmount = monthFees.stream().mapToDouble(MonthlyFee::getAmountPaid).sum();
//
//                monthlyTotals.put(month, monthlyTotals.get(month) + monthlyTariffAmount - monthCutAmount);
//                monthlyPaid.put(month, monthlyPaid.get(month) + monthPaidAmount);
//                monthlyCutting.put(month, monthlyCutting.get(month) + monthCutAmount);
//                monthlyUnPaid.put(month, monthlyTotals.get(month) - monthlyPaid.get(month));
//            }
//        }
//        // Convert to response list
//        List<GetAmountByMonthResponse> responseList = new ArrayList<>();
//        for (Months month : Months.values()) {
//            double totalAmount = monthlyTotals.get(month);
//            double paidAmount = monthlyPaid.get(month);
//            double cuttingAmount = monthlyCutting.get(month);
//            double unPaidAmount = monthlyUnPaid.get(month);
//
//            GetAmountByMonthResponse response = new GetAmountByMonthResponse();
//            response.setMonth(month);
//            response.setTotalAmount(totalAmount);
//            response.setPaidAmount(paidAmount);
//            response.setUnPaidAmount(unPaidAmount);
//            response.setCuttingAmount(cuttingAmount);
//
//            responseList.add(response);
//        }
//
//        return ApiResponse.builder()
//                .status(HttpStatus.OK)
//                .message(RestConstants.SUCCESS)
//                .data(responseList)
//                .meta(Map.of(
//                        "type", "MONTHLY_BREAKDOWN",
//                        "totalStudents", studentContractList.size()))
//                .build();
//    }
//    private ApiResponse getQuarterlyStatisticsWithOverlap(List<StudentContract> studentContractList, String year) {
//        // Input validation
//        if (studentContractList == null || year == null || year.trim().isEmpty()) {
//            return ApiResponse.builder()
//                    .status(HttpStatus.BAD_REQUEST)
//                    .message("Invalid input: studentContractList or year is null/empty")
//                    .build();
//        }
//
//
//        // Define quarters with associated months
//        Map<String, List<Months>> quarters = Map.of(
//                "I-QUARTER", Arrays.asList(Months.SENTABR, Months.OKTABR, Months.NOYABR),
//                "II-QUARTER", Arrays.asList(Months.NOYABR, Months.DEKABR, Months.YANVAR),
//                "III-QUARTER", Arrays.asList(Months.FEVRAL, Months.MART, Months.APREL),
//                "IV-QUARTER", Arrays.asList(Months.APREL, Months.MAY, Months.IYUN)
//        );
//
//        // Initialize maps for totals
//        Map<String, Double> quarterlyTotals = new HashMap<>();
//        Map<String, Double> quarterlyPaid = new HashMap<>();
//        Map<String, Double> quarterlyCut = new HashMap<>();
//
//        // Initialize quarters
//        quarters.keySet().forEach(quarter -> {
//            quarterlyTotals.put(quarter, 0.0);
//            quarterlyPaid.put(quarter, 0.0);
//            quarterlyCut.put(quarter, 0.0);
//        });
//
//        for (StudentContract contract : studentContractList) {
//            // Fetch fees for the contract and year
//            List<MonthlyFee> feeList = Optional.ofNullable(
//                    monthlyFeeRepository.findAllByStudentContractIdAndYear(contract.getId(), year)
//            ).orElse(Collections.emptyList());
//
//            // Group fees by month
//            Map<Months, List<MonthlyFee>> feesByMonth = feeList.stream()
//                    .collect(Collectors.groupingBy(MonthlyFee::getMonths));
//
//            // Calculate total tariff and cut amounts per quarter
//            Map<String, Double> contractQuarterlyTotals = new HashMap<>();
//            Map<String, Double> contractQuarterlyCut = new HashMap<>();
//            quarters.keySet().forEach(quarter -> {
//                contractQuarterlyTotals.put(quarter, 0.0);
//                contractQuarterlyCut.put(quarter, 0.0);
//            });
//
//            // Calculate quarterly tariff amount
//            double quarterlyTariffAmount = contract.getTariff().getAmount();
//
//            for (Map.Entry<String, List<Months>> quarterEntry : quarters.entrySet()) {
//                String quarterName = quarterEntry.getKey();
//                List<Months> quarterMonths = quarterEntry.getValue();
//
//                double quarterTotalAmount = 0.0;
//                double quarterCutAmount = 0.0;
//
//                for (Months month : quarterMonths) {
//                    // Count quarters containing this month
//                    long quartersContainingMonth = quarters.values().stream()
//                            .filter(months -> months.contains(month))
//                            .count();
//
//                    // Calculate monthly tariff based on tariff type
//                    double monthlyTariffAmount = calculateMonthlyTariff(
//                            contract.getTariff().getTariffStatus(),
//                            quarterlyTariffAmount
//                    );
//
//                    // Divide amounts by number of quarters the month appears in
//                    double monthTotalForQuarter = monthlyTariffAmount / quartersContainingMonth;
//
//                    // Sum cut amounts for the month
//                    List<MonthlyFee> monthFees = feesByMonth.getOrDefault(month, Collections.emptyList());
//                    double monthCut = monthFees.stream()
//                            .mapToDouble(MonthlyFee::getCutAmount)
//                            .sum() / quartersContainingMonth;
//
//                    quarterTotalAmount += monthTotalForQuarter;
//                    quarterCutAmount += monthCut;
//                }
//
//                contractQuarterlyTotals.put(quarterName, quarterTotalAmount);
//                contractQuarterlyCut.put(quarterName, quarterCutAmount);
//            }
//
//            // Sum total paid amount for the contract across all months
//            double totalPaidForContract = feeList.stream()
//                    .mapToDouble(MonthlyFee::getAmountPaid)
//                    .sum();
//
//            // Allocate payments sequentially to quarters
//            double remainingPaid = totalPaidForContract;
//            Map<String, Double> contractQuarterlyPaid = new HashMap<>();
//            quarters.keySet().forEach(quarter -> contractQuarterlyPaid.put(quarter, 0.0));
//
//            String[] quarterOrder = {"I-QUARTER", "II-QUARTER", "III-QUARTER", "IV-QUARTER"};
//            for (String quarterName : quarterOrder) {
//                double netAmount = contractQuarterlyTotals.get(quarterName) - contractQuarterlyCut.get(quarterName);
//                double paidForQuarter = Math.min(remainingPaid, netAmount);
//                contractQuarterlyPaid.put(quarterName, paidForQuarter);
//                remainingPaid -= paidForQuarter;
//            }
//
//            // Update global quarterly totals
//            for (String quarterName : quarters.keySet()) {
//                quarterlyTotals.merge(quarterName, contractQuarterlyTotals.get(quarterName), Double::sum);
//                quarterlyPaid.merge(quarterName, contractQuarterlyPaid.get(quarterName), Double::sum);
//                quarterlyCut.merge(quarterName, contractQuarterlyCut.get(quarterName), Double::sum);
//            }
//        }
//
//        // Convert to response list in defined order
//        List<GetAmountByMonthResponse> responseList = new ArrayList<>();
//        String[] quarterOrder = {"I-QUARTER", "II-QUARTER", "III-QUARTER", "IV-QUARTER"};
//
//        for (String quarterName : quarterOrder) {
//            double totalAmount = quarterlyTotals.get(quarterName);
//            double cutAmount = quarterlyCut.get(quarterName);
//            double paidAmount = quarterlyPaid.get(quarterName);
//            double netAmount = totalAmount - cutAmount;
//
//            GetAmountByMonthResponse response = new GetAmountByMonthResponse();
//            response.setTotalAmount(netAmount);
//            response.setPaidAmount(paidAmount);
//            response.setUnPaidAmount(netAmount - paidAmount);
//            response.setCuttingAmount(cutAmount);
//
//            responseList.add(response);
//        }
//
//        return ApiResponse.builder()
//                .status(HttpStatus.OK)
//                .message(RestConstants.SUCCESS)
//                .data(responseList)
//                .meta(Map.of(
//                        "type", "QUARTERLY_BREAKDOWN",
//                        "totalStudents", studentContractList.size()
//                ))
//                .build();
//    }
//    private ApiResponse getQuarterlyStatisticsWithOverlap(List<StudentContract> studentContractList, String year) {
//        Map<String, List<Months>> quarters = Map.of(
//                "I-QUARTER", Arrays.asList(Months.SENTABR, Months.OKTABR, Months.NOYABR),
//                "II-QUARTER", Arrays.asList(Months.NOYABR, Months.DEKABR, Months.YANVAR),
//                "III-QUARTER", Arrays.asList(Months.FEVRAL, Months.MART, Months.APREL),
//                "IV-QUARTER", Arrays.asList(Months.APREL, Months.MAY, Months.IYUN)
//        );
//
//        Map<String, Double> quarterlyTotals = new HashMap<>();
//        Map<String, Double> quarterlyPaid = new HashMap<>();
//        Map<String, Double> quarterlyCutting = new HashMap<>();
//
//        // Initialize quarters
//        for (String quarter : quarters.keySet()) {
//            quarterlyTotals.put(quarter, 0.0);
//            quarterlyPaid.put(quarter, 0.0);
//            quarterlyCutting.put(quarter, 0.0);
//        }
//
//        for (StudentContract studentContract : studentContractList) {
//            List<MonthlyFee> feeList = this.monthlyFeeRepository.findAllByStudentContractIdAndYear(
//                    studentContract.getId(), year);
//
//            // Quarterly tariff amount is 6,250,000 som
//            double quarterlyTariffAmount = studentContract.getTariff().getAmount(); // 6,250,000
//
//            // Group fees by month
//            Map<Months, List<MonthlyFee>> feesByMonth = feeList.stream()
//                    .collect(Collectors.groupingBy(MonthlyFee::getMonths));
//
//            // Process each quarter
//            for (Map.Entry<String, List<Months>> quarterEntry : quarters.entrySet()) {
//                String quarterName = quarterEntry.getKey();
//                List<Months> quarterMonths = quarterEntry.getValue();
//
//                double quarterCutAmount = 0.0;
//                double quarterPaidAmount = 0.0;
//
//                // For overlapping months, divide the amounts properly
//                for (Months month : quarterMonths) {
//                    List<MonthlyFee> monthFees = feesByMonth.getOrDefault(month, new ArrayList<>());
//
//                    if (!monthFees.isEmpty()) {
//                        // Count how many quarters this month appears in
//                        long quartersContainingMonth = quarters.values().stream()
//                                .mapToLong(months -> months.contains(month) ? 1 : 0)
//                                .sum();
//
//                        // Divide BOTH amounts by number of quarters the month appears in
//                        double monthCut = monthFees.stream().mapToDouble(MonthlyFee::getCutAmount).sum();
//                        double monthPaid = monthFees.stream().mapToDouble(MonthlyFee::getAmountPaid).sum() / quartersContainingMonth;
//
//                        quarterCutAmount += monthCut;
//                        quarterPaidAmount += monthPaid;
//                    }
//                }
//
//                // Add to quarter totals
//                quarterlyTotals.put(quarterName, quarterlyTotals.get(quarterName) + quarterlyTariffAmount);
//                quarterlyPaid.put(quarterName, quarterlyPaid.get(quarterName) + quarterPaidAmount);
//                quarterlyCutting.put(quarterName, quarterlyCutting.get(quarterName) + quarterCutAmount);
//            }
//        }
//
//        // Convert to response list
//        List<GetAmountByMonthResponse> responseList = new ArrayList<>();
//        String[] quarterOrder = {"I-QUARTER", "II-QUARTER", "III-QUARTER", "IV-QUARTER"};
//
//        for (String quarterName : quarterOrder) {
//            double totalAmount = quarterlyTotals.get(quarterName);
//            double cuttingAmount = quarterlyCutting.get(quarterName);
//            double paidAmount = quarterlyPaid.get(quarterName);
//            double netAmount = totalAmount - cuttingAmount; // Net amount after cuts
//
//            GetAmountByMonthResponse response = new GetAmountByMonthResponse();
//            response.setTotalAmount(netAmount); // 6,250,000 - 250,000 = 6,000,000
//            response.setPaidAmount(paidAmount);                    // 6,000,000
//            response.setUnPaidAmount(netAmount - paidAmount);      // 6,000,000 - 6,000,000 = 0
//            response.setCuttingAmount(cuttingAmount);              // 250,000
//
//            responseList.add(response);
//        }
//
//        return ApiResponse.builder()
//                .status(HttpStatus.OK)
//                .message(RestConstants.SUCCESS)
//                .data(responseList)
//                .meta(Map.of(
//                        "type", "QUARTERLY_BREAKDOWN",
//                        "totalStudents", studentContractList.size()
//                ))
//                .build();
//    }


