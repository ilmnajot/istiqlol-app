package org.example.moliyaapp.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.example.moliyaapp.dto.ApiResponse;
import org.example.moliyaapp.dto.StudentContractDto;
import org.example.moliyaapp.dto.TeacherTableDto;
import org.example.moliyaapp.entity.*;
import org.example.moliyaapp.enums.*;
import org.example.moliyaapp.exception.ResourceNotFoundException;
import org.example.moliyaapp.filter.TeacherTabelSheetFilter;
import org.example.moliyaapp.filter.TeacherTableFilter;
import org.example.moliyaapp.mapper.TeacherTableMapper;
import org.example.moliyaapp.repository.*;
import org.example.moliyaapp.service.*;
import org.example.moliyaapp.utils.RestConstants;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@RequiredArgsConstructor
@Service
public class TeacherTableServiceImpl implements TeacherTableService {

    private static final String XODIMGA_OYLIK = "XODIMGA_OYLIK";

    private final TeacherTableRepository teacherTableRepository;
    private final TeacherTableMapper teacherTableMapper;
    private final UserRepository userRepository;
    private final TeacherContractRepository teacherContractRepository;
    private final ExtraLessonPriceRepository extraLessonPriceRepository;
    private final MonthlyFeeRepository monthlyFeeRepository;
    private final CompanyRepository companyRepository;
    private final CategoryRepository categoryRepository;
    private final TransactionRepository transactionRepository;
    private final ExpensesRepository expensesRepository;
    private final EmployeeGoogleSheet employeeGoogleSheet;
    private final EmployeeTransactionGoogleSheet employeeTransactionGoogleSheet;
    private final EmployeeTabelSheet employeeTabelSheet;
    private final EmployeeMonthlyFeeGoogleSheet employeeMonthlyFeeGoogleSheet;

    @Override
    public ApiResponse addTeacherTable(TeacherTableDto.CreateTeacherTableDto dto) {

        User employee = this.userRepository.findByIdAndDeletedFalse(dto.getTeacherId())
                .orElseThrow(() -> new ResourceNotFoundException(RestConstants.TEACHER_NOT_FOUND));

        Set<UserRole> roles = employee.getRole();
        ExtraLessonPrice extraLessonPrice = null;
        boolean match = roles.stream().anyMatch(role -> role.getName().equals(Role.TEACHER.name()));
        if (match) {
            extraLessonPrice = this.extraLessonPriceRepository
                    .findByIdAndDeletedFalse(dto.getExtraLessonPriceId())
                    .orElseThrow(() -> new ResourceNotFoundException("QO'SHIMCHA DARS NARXI TOPILMADI"));
        }

        double fixedAmount = extraLessonPrice != null
                ? extraLessonPrice.getFixedAmount()
                : 0.0;
        TeacherContract contract = this.teacherContractRepository
                .findByTeacherIdAndDeletedFalse(dto.getTeacherId())
                .orElseThrow(() -> new ResourceNotFoundException(RestConstants.CONTRACT_NOT_FOUND));

        int year = LocalDateTime.now().getYear();

        Optional<TeacherTable> existingTable = this.teacherTableRepository.existByTeacherIdAndMonthAndYear(
                dto.getTeacherId(),
                dto.getMonths().name(),
                year);

        if (existingTable.isPresent()) {
            return ApiResponse.builder()
                    .status(HttpStatus.BAD_REQUEST)
                    .message(RestConstants.TABLE_ALREADY_EXISTS_FOR_THIS_MONTH)
                    .build();
        }

        TeacherTable entity = this.teacherTableMapper.toEntity(dto);
        entity.setTeacher(employee);
        entity.setTeacherContract(contract);
        if (dto.getSalary() == null) {
            entity.setMonthlySalary(contract.getMonthlySalaryOrPerLessonOrPerDay());
        } else {
            entity.setMonthlySalary(dto.getSalary());
        }

        if (dto.getWorkDaysOrLessons() < 0 || dto.getWorkedDaysOrLessons() < 0) {
            return ApiResponse.builder()
                    .status(HttpStatus.BAD_REQUEST)
                    .message(RestConstants.INVALID_NUMBER)
                    .build();
        }
        int workDaysOrLessons = dto.getWorkDaysOrLessons();
        int workedDaysOrLessons = dto.getWorkedDaysOrLessons();
        int extraWorked = dto.getExtraWorkedDaysOrLessons() != null ? dto.getExtraWorkedDaysOrLessons() : 0;

        SalaryType salaryType = contract.getSalaryType();

        double salary = dto.getSalary() != null ? dto.getSalary() : contract.getMonthlySalaryOrPerLessonOrPerDay();

        double finalAmount;
        double extraLessonAmount;

        if (salaryType == SalaryType.PRICE_PER_LESSON) {
            extraLessonAmount = extraWorked * fixedAmount;
            finalAmount = workedDaysOrLessons * salary;
            entity.setAmount(roundTo1000(finalAmount) + extraLessonAmount);

        } else if (salaryType == SalaryType.PRICE_PER_MONTH) {
            extraLessonAmount = extraWorked * fixedAmount;
            finalAmount = roundTo1000(salary / workDaysOrLessons * workedDaysOrLessons);
            entity.setAmount(finalAmount + extraLessonAmount);
        } else if (salaryType == SalaryType.PRICE_PER_DAY) {
            finalAmount = salary * workedDaysOrLessons;
            entity.setAmount(roundTo1000(finalAmount));
        }
        entity.setExtraLessonPrice(extraLessonPrice);


        TeacherTable teacherTable = this.teacherTableRepository.save(entity);

        try {
            employeeTabelSheet.initializeSheet();
            this.employeeTabelSheet.recordEmployeeTabel(teacherTable);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        MonthlyFee fee;
        // Handle MonthlyFee logic
        Optional<MonthlyFee> feeOptional = this.monthlyFeeRepository.findByEmployeeIdAndMonthsAndYear(
                employee.getId(), dto.getMonths().lastMonth().name(), year);

        if (feeOptional.isPresent() && feeOptional.get().getRemainingBalance() < 0) {
            MonthlyFee last = feeOptional.get();
            Double remainingBalance = last.getRemainingBalance();
            teacherTable.setAmount(teacherTable.getAmount() + remainingBalance);
            last.setRemainingBalance(0.0);
            this.monthlyFeeRepository.save(last);
            log.info("tabel amount :  {}", teacherTable.getAmount());
        }

        // Handle MonthlyFee logic
        Optional<MonthlyFee> thisMonthFee = this.monthlyFeeRepository.findByEmployeeIdAndMonthsAndYear(
                employee.getId(), dto.getMonths().name(), year);

        if (thisMonthFee.isPresent() && thisMonthFee.get().getIsAdvanced()) {
            fee = thisMonthFee.get();
            Double amountPaid = fee.getAmountPaid();
            double diff = teacherTable.getAmount() - amountPaid;
            if (diff < 0) {
                fee.setRemainingBalance(diff);
                this.teacherTableRepository.save(teacherTable);
            }
        }


        log.info("finalAmount + {}", teacherTable.getAmount());
        return ApiResponse.builder()
                .status(HttpStatus.OK)
                .message(RestConstants.SUCCESSFULLY_SAVED)
                .build();
    }

    @Override
    public ApiResponse deleteTeacherTable(Long id) {
        TeacherTable teacherTable = this.teacherTableRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(RestConstants.TEACHER_NOT_FOUND));
        teacherTable.setDeleted(true);
        this.teacherTableRepository.save(teacherTable);
        return ApiResponse.builder()
                .status(HttpStatus.OK)
                .message(RestConstants.SUCCESSFULLY_DELETED)
                .build();
    }

    @Override
    public ApiResponse getTeacherTableById(Long id) {
        TeacherTable teacherTable = this.teacherTableRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(RestConstants.TABEL_NOT_FOUND));
        TeacherTableDto teacherTableDto = this.teacherTableMapper.toDto(teacherTable);
        return ApiResponse.builder()
                .message(RestConstants.SUCCESS)
                .status(HttpStatus.OK)
                .data(teacherTableDto)
                .build();
    }

    @Override
    public ApiResponse getAllTeacherTableList() {
        List<TeacherTable> tableList = this.teacherTableRepository.findAllByDeletedFalse();
        List<TeacherTableDto> dtoList = this.teacherTableMapper.toDto(tableList);
        return ApiResponse.builder()
                .status(HttpStatus.OK)
                .message(RestConstants.SUCCESS)
                .data(dtoList)
                .build();
    }

    @Override
    public ApiResponse getAllTeacherTablePage(Pageable pageable) {
        Page<TeacherTable> tablePage = this.teacherTableRepository.findAllByDeletedFalsePage(pageable);
        if (tablePage.isEmpty()) {
            return ApiResponse.builder()
                    .status(HttpStatus.OK)
                    .message(RestConstants.SUCCESS)
                    .data(new ArrayList<>())
                    .build();
        }
        List<TeacherTableDto> dtoList = this.teacherTableMapper.toDto(tablePage.getContent());
        return ApiResponse.builder()
                .status(HttpStatus.OK)
                .message(RestConstants.SUCCESS)
                .data(dtoList)
                .pages(tablePage.getTotalPages())
                .elements(tablePage.getTotalElements())
                .build();
    }


    @Transactional
    @Override
    public ApiResponse updateTeacherTable(Long id, TeacherTableDto.CreateTeacherTableDto dto) {
        // Fetch existing entities
        TeacherTable teacherTable = teacherTableRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException(RestConstants.TABEL_NOT_FOUND));

        User employee = userRepository.findByIdAndDeletedFalse(dto.getTeacherId())
                .orElseThrow(() -> new ResourceNotFoundException(RestConstants.TEACHER_NOT_FOUND));

        TeacherContract teacherContract = teacherContractRepository.findByTeacherIdAndDeletedFalse(dto.getTeacherId())
                .orElseThrow(() -> new ResourceNotFoundException(RestConstants.CONTRACT_NOT_FOUND));

        // Handle extra lesson price for teachers
        ExtraLessonPrice extraLessonPrice = null;
        Set<UserRole> roles = employee.getRole();
        boolean isTeacher = roles.stream().anyMatch(role -> role.getName().equals(Role.TEACHER.name()));
        if (isTeacher && dto.getExtraLessonPriceId() != null) {
            extraLessonPrice = extraLessonPriceRepository.findByIdAndDeletedFalse(dto.getExtraLessonPriceId())
                    .orElseThrow(() -> new ResourceNotFoundException("QO'SHIMCHA DARS NARXI TOPILMADI"));
        }

        // Update fields
        teacherTable.setTeacher(employee);
        teacherTable.setTeacherContract(teacherContract);

        if (dto.getWorkDaysOrLessons() != null) {
            teacherTable.setWorkDaysOrLessons(dto.getWorkDaysOrLessons());
        }
        if (dto.getWorkedDaysOrLessons() != null) {
            teacherTable.setWorkedDaysOrLessons(dto.getWorkedDaysOrLessons());
        }
        if (dto.getExtraWorkedDaysOrLessons() != null) {
            teacherTable.setExtraWorkedDaysOrLessons(dto.getExtraWorkedDaysOrLessons());
        }
        if (dto.getDescription() != null) {
            teacherTable.setDescription(dto.getDescription());
        }
        if (dto.getMonths() != null) {
            teacherTable.setMonths(dto.getMonths());
        }

        // Handle salary - use contract salary if not provided in DTO
        Double salary = dto.getSalary() != null
                ? dto.getSalary()
                : teacherContract.getMonthlySalaryOrPerLessonOrPerDay();
        teacherTable.setMonthlySalary(salary);

        // Calculate missed days/lessons
        int workDaysOrLessons = teacherTable.getWorkDaysOrLessons();
        int workedDaysOrLessons = teacherTable.getWorkedDaysOrLessons();
        teacherTable.setMissedWorkDaysOrLessons(workDaysOrLessons - workedDaysOrLessons);

        // Calculate final amount
        SalaryType salaryType = teacherContract.getSalaryType();
        double fixedAmount = extraLessonPrice != null ? extraLessonPrice.getFixedAmount() : 0;
        int extraWorked = teacherTable.getExtraWorkedDaysOrLessons() != null ? teacherTable.getExtraWorkedDaysOrLessons() : 0;
        double finalAmount = 0;
        double extraLessonAmount = 0;


        if (salaryType == SalaryType.PRICE_PER_LESSON) {
            extraLessonAmount = extraWorked * fixedAmount;
            finalAmount = workedDaysOrLessons * salary;
            teacherTable.setAmount(roundTo1000(finalAmount) + extraLessonAmount);
        } else if (salaryType == SalaryType.PRICE_PER_MONTH) {
            finalAmount = (salary / workDaysOrLessons) * workedDaysOrLessons;
            extraLessonAmount = extraWorked * fixedAmount;
            teacherTable.setAmount(roundTo1000(finalAmount) + extraLessonAmount);
        } else if (salaryType == SalaryType.PRICE_PER_DAY) {
            finalAmount = salary * workedDaysOrLessons;
            teacherTable.setAmount(roundTo1000(finalAmount));
        }
        teacherTable.setExtraLessonPrice(extraLessonPrice);

        Optional<MonthlyFee> optionalMonthlyFee = this.monthlyFeeRepository.findByEmployeeIdAndMonthsAndYear(employee.getId(), dto.getMonths().lastMonth().name(), LocalDateTime.now().getYear());
        if (optionalMonthlyFee.isPresent()) {
            Optional<TeacherTable> tableOptional = this.teacherTableRepository.findByTeacherIdAndMonthAndYear(employee.getId(), dto.getMonths().lastMonth(), LocalDateTime.now().getYear());
            if (tableOptional.isPresent()) {
                MonthlyFee fee = optionalMonthlyFee.get();
                Double amountPaid = fee.getAmountPaid();
                TeacherTable table = tableOptional.get();
                Double amount = table.getAmount();
                double difference = amount - amountPaid;
                finalAmount += difference;
                teacherTable.setAmount(roundTo1000(finalAmount));
            }
        }

        int year = LocalDateTime.now().getYear();
        MonthlyFee fee;
        Optional<MonthlyFee> feeOptional = this.monthlyFeeRepository.findByEmployeeIdAndMonthsAndYear(employee.getId(), dto.getMonths().name(), year);
        if (feeOptional.isPresent()) {
            fee = feeOptional.get();
            Double amountPaid = fee.getAmountPaid();
            double diff = teacherTable.getAmount() - amountPaid;
            fee.setRemainingBalance(roundTo1000(diff));
            if (fee.getIsAdvanced()) {
                fee.setTotalFee(finalAmount);
                fee.setRemainingBalance(roundTo1000(finalAmount - fee.getAmountPaid()) + extraLessonAmount); //todo
            }
            this.monthlyFeeRepository.save(fee);
        }

        TeacherTable saved = teacherTableRepository.save(teacherTable);
        try {
            this.employeeTabelSheet.initializeSheet();
            this.employeeTabelSheet.updateEmployeeTabel(saved);
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        TeacherTableDto teacherTableDto = teacherTableMapper.toDto(saved);


        return ApiResponse.builder()
                .status(HttpStatus.OK)
                .message(RestConstants.SUCCESSFULLY_UPDATED)
                .data(teacherTableDto)
                .build();
    }


    @Override
    public ApiResponse getByTeacherIdAndMonth(Long teacherId, Months month) {
        List<TeacherTable> tableList = this.teacherTableRepository.findByTeacherIdAndMonth(teacherId, month);
        List<TeacherTableDto> dtoList = this.teacherTableMapper.toDto(tableList);
        return ApiResponse.builder()
                .status(HttpStatus.OK)
                .message(RestConstants.SUCCESS)
                .data(dtoList)
                .build();
    }

    @Override
    public ApiResponse getAllTeacherTableByMonth(Months month, Integer year) {
        List<TeacherTable> teacherTable = this.teacherTableRepository.findAllMonthsAndYear(month, year);
        return ApiResponse.builder()
                .status(HttpStatus.OK)
                .message("SUCCESS")
                .data(this.teacherTableMapper.toDto(teacherTable))
                .build();
    }

    @Override
    public ApiResponse filter(Pageable pageable, TeacherTableFilter filter) {
        Page<TeacherTable> tablePage = this.teacherTableRepository.findAll(filter, pageable);
        if (tablePage.isEmpty()) {
            return ApiResponse.builder()
                    .status(HttpStatus.OK)
                    .message(RestConstants.SUCCESS)
                    .data(new ArrayList<>())
                    .build();
        }
        return ApiResponse.builder()
                .status(HttpStatus.OK)
                .message(RestConstants.SUCCESS)
                .data(this.teacherTableMapper.toDto(tablePage.getContent()))
                .pages(tablePage.getTotalPages())
                .elements(tablePage.getTotalElements())
                .build();
    }

    //true : all except employee and teacher
    @Override
    public ApiResponse getAllUsersByRole(Role role, Pageable pageable, Boolean otherRoles) {

        Set<String> teacherAndEmployee = Set.of("TEACHER", "EMPLOYEE");

        Page<TeacherTable> teacherTables;

        if (role != null) {
            teacherTables = this.teacherTableRepository.findAllUsersByRole(role.name(), pageable);
        } else if (Boolean.TRUE.equals(otherRoles)) {
            teacherTables = this.teacherTableRepository.findAllByRolesNotIn(teacherAndEmployee, pageable);
        } else {
            teacherTables = this.teacherTableRepository.findAllByRolesIn(teacherAndEmployee, pageable);

        }

        if (teacherTables.isEmpty()) {
            return ApiResponse.builder()
                    .status(HttpStatus.OK)
                    .data(new ArrayList<>())
                    .build();
        }
        return ApiResponse.builder()
                .status(HttpStatus.OK)
                .message(RestConstants.SUCCESS)
                .data(this.teacherTableMapper.toDto(teacherTables.getContent()))
                .elements(teacherTables.getTotalElements())
                .pages(teacherTables.getTotalPages())
                .build();
    }

    @Override
    public ApiResponse getAllByMonth(Months months, Pageable pageable) {
        Page<TeacherTable> contractPage = this.teacherTableRepository.findAllByMonths(months, pageable);

        return ApiResponse.builder()
                .status(HttpStatus.OK)
                .data(this.teacherTableMapper.toDto(contractPage.getContent()))
                .build();
    }

    @Override
    public ApiResponse searchByName(String keyword, Pageable pageable) {
        Page<TeacherTable> tablePage;
        if (keyword == null || keyword.trim().isEmpty()) {
            tablePage = this.teacherTableRepository.findAllActiveTeacherTables(pageable);
        } else {
            tablePage = this.teacherTableRepository.searchByName(keyword, pageable);

        }
        return ApiResponse.builder()
                .status(HttpStatus.OK)
                .data(this.teacherTableMapper.toDto(tablePage.getContent()))
                .pages(tablePage.getTotalPages())
                .elements(tablePage.getTotalElements())
                .build();
    }

    @Transactional
    @Override
    public ApiResponse deleteConctracts(List<Long> ids) {
        List<Long> teacherTableList = this.teacherTableRepository.findAllByIdsAndDeletedTrue(ids);
        if (teacherTableList.isEmpty()) {
            return ApiResponse.builder()
                    .status(HttpStatus.OK)
                    .message("O'CHIRISH UCHUN TABELLAR TOPILMADI!")
                    .build();
        }
        this.teacherTableRepository.deleteAllByIds(teacherTableList);
        return ApiResponse.builder()
                .status(HttpStatus.OK)
                .message("TANLANGAN TABELLAR BUTUNLAYGA O'CHIRILDI!")
                .build();
    }

    @Transactional
    @Override
    public ApiResponse payForCash(Long tableId, Double amount, Double bonus, String comment) {

        if (amount < 0 || bonus < 0) {
            return ApiResponse.builder()
                    .status(HttpStatus.CONFLICT)
                    .message(RestConstants.INVALID_AMOUNT)
                    .build();
        }

        TeacherTable table = this.teacherTableRepository.findByIdAndDeletedFalse(tableId)
                .orElseThrow(() -> new ResourceNotFoundException(RestConstants.TABEL_NOT_FOUND));

        User employee = table.getTeacher();
        Months months = table.getMonths();

        Category category = this.categoryRepository.findByName(XODIMGA_OYLIK)
                .orElseThrow(() -> new ResourceNotFoundException(RestConstants.CATEGORY_NOT_FOUND));


        Company company = companyRepository.findOne()
                .orElseThrow(() -> new ResourceNotFoundException(RestConstants.COMPANY_NOT_FOUND));

        double cashBalance = company.getCashBalance() != null ? company.getCashBalance() : 0.0;

        double finalAmount = cashBalance - (amount + bonus);

        if (finalAmount < 0) {
            return ApiResponse.builder()
                    .status(HttpStatus.CONFLICT)
                    .message(RestConstants.NOT_ENOUGH_CASH_BALANCE)
                    .build();
        }
        company.setCashBalance(finalAmount);
        this.companyRepository.save(company);


        MonthlyFee fee;
        Optional<MonthlyFee> monthlyFee = monthlyFeeRepository.findByEmployeeIdAndMonths(employee.getId(), months);
        if (monthlyFee.isEmpty()) {
            fee = new MonthlyFee();
            fee.setEmployee(employee);
            fee.setMonths(months);
            fee.setTotalFee(table.getAmount());
            fee.setAmountPaid(amount);
            fee.setRemainingBalance(table.getAmount() - amount);
            fee.setCategory(category);
            fee.setCompany(company);
            fee.setBonus(bonus);

            if (amount.equals(table.getAmount())) {
                fee.setStatus(PaymentStatus.FULLY_PAID);
            }
            fee.setStatus(PaymentStatus.PARTIALLY_PAID);

            fee = monthlyFeeRepository.save(fee);
            try {
                this.employeeMonthlyFeeGoogleSheet.initializeSheet();
                this.employeeMonthlyFeeGoogleSheet.recordEmployeeMonthlyFee(fee);
            } catch (Exception e) {
                log.error(e.getMessage());
            }
        } else {
            fee = monthlyFee.get();
            fee.setAmountPaid(fee.getAmountPaid() + amount);
            fee.setBonus((fee.getBonus() != null ? fee.getBonus() : 0) + bonus);
            fee.setMonths(months);
            fee.setRemainingBalance(fee.getRemainingBalance() - amount);

            if (fee.getRemainingBalance() <= 0 ||
                    (fee.getAmountPaid() >= fee.getTotalFee())) {
                fee.setStatus(PaymentStatus.FULLY_PAID);
            } else if (fee.getAmountPaid() > 0) {
                fee.setStatus(PaymentStatus.PARTIALLY_PAID);
            } else {
                fee.setStatus(PaymentStatus.UNPAID);
            }

            fee = this.monthlyFeeRepository.save(fee);
            try {
                this.employeeMonthlyFeeGoogleSheet.initializeSheet();
                this.employeeMonthlyFeeGoogleSheet.updateEmployeeMonthlyFee(fee);
            } catch (Exception e) {
                log.error(e.getMessage());
            }

        }
        if (amount > 0) {

            log.debug("bonus {}", bonus);
            log.debug("amount {}", amount);

            Transaction salaryTransaction = new Transaction();
            salaryTransaction.setTransactionType(TransactionType.OUTCOME);
            salaryTransaction.setPaymentType(PaymentType.NAQD);
            salaryTransaction.setMonthlyFee(fee);
            salaryTransaction.setAmount(amount);
            salaryTransaction.setCompany(company);
            salaryTransaction.setDescription("ASOSIY_TO'LOV_TRANSAKSIYASI: " + comment);
            salaryTransaction = transactionRepository.save(salaryTransaction);
            try {
                this.employeeTransactionGoogleSheet.initializeSheet();
                this.employeeTransactionGoogleSheet.recordEmployeeTransactions(salaryTransaction);
            } catch (Exception e) {
                log.error("Error recording employee transaction to Google Sheets: {}", e.getMessage());
            }

        }
        if (bonus > 0) {
            Transaction bonusTransaction = new Transaction();
            bonusTransaction.setTransactionType(TransactionType.OUTCOME);
            bonusTransaction.setPaymentType(PaymentType.NAQD);
            bonusTransaction.setMonthlyFee(fee);
            bonusTransaction.setAmount(bonus);
            bonusTransaction.setCompany(company);
            bonusTransaction.setDescription("BONUS_TO'LOV_TRANSAKSIYASI: " + comment);
            bonusTransaction = transactionRepository.save(bonusTransaction);
            try {
                this.employeeTransactionGoogleSheet.initializeSheet();
                this.employeeTransactionGoogleSheet.recordEmployeeTransactions(bonusTransaction);
            } catch (Exception e) {
                log.error("Error recording employee transaction to Google Sheets: {}", e.getMessage());
            }
        }

        return ApiResponse.builder()
                .status(HttpStatus.CREATED)
                .message(RestConstants.SUCCESSFULLY_PAID)
                .build();
    }

    @Transactional
    @Override
    public ApiResponse payAdvanceForCash(Long employeeContractId, Double amount, Months month, String comment, Boolean isAdvanced) { //todo

        if (isAdvanced == null || !isAdvanced || month == null) {
            return ApiResponse.builder()
                    .status(HttpStatus.BAD_REQUEST)
                    .message("AVANS BERISHN TANLANMAGAN!")
                    .build();
        }
        if (amount < 0) {
            return ApiResponse.builder()
                    .status(HttpStatus.BAD_REQUEST)
                    .message(RestConstants.INVALID_AMOUNT)
                    .build();
        }

        TeacherContract employeeContract = this.teacherContractRepository.findByIdAndDeletedFalse(employeeContractId)
                .orElseThrow(() -> new ResourceNotFoundException(RestConstants.CONTRACT_NOT_FOUND));

        User employee = employeeContract.getTeacher();
        Double monthlySalaryOrPerLessonOrPerDay = employeeContract.getMonthlySalaryOrPerLessonOrPerDay();

        double leftAmount = monthlySalaryOrPerLessonOrPerDay - amount;

        if (leftAmount < 0) {
            return ApiResponse.builder()
                    .status(HttpStatus.BAD_REQUEST)
                    .message(RestConstants.INVALID_AMOUNT)
                    .build();
        }

        Category category = this.categoryRepository.findByName(XODIMGA_OYLIK)
                .orElseThrow(() -> new ResourceNotFoundException(RestConstants.CATEGORY_NOT_FOUND));

        Company company = companyRepository.findOne()
                .orElseThrow(() -> new ResourceNotFoundException(RestConstants.COMPANY_NOT_FOUND));
        double cashBalance = company.getCashBalance() != null ? company.getCashBalance() : 0.0;
        double finalAmount = cashBalance - amount;
        if (finalAmount < 0) {
            return ApiResponse.builder()
                    .status(HttpStatus.BAD_REQUEST)
                    .message(RestConstants.NOT_ENOUGH_CASH_BALANCE)
                    .build();
        }
        company.setCashBalance(finalAmount);
        this.companyRepository.save(company);
        MonthlyFee fee;
        Optional<MonthlyFee> feeOptional = this.monthlyFeeRepository.findByEmployeeIdAndMonths(employee.getId(), month);
        if (feeOptional.isEmpty()) {
            fee = new MonthlyFee();
            fee.setEmployee(employee);
            fee.setMonths(month);
            fee.setTotalFee(monthlySalaryOrPerLessonOrPerDay);
            fee.setAmountPaid(amount);
            fee.setRemainingBalance(leftAmount);
            try {
                employeeMonthlyFeeGoogleSheet.initializeSheet();
                this.employeeMonthlyFeeGoogleSheet.recordEmployeeMonthlyFee(fee);
            } catch (Exception e) {
                log.error(e.getMessage());
            }
        } else {
            fee = feeOptional.get();
            Double amountPaid = amount + fee.getAmountPaid();
            if (monthlySalaryOrPerLessonOrPerDay - amountPaid < 0) {
                return ApiResponse.builder()
                        .status(HttpStatus.BAD_REQUEST)
                        .message(RestConstants.INVALID_AMOUNT)
                        .build();
            }
            fee.setTotalFee(fee.getTotalFee() + amount);
            fee.setAmountPaid(amountPaid);
            fee.setRemainingBalance(fee.getRemainingBalance() - amount);
            fee.setMonths(month);
            if (amountPaid.equals(employeeContract.getMonthlySalaryOrPerLessonOrPerDay())) {
                fee.setStatus(PaymentStatus.FULLY_PAID);
            } else {
                fee.setStatus(PaymentStatus.PARTIALLY_PAID);
            }
        }
        fee.setCategory(category);
        fee.setCompany(company);
        fee.setIsAdvanced(isAdvanced);


        if (amount.equals(employeeContract.getMonthlySalaryOrPerLessonOrPerDay())) {
            fee.setStatus(PaymentStatus.FULLY_PAID);
        } else {
            fee.setStatus(PaymentStatus.PARTIALLY_PAID);
        }

        monthlyFeeRepository.save(fee);
        fee = this.monthlyFeeRepository.save(fee);

        try {
            employeeMonthlyFeeGoogleSheet.initializeSheet();
            this.employeeMonthlyFeeGoogleSheet.updateEmployeeMonthlyFee(fee);
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        Transaction salaryTransaction = new Transaction();
        salaryTransaction.setTransactionType(TransactionType.OUTCOME);
        salaryTransaction.setPaymentType(PaymentType.NAQD);
        salaryTransaction.setMonthlyFee(fee);
        salaryTransaction.setAmount(amount);
        salaryTransaction.setCompany(company);
        salaryTransaction.setDescription(month + " OYINING AVANS TO'LOV TRANSAKSIYASI UCHUN IZOH: " + comment);
        salaryTransaction = transactionRepository.save(salaryTransaction);
        try {
            this.employeeTransactionGoogleSheet.initializeSheet();
            this.employeeTransactionGoogleSheet.recordEmployeeTransactions(salaryTransaction);
        } catch (Exception e) {
            log.error("Error recording employee transaction to Google Sheets: {}", e.getMessage());
        }

        return ApiResponse.builder()
                .status(HttpStatus.CREATED)
                .message(RestConstants.SUCCESSFULLY_PAID)
                .build();
    }

    @Override
    @Transactional
    public ApiResponse updateEmployeeBonusTuition(Long bonusId, StudentContractDto.EmployeeBonusPaymentDto dto) {

        Transaction transaction = this.transactionRepository.findByIdAndDeletedFalse(bonusId)
                .orElseThrow(() -> new ResourceNotFoundException(RestConstants.TRANSACTION_NOT_FOUND));
        if (dto.getBonus() < 0) {
            return ApiResponse.builder()
                    .status(HttpStatus.CONFLICT)
                    .message(RestConstants.INVALID_AMOUNT)
                    .build();
        }
        MonthlyFee fee = transaction.getMonthlyFee();
        Company company = this.getCompany();
        double cashBalance = company.getCashBalance() != null ? company.getCashBalance() : 0.0;
        Double oldBonusAmount = fee.getBonus();
        double finalAmount = cashBalance + oldBonusAmount - dto.getBonus();

        if (finalAmount < 0) {
            return ApiResponse.builder()
                    .status(HttpStatus.CONFLICT)
                    .message(RestConstants.NOT_ENOUGH_CASH_BALANCE)
                    .build();
        }
        company.setCashBalance(finalAmount);
        this.companyRepository.save(company);

        if (dto.getBonus() != null) {
            fee.setBonus(fee.getBonus() - transaction.getAmount() + dto.getBonus());
            transaction.setAmount(dto.getBonus());
        }
        if (fee.getBonus() < 0) {
            return ApiResponse.builder()
                    .status(HttpStatus.CONFLICT)
                    .message(RestConstants.INVALID_AMOUNT)
                    .build();
        }
        if (dto.getComment() != null && !dto.getComment().isEmpty()) {
            transaction.setDescription(dto.getComment());
        }
        fee = this.monthlyFeeRepository.save(fee);
        try {
            this.employeeMonthlyFeeGoogleSheet.updateEmployeeMonthlyFee(fee);
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        transaction = this.transactionRepository.save(transaction);
        try {
            this.employeeTransactionGoogleSheet.initializeSheet();
            this.employeeTransactionGoogleSheet.updateEmployeeTransaction(transaction);
        } catch (Exception e) {
            log.error(e.getMessage());
        }

        return ApiResponse.builder()
                .status(HttpStatus.OK)
                .message(RestConstants.SUCCESSFULLY_SAVED)
                .data(Map.of(
                        "newBonus", dto.getBonus(),
                        "oldBonus", oldBonusAmount,
                        "newBalance", finalAmount
                ))
                .build();
    }

    @Override
    public ApiResponse deleteTables(List<Long> ids) {
        List<TeacherTable> list = this.teacherTableRepository.findAllByIdsAndDeletedFalse(ids);
        if (list.isEmpty()) {
            return ApiResponse.builder()
                    .status(HttpStatus.OK)
                    .message("O'CHIRISH UCHUN TABELLAR TOPILMADI!")
                    .build();
        }
        // Mark entities as deleted in a single batch operation
        list.forEach(teacherTable -> teacherTable.setDeleted(true));
        teacherTableRepository.saveAll(list);

        return ApiResponse.builder()
                .status(HttpStatus.OK)
                .message(RestConstants.SUCCESSFULLY_DELETED)
                .build();
    }


    @Transactional
    @Override
    public ApiResponse updateEmployeeTuition(Long transactionId, StudentContractDto.EmployeePaymentDto dto) {

        if (dto.getNewAmount() < 0) {
            return ApiResponse.builder()
                    .status(HttpStatus.CONFLICT)
                    .message(RestConstants.INVALID_AMOUNT)
                    .build();
        }

        Company company = this.getCompany();
        double cashBalance = company.getCashBalance() != null ? company.getCashBalance() : 0.0;

        // Get existing transaction
        Transaction transaction = transactionRepository.findByIdAndDeletedFalse(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException(RestConstants.TRANSACTION_NOT_FOUND));

        Double oldAmount = transaction.getAmount();
        MonthlyFee fee = transaction.getMonthlyFee();
        double amountDifference = dto.getNewAmount() - oldAmount;

        // Validate new amount doesn't exceed total fee
        Double totalFeeAmount = fee.getTotalFee(); // Assuming this exists
        if (dto.getNewAmount() > totalFeeAmount) {
            return ApiResponse.builder()
                    .status(HttpStatus.CONFLICT)
                    .message("Kiritilgan miqdor oylik to'lovdan katta bo'lishi mumkin emas!")
                    .build();
        }

        // Check cash balance for payment increase
        if (amountDifference > 0) {
            if (cashBalance < amountDifference) {
                return ApiResponse.builder()
                        .status(HttpStatus.CONFLICT)
                        .message(RestConstants.MONEY_IS_NOT_ENOUGH)
                        .build();
            }
            // Deduct from company cash
            company.setCashBalance(cashBalance - amountDifference);
        } else if (amountDifference < 0) {
            // Refund to company cash
            company.setCashBalance(cashBalance - amountDifference); // amountDifference is negative
        }

        // Update fee balances
        Double currentAmountPaid = fee.getAmountPaid();
        double newAmountPaid = currentAmountPaid + amountDifference;


        TeacherTable teacherTable = this.teacherTableRepository.findByTeacherIdAndMonthAndYear(
                fee.getEmployee().getId(),
                fee.getMonths(),
                LocalDateTime.now().getYear()
        ).orElseThrow(() -> new ResourceNotFoundException(RestConstants.TABEL_NOT_FOUND));
        Double amount = teacherTable.getAmount();

        // Update entities
        fee.setAmountPaid(newAmountPaid);
        transaction.setAmount(dto.getNewAmount());

        double diff = amount - newAmountPaid;
        fee.setRemainingBalance(diff);

        if (dto.getComment() != null && !dto.getComment().isEmpty()) {
            transaction.setDescription(dto.getComment());
        }

        // Update payment status
        if (diff == 0) {
            fee.setStatus(PaymentStatus.FULLY_PAID);
        } else if (newAmountPaid > 0) {
            fee.setStatus(PaymentStatus.PARTIALLY_PAID);
        } else {
            fee.setStatus(PaymentStatus.UNPAID); // Assuming this status exists
        }

        // Save all changes
        if (amountDifference != 0) {
            companyRepository.save(company);
        }
        transaction = transactionRepository.save(transaction);
        try {
            this.employeeTransactionGoogleSheet.initializeSheet();
            this.employeeTransactionGoogleSheet.updateEmployeeTransaction(transaction);
        } catch (Exception e) {
            log.error("Error recording employee transaction to Google Sheets: {}", e.getMessage());
        }
        monthlyFeeRepository.save(fee);

        return ApiResponse.builder()
                .status(HttpStatus.OK)
                .message(RestConstants.SUCCESSFULLY_UPDATED)
                .build();
    }


    @Transactional
    @Override
    public ApiResponse payForCard(List<Long> employeeIds, Months months, Double amount) {

        List<User> userList = this.userRepository.findAllByIdAndDeletedFalse(employeeIds);
        Company company = companyRepository.findOne()
                .orElseThrow(() -> new ResourceNotFoundException(RestConstants.COMPANY_NOT_FOUND));

        int size = userList.size();
        double totalAmount = size * amount;


        Double cardBalance = company.getCardBalance();
        double finalAmount = cardBalance - totalAmount;

        if (finalAmount < 0 || amount <= 0) {
            return ApiResponse.builder()
                    .status(HttpStatus.CONFLICT)
                    .message(RestConstants.MONEY_IS_NOT_ENOUGH)
                    .build();
        }

        company.setCardBalance(finalAmount);
        this.companyRepository.save(company);

        for (User employee : userList) {
            Category category = categoryRepository.findByName(XODIMGA_OYLIK)
                    .orElseThrow(() -> new ResourceNotFoundException(RestConstants.CATEGORY_NOT_FOUND));
            int year = LocalDateTime.now().getYear();


            TeacherTable table = this.teacherTableRepository.existByTeacherIdAndMonthAndYear(employee.getId(), months.name(), year)
                    .orElseThrow(() -> new ResourceNotFoundException(RestConstants.TABEL_NOT_FOUND));

            MonthlyFee monthlyFee;

            Optional<MonthlyFee> feeOptional = this.monthlyFeeRepository.findByEmployeeIdAndMonths(employee.getId(), months);

            if (feeOptional.isEmpty()) {
                monthlyFee = new MonthlyFee();
                monthlyFee.setEmployee(employee);
                monthlyFee.setMonths(months);
                monthlyFee.setAmountPaid(amount);
                monthlyFee.setTotalFee(table.getAmount());
                monthlyFee.setRemainingBalance(table.getAmount() - amount);
                monthlyFee.setCategory(category);
                monthlyFee.setCompany(company);
                monthlyFee = monthlyFeeRepository.save(monthlyFee);
                try {
                    this.employeeMonthlyFeeGoogleSheet.initializeSheet();
                    this.employeeMonthlyFeeGoogleSheet.recordEmployeeMonthlyFee(monthlyFee);
                } catch (Exception e) {
                    log.error(e.getMessage());
                }
            } else {
                monthlyFee = feeOptional.get();
                monthlyFee.setEmployee(employee);
                monthlyFee.setMonths(months);
                monthlyFee.setAmountPaid(monthlyFee.getAmountPaid() + amount);
                monthlyFee.setRemainingBalance(monthlyFee.getRemainingBalance() - amount);
                monthlyFee.setCategory(category);

                if (monthlyFee.getRemainingBalance() <= 0 ||
                        (monthlyFee.getAmountPaid() >= monthlyFee.getTotalFee())) {
                    monthlyFee.setStatus(PaymentStatus.FULLY_PAID);
                } else if (monthlyFee.getAmountPaid() > 0) {
                    monthlyFee.setStatus(PaymentStatus.PARTIALLY_PAID);
                } else {
                    monthlyFee.setStatus(PaymentStatus.UNPAID);
                }

                monthlyFee.setCompany(company);
                monthlyFee = monthlyFeeRepository.save(monthlyFee);
                try {
                    this.employeeMonthlyFeeGoogleSheet.initializeSheet();
                    this.employeeMonthlyFeeGoogleSheet.updateEmployeeMonthlyFee(monthlyFee);
                } catch (Exception e) {
                    log.error(e.getMessage());
                }

            }

            Transaction transaction = new Transaction();
            transaction.setCompany(company);
            transaction.setTransactionType(TransactionType.OUTCOME);
            transaction.setPaymentType(PaymentType.BANK);
            transaction.setAmount(amount);
            transaction.setMonthlyFee(monthlyFee);
            transaction.setDescription("ASOSIY_TO'LOV_TRANSAKSIYASI: " + months.name() + " OYI UCHUN XODIM PLASTIK KARTASIGA TO'LOV QILINDI!");
            transaction = this.transactionRepository.save(transaction);

            try {
                this.employeeTransactionGoogleSheet.initializeSheet();
                this.employeeTransactionGoogleSheet.recordEmployeeTransactions(transaction);
            } catch (Exception e) {
                log.error(e.getMessage());
            }

        }
        return ApiResponse.builder()
                .status(HttpStatus.OK)
                .message(RestConstants.SUCCESSFULLY_PAID)
                .build();
    }

    @Override
    public void downloadExcel(TeacherTabelSheetFilter filter, OutputStream outputStream) {
        List<TeacherTable> allByStatus = this.teacherTableRepository.findAll(filter);

        log.info(allByStatus.toString());
        if (allByStatus.isEmpty()) {
            throw new ResourceNotFoundException("Ma'lumot topilmadi!");
        }

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet(LocalDate.now() + "_tabellar");

            // Column headers
            String[] columns = {
                    "ID",
                    "Xodim",
                    "Ish Kunlari/Dars Soatlari",
                    "Ishlagan Kunlari/Bajarilgan Dars Soatlari",
                    "Qo'shimcha Ishlagan Kunlar/Dars Soatlari",
                    "Qoldirilgan Kunlar/Dars Soatlari",
                    "Oy",
                    "Izoh",
                    "Oylik Maosh",
                    "Xisoblangan SUMMA",
                    "Premiya miqdori",
                    "Premiyaga sabab",
                    "Jami to'langan miqdor",
                    "Qo'shimcha Dars Narxi",
                    "Yaratilgan sana"
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
            }

            // Fill data rows
            int rowIdx = 1;
            for (TeacherTable tabel : allByStatus) {
                Row row = sheet.createRow(rowIdx++);
                row.setHeightInPoints(18); // 🔹 Increase Row Height
                Cell cell0 = row.createCell(0);
                cell0.setCellValue(tabel.getId());
                cell0.setCellStyle(dataStyle);
                Cell cell1 = row.createCell(1);
                cell1.setCellValue(tabel.getTeacher().getFullName() != null ? tabel.getTeacher().getFullName() : "N/A");
                cell1.setCellStyle(dataStyle);
                double bonusAmount = this.getBonusAmount(tabel) != null ? this.getBonusAmount(tabel) : 0.0;
                String bonusReason = this.getBonusReason(tabel) != null ? this.getBonusReason(tabel) : "N/A";
                double paidAmount = this.getPaidAmount(tabel) != null ? this.getPaidAmount(tabel) : 0.0;
                row.createCell(2).setCellValue(tabel.getWorkDaysOrLessons() != null ? tabel.getWorkDaysOrLessons() : 0);
                row.createCell(3).setCellValue(tabel.getWorkedDaysOrLessons() != null ? tabel.getWorkedDaysOrLessons() : 0);
                row.createCell(4).setCellValue(tabel.getExtraWorkedDaysOrLessons() != null ? tabel.getExtraWorkedDaysOrLessons() : 0);
                row.createCell(5).setCellValue(tabel.getMissedWorkDaysOrLessons() != null ? tabel.getMissedWorkDaysOrLessons() : 0);
                row.createCell(6).setCellValue(tabel.getMonths() != null ? tabel.getMonths().name() : "N/A");
                row.createCell(7).setCellValue(tabel.getDescription() != null ? tabel.getDescription() : "N/A");
                row.createCell(8).setCellValue(
                        tabel.getMonthlySalary() != null ? tabel.getMonthlySalary() : 0.0
                );
                row.createCell(9).setCellValue(
                        tabel.getAmount() != null ? tabel.getAmount() : 0.0
                );
                row.createCell(10).setCellValue(bonusAmount);
                row.createCell(11).setCellValue(bonusReason);
                row.createCell(12).setCellValue(paidAmount);
                row.createCell(13).setCellValue(
                        tabel.getExtraLessonPrice() != null && tabel.getExtraLessonPrice().getFixedAmount() != null
                                ? tabel.getExtraLessonPrice().getFixedAmount()
                                : 0.0
                );
                row.createCell(14).setCellValue(
                        tabel.getCreatedAt() != null
                                ? tabel.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm"))
                                : "N/A"
                );

                for (int i = 0; i < columns.length; i++) {
                    row.getCell(i).setCellStyle(dataStyle);
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

    private Double getBonusAmount(TeacherTable teacherTable) {
        return this.monthlyFeeRepository
                .findByEmployeeIdAndMonths(teacherTable.getTeacher().getId(), teacherTable.getMonths())
                .map(fee -> fee.getBonus() != null ? fee.getBonus() : 0.0)
                .orElse(0.0); // 🔹 default if no MonthlyFee exists
    }

    private Double getPaidAmount(TeacherTable teacherTable) {
        return this.monthlyFeeRepository
                .findByEmployeeIdAndMonths(teacherTable.getTeacher().getId(), teacherTable.getMonths())
                .map(fee -> (fee.getBonus() != null ? fee.getBonus() : 0.0) + (fee.getAmountPaid() != null ? fee.getAmountPaid() : 0.0))
                .orElse(0.0); // 🔹 default if no MonthlyFee exists
    }

    private String getBonusReason(TeacherTable teacherTable) {
        return this.monthlyFeeRepository
                .findByEmployeeIdAndMonths(teacherTable.getTeacher().getId(), teacherTable.getMonths())
                .map(fee -> fee.getReason() != null ? fee.getReason() : "N/A")
                .orElse("N/A"); // 🔹 default if no MonthlyFee exists
    }


    private void setBorders(CellStyle style) {
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);

    }

    public static double roundTo1000(double amount) {
        return Math.round(amount / 1000.0) * 1000;
    }

    public Company getCompany() {
        return this.companyRepository.findOne()
                .orElseThrow(() -> new ResourceNotFoundException(RestConstants.COMPANY_NOT_FOUND));
    }

}

