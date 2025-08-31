package org.example.moliyaapp.service.impl;

import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.example.moliyaapp.dto.ApiResponse;
import org.example.moliyaapp.dto.ExpensesDto;
import org.example.moliyaapp.entity.*;
import org.example.moliyaapp.enums.PaymentType;
import org.example.moliyaapp.enums.TransactionType;
import org.example.moliyaapp.exception.ResourceNotFoundException;
import org.example.moliyaapp.filter.ExpensesFilter;
import org.example.moliyaapp.filter.ExpensesFilterWithStatus;
import org.example.moliyaapp.filter.ExpensesSheetFilter;
import org.example.moliyaapp.mapper.ExpenseMapper;
import org.example.moliyaapp.mapper.TransactionMapper;
import org.example.moliyaapp.projection.GetExpenseData;
import org.example.moliyaapp.projection.GetExpenseDataDto;
import org.example.moliyaapp.repository.*;
import org.example.moliyaapp.service.ExpenseGoogleSheet;
import org.example.moliyaapp.service.ExpensesService;
import org.example.moliyaapp.utils.DateUtils;
import org.example.moliyaapp.utils.RestConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExpensesServiceImpl implements ExpensesService {

    private final TransactionMapper transactionMapper;
    private final ImageRepository imageRepository;
    private final ExpenseGoogleSheet expenseGoogleSheetImpl;
    Logger log = LoggerFactory.getLogger(ExpensesService.class);

    private final ExpenseMapper expenseMapper;
    private final ExpensesRepository expensesRepository;
    private final TransactionRepository transactionRepository;
    private final CompanyRepository companyRepository;
    private final CategoryRepository categoryRepository;


    @Transactional
    @Override
    public ApiResponse create(ExpensesDto.CreateExpense dto) {

        if (dto.getAmount() == null || dto.getAmount() <= 0) {
            return ApiResponse.builder()
                    .status(HttpStatus.BAD_REQUEST)
                    .message(RestConstants.INVALID_AMOUNT)
                    .build();
        }
        List<Image> images = new ArrayList<>();
        if (dto.getImageUrls() != null && !dto.getImageUrls().isEmpty()) {
            for (String imageUrl : dto.getImageUrls()) {
                Image image = this.imageRepository.findByUrl(imageUrl)
                        .orElseThrow(() -> new ResourceNotFoundException("RASM TOPILMADI!"));
                images.add(image);
            }
        }

        Company company = this.companyRepository.findOne()
                .orElseThrow(() -> new ResourceNotFoundException(RestConstants.COMPANY_NOT_FOUND));

        Category category = this.categoryRepository.findByName(dto.getCategoryName())
                .orElseThrow(() -> new ResourceNotFoundException(RestConstants.CATEGORY_NOT_FOUND));

        Double cardBalance = company.getCardBalance() != null ? company.getCardBalance() : 0.0;
        Double cashBalance = company.getCashBalance() != null ? company.getCashBalance() : 0.0;

        if (dto.getAmount() == null || dto.getAmount() <= 0) {
            return ApiResponse.builder()
                    .status(HttpStatus.BAD_REQUEST)
                    .message(RestConstants.INVALID_AMOUNT)
                    .build();
        }

        if (dto.getTransactionType() == TransactionType.OUTCOME) {
            if (dto.getPaymentType() == PaymentType.BANK) {
                // Card payment validation
                if (cardBalance < dto.getAmount()) {
                    return ApiResponse.builder()
                            .status(HttpStatus.BAD_REQUEST)
                            .message(RestConstants.NOT_ENOUGH_CARD_BALANCE)
                            .build();
                }
                company.setCardBalance(cardBalance - dto.getAmount());
            } else if (dto.getPaymentType() == PaymentType.NAQD) {
                // Cash payment validation (strict - no fallback)
                if (cashBalance < dto.getAmount()) {
                    return ApiResponse.builder()
                            .status(HttpStatus.BAD_REQUEST)
                            .message(RestConstants.NOT_ENOUGH_CASH_BALANCE)
                            .build();
                }
                company.setCashBalance(cashBalance - dto.getAmount());
            }
        } else if (dto.getTransactionType() == TransactionType.INCOME) {
            // Handle income transactions
            if (dto.getPaymentType() == PaymentType.BANK) {
                company.setCardBalance(cardBalance + dto.getAmount());
            } else if (dto.getPaymentType() == PaymentType.NAQD) {
                company.setCashBalance(cashBalance + dto.getAmount());
            }
        }

        // Save company with updated balances
        Company saveCompany = this.companyRepository.save(company);
        Transaction transaction = new Transaction();
        transaction.setTransactionType(dto.getTransactionType());
        transaction.setPaymentType(dto.getPaymentType());
        transaction.setAmount(dto.getAmount());
        transaction.setDescription(dto.getDescription());
        transaction.setCompany(saveCompany);
        transaction.setCreatedAt(dto.getTime());

        Expenses expenses = this.expenseMapper.toEntity(dto);
        expenses.setCompany(saveCompany);
        expenses.setCategory(category);
        expenses.setCreatedAt(dto.getTime());
        if (expenses.getImages() == null) expenses.setImages(new ArrayList<>());
        expenses.getImages().addAll(images);
        images.forEach(img -> img.setExpenses(expenses));

        Expenses save = this.expensesRepository.save(expenses);

        transaction.setExpenses(save);
        this.transactionRepository.save(transaction);
        try {
            expenseGoogleSheetImpl.initializeSheet();
            expenseGoogleSheetImpl.recordExpense(expenses);
        } catch (RuntimeException e) {
            log.info(e.getMessage());
        }
        return ApiResponse.builder()
                .status(HttpStatus.OK)
                .message(RestConstants.SUCCESSFULLY_SAVED)
                .build();
    }


    @Override
    public ApiResponse getById(Long id) {
        Expenses expenses = this.expensesRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(RestConstants.EXPENSE_NOT_FOUND));
        return ApiResponse.builder()
                .status(HttpStatus.OK)
                .data(this.expenseMapper.toDto(expenses))
                .build();
    }

    @Override
    public ApiResponse getAll(Pageable pageable, ExpensesFilter filter) {
        Page<Expenses> expensesList = this.expensesRepository.findAll(filter, pageable);
        if (!expensesList.isEmpty()) {
            return ApiResponse.builder()
                    .status(HttpStatus.OK)
                    .message(RestConstants.SUCCESS)
                    .data(this.expenseMapper.dtoList(expensesList.getContent()))
                    .elements(expensesList.getTotalElements())
                    .pages(expensesList.getTotalPages())
                    .build();
        }
        return ApiResponse.builder()
                .status(HttpStatus.OK)
                .data(new ArrayList<>())
                .build();
    }

    @Override
    public ApiResponse delete(Long id) {
        Expenses expenses = this.expensesRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(RestConstants.EXPENSE_NOT_FOUND));
        expenses.setDeleted(true);
        this.expensesRepository.save(expenses);
        return ApiResponse.builder()
                .status(HttpStatus.OK)
                .message(RestConstants.SUCCESSFULLY_DELETED)
                .build();
    }

    @Override
    public ApiResponse getAllDeletedExpenses(Pageable pageable) {
        Page<Expenses> expensesPage = this.expensesRepository.findAllDeleted(pageable);
        return ApiResponse.builder()
                .status(HttpStatus.OK)
                .message(RestConstants.SUCCESS)
                .data(expensesPage.getContent()
                        .stream()
                        .map(this.expenseMapper::toDto)
                        .collect(Collectors.toSet()))
                .elements(expensesPage.getTotalElements())
                .pages(expensesPage.getTotalPages())
                .build();
    }

    @Transactional
    @Override
    public ApiResponse updateExpense(Long id, ExpensesDto.UpdateExpense dto) {
        try {
            // Validate input
            if (dto.getAmount() == null || dto.getAmount() < 0) {
                return ApiResponse.builder()
                        .status(HttpStatus.BAD_REQUEST)
                        .message(RestConstants.INVALID_AMOUNT)
                        .build();
            }

            // Fetch required entities
            Category category = this.categoryRepository.findByName(dto.getCategoryName())
                    .orElseThrow(() -> new ResourceNotFoundException(RestConstants.CATEGORY_NOT_FOUND));

            Expenses expense = this.expensesRepository.findByIdAndDeletedFalse(id)
                    .orElseThrow(() -> new ResourceNotFoundException(RestConstants.EXPENSE_NOT_FOUND));

            Company company = this.companyRepository.findOne()
                    .orElseThrow(() -> new ResourceNotFoundException(RestConstants.COMPANY_NOT_FOUND));

            // Get current balances with null safety
            Double cardBalance = company.getCardBalance() != null ? company.getCardBalance() : 0.0;
            Double cashBalance = company.getCashBalance() != null ? company.getCashBalance() : 0.0;
            double oldAmount = expense.getAmount() != null ? expense.getAmount() : 0.0;
            Double newAmount = dto.getAmount();

            log.info("Updating expense - Old Amount: {}, New Amount: {}", oldAmount, newAmount);
            log.info("Current Balances - Cash: {}, Card: {}", cashBalance, cardBalance);

            // First revert the old transaction
            if (expense.getTransactionType() == TransactionType.INCOME) {
                if (expense.getPaymentType() == PaymentType.NAQD) {
                    cashBalance -= oldAmount;
                } else if (expense.getPaymentType() == PaymentType.BANK) {
                    cardBalance -= oldAmount;
                }
            } else {
                if (expense.getPaymentType() == PaymentType.NAQD) {
                    cashBalance += oldAmount;
                } else if (expense.getPaymentType() == PaymentType.BANK) {
                    cardBalance += oldAmount;
                }
            }

            // Now apply the new transaction
            if (dto.getTransactionType() == TransactionType.INCOME) {
                if (dto.getPaymentType() == PaymentType.NAQD) {
                    cashBalance += newAmount;
                } else if (dto.getPaymentType() == PaymentType.BANK) {
                    cardBalance += newAmount;
                }
            } else {
                // Check if enough balance exists
                if (dto.getPaymentType() == PaymentType.NAQD) {
                    if (cashBalance < newAmount) {
                        // Check card balance as fallback
                        if (cardBalance >= newAmount) {
                            cardBalance -= newAmount;
                        } else {
                            return ApiResponse.builder()
                                    .status(HttpStatus.BAD_REQUEST)
                                    .message(RestConstants.MONEY_IS_NOT_ENOUGH)
                                    .build();
                        }
                    } else {
                        cashBalance -= newAmount;
                    }
                } else if (dto.getPaymentType() == PaymentType.BANK) {
                    if (cardBalance < newAmount) {
                        return ApiResponse.builder()
                                .status(HttpStatus.BAD_REQUEST)
                                .message(RestConstants.MONEY_IS_NOT_ENOUGH)
                                .build();
                    }
                    cardBalance -= newAmount;
                }
            }

            // Update company balances
            company.setCashBalance(cashBalance);
            company.setCardBalance(cardBalance);
            company = this.companyRepository.save(company);

            log.info("Updating transactionType from {} to {}",
                    expense.getTransactionType(),
                    dto.getTransactionType());

            // Update expense
            Expenses updatedExpense = this.expenseMapper.toUpdate(expense, dto);
            updatedExpense.setAmount(newAmount);
            updatedExpense.setCategory(category);
            updatedExpense.setTransactionType(dto.getTransactionType());
            updatedExpense = this.expensesRepository.save(updatedExpense);

            try {
                this.expenseGoogleSheetImpl.updateExpense(updatedExpense);
            } catch (Exception e) {
                log.info("failed to update in google sheet!");
            }
            // CORRECTED: Always update the existing transaction or create new one
            Optional<Transaction> existingTransactionOpt = transactionRepository.findByExpensesId(expense.getId());
            Transaction transaction;

            if (existingTransactionOpt.isPresent()) {
                // Update existing transaction with new values
                transaction = existingTransactionOpt.get();
                log.info("Updating existing transaction ID: {}", transaction.getId());
            } else {
                // Create new transaction if none exists
                transaction = new Transaction();
                transaction.setExpenses(updatedExpense);
                transaction.setCompany(company);
                log.info("Creating new transaction for expense ID: {}", updatedExpense.getId());
            }

            // Update transaction with new values (whether existing or new)
            transaction.setAmount(newAmount);
            transaction.setPaymentType(dto.getPaymentType());
            transaction.setTransactionType(dto.getTransactionType());
            transaction.setDescription(dto.getDescription());

            // Ensure transaction reflects the updated expense relationship
            transaction.setExpenses(updatedExpense);
            transaction.setCompany(company);

            transaction = this.transactionRepository.save(transaction);

            log.info("Transaction saved with ID: {}, Amount: {}, Type: {}",
                    transaction.getId(), transaction.getAmount(), transaction.getTransactionType());
            log.info("Updated balances - Cash: {}, Card: {}", company.getCashBalance(), company.getCardBalance());

            return ApiResponse.builder()
                    .status(HttpStatus.OK)
                    .message(RestConstants.SUCCESS)
                    .build();

        } catch (Exception e) {
            log.error("Error updating expense: {}", e.getMessage(), e);
            return ApiResponse.builder()
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .message(RestConstants.FAILED_TO_UPDATE)
                    .build();
        }
    }

    @Override
    public ApiResponse getDailyExpenses(Pageable pageable) {
        LocalDateTime start = DateUtils.startOfToday();
        LocalDateTime end = DateUtils.endOfToday();
        Page<Expenses> expensesPage = this.expensesRepository.findByCreatedAtBetween(start, end, pageable);
        Double dailyExpenses = this.expensesRepository.getTotalExpensesBetween(start, end);
        List<ExpensesDto> expensesDtos = this.expenseMapper.dtoList(expensesPage.getContent());
        return ApiResponse.builder()
                .message(RestConstants.SUCCESS)
                .status(HttpStatus.OK)
                .data(expensesDtos)
                .meta(Map.of("totalAmount", dailyExpenses))
                .elements(expensesPage.getTotalElements())
                .pages(expensesPage.getTotalPages())
                .build();
    }

    @Override
    public ApiResponse getWeeklyExpenses(Pageable pageable) {
        LocalDateTime start = DateUtils.startOfWeek();
        LocalDateTime end = DateUtils.endOfToday();
        Page<Expenses> expensesPage = this.expensesRepository.findByCreatedAtBetween(start, end, pageable);
        Double totalExpensesBetween = this.expensesRepository.getTotalExpensesBetween(start, end);
        List<ExpensesDto> expensesDtos = this.expenseMapper.dtoList(expensesPage.getContent());
        return ApiResponse.builder()
                .status(HttpStatus.OK)
                .message(RestConstants.SUCCESS)
                .data(expensesDtos)
                .meta(Map.of("totalAmount", totalExpensesBetween))
                .elements(expensesPage.getTotalElements())
                .pages(expensesPage.getTotalPages())
                .build();
    }

    @Override
    public ApiResponse getMonthlyExpenses(Pageable pageable) {
        LocalDateTime start = DateUtils.startOfMonth();
        LocalDateTime end = DateUtils.endOfToday();
        Page<Expenses> expensesPage = this.expensesRepository.findByCreatedAtBetween(start, end, pageable);
        Double amount = this.expensesRepository.getTotalExpensesBetween(start, end);
        List<ExpensesDto> expensesDtos = this.expenseMapper.dtoList(expensesPage.getContent());
        return ApiResponse.builder()
                .status(HttpStatus.OK)
                .message(RestConstants.SUCCESS)
                .data(expensesDtos)
                .meta(Map.of("totalAmount", amount))
                .elements(expensesPage.getTotalElements())
                .pages(expensesPage.getTotalPages())
                .build();
    }

    @Override
    public ApiResponse getYearlyExpenses(Pageable pageable) {
        LocalDateTime start = DateUtils.startOfYear();
        LocalDateTime end = DateUtils.endOfToday();
        Page<Expenses> expensesPage = this.expensesRepository.findByCreatedAtBetween(start, end, pageable);
        Double amount = this.expensesRepository.getTotalExpensesBetween(start, end);
        List<ExpensesDto> expensesDtos = this.expenseMapper.dtoList(expensesPage.getContent());
        return ApiResponse.builder()
                .status(HttpStatus.OK)
                .message(RestConstants.SUCCESS)
                .data(expensesDtos)
                .meta(Map.of("totalAmount", amount))
                .elements(expensesPage.getTotalElements())
                .pages(expensesPage.getTotalPages())
                .build();

    }

    @Override
    public ApiResponse getAllExpensesByDateRange(Pageable pageable, LocalDate start, LocalDate end) {
        LocalDateTime startOfDay = start.atStartOfDay();
        LocalDateTime endOfDay = end.atTime(LocalTime.MAX);
        Page<Expenses> expensesPage = this.expensesRepository.findByCreatedAtBetween(startOfDay, endOfDay, pageable);
        Double totalExpensesBetween = this.expensesRepository.getTotalExpensesBetween(startOfDay, endOfDay);
        List<ExpensesDto> expensesDtos = this.expenseMapper.dtoList(expensesPage.getContent());
        return ApiResponse.builder()
                .status(HttpStatus.OK)
                .message(RestConstants.SUCCESS)
                .data(expensesDtos)
                .meta(Map.of("totalAmount", totalExpensesBetween))
                .elements(expensesPage.getTotalElements())
                .pages(expensesPage.getTotalPages())
                .build();
    }

    @Override
    public ApiResponse findAllByCategory(Long categoryId, Pageable pageable) {
        Page<Expenses> expensesPage = this.expensesRepository.findAllByCategoryId(categoryId, pageable);
        if (expensesPage.isEmpty()) {
            return ApiResponse.builder()
                    .status(HttpStatus.OK)
                    .message(RestConstants.EXPENSE_NOT_FOUND)
                    .data(this.expenseMapper.dtoList(expensesPage.getContent()))
                    .build();
        }
        return ApiResponse.builder()
                .status(HttpStatus.OK)
                .message(RestConstants.SUCCESS)
                .data(this.expenseMapper.dtoList(expensesPage.getContent()))
                .build();
    }

    @Override
    public ApiResponse deleteExpensesList(List<Long> ids) {
        List<Long> allByDeletedTrue = this.expensesRepository.findAllByDeletedTrue(ids);
        if (allByDeletedTrue.isEmpty()) {
            return ApiResponse.builder()
                    .status(HttpStatus.OK)
                    .message("O'CHIRISH UCHUN XARAJATLAR MAVJUD EMAS!")
                    .build();
        }
        this.expensesRepository.deleteAllByIds(allByDeletedTrue);
        return ApiResponse.builder()
                .status(HttpStatus.OK)
                .message("TANLANGAN XARAJATLAR BUTUNLAY O'CHIRILDI!")
                .build();
    }

    @Override
    public ApiResponse getAllByCategoryStatus(ExpensesFilterWithStatus status, Pageable pageable) {
        List<Expenses> all = this.expensesRepository.findAll(status);
        Page<Expenses> monthlyFeePage = this.expensesRepository.findAll(status, pageable);
        double amount = 0.0;
        if (!all.isEmpty()) {
            for (Expenses expenses : all) {
                amount += expenses.getAmount();
            }
            return ApiResponse.builder()
                    .status(HttpStatus.OK)
                    .message(RestConstants.SUCCESS)
                    .data(this.expenseMapper.dtoList(monthlyFeePage.getContent()))
                    .elements(monthlyFeePage.getTotalElements())
                    .pages(monthlyFeePage.getTotalPages())
                    .meta(Map.of("amount", amount))
                    .build();
        }
        return ApiResponse.builder()
                .status(HttpStatus.OK)
                .data(new ArrayList<>())
                .build();
    }

    @Override
    public void downloadExcel(ExpensesSheetFilter filter, OutputStream outputStream) {
        List<Expenses> allByStatus = this.expensesRepository.findAll(filter);

        log.info(allByStatus.toString());
        if (allByStatus.isEmpty()) {
            throw new ResourceNotFoundException("Yuklab olinadigan xarajatlar topilmadi!");
        }

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("xarajatlar");

            // Column headers
            String[] columns = {
                    "ID",
                    "Kirim/Chiqim Nomi",
                    "Miqdori",
                    "Qubul qiluvchi",
                    "Sarflovchi",
                    "Izoh",
                    "Transaksiya Turi",
                    "To'lov turi",
                    "Kategoriya",
                    "Sana"
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
            for (Expenses expenses : allByStatus) {
                Row row = sheet.createRow(rowIdx++);
                row.setHeightInPoints(18); // 🔹 Increase Row Height
                Cell cell0 = row.createCell(0);
                cell0.setCellValue(expenses.getId());
                cell0.setCellStyle(dataStyle);

                Cell cell1 = row.createCell(1);
                cell1.setCellValue(expenses.getName() != null ? expenses.getName() : "N/A");
                cell1.setCellStyle(dataStyle);
                row.createCell(2).setCellValue(expenses.getAmount() != null ? expenses.getAmount() : 0.0);
                row.createCell(3).setCellValue(expenses.getReceiver() != null ? expenses.getReceiver() : "N/A");
                row.createCell(4).setCellValue(expenses.getSpender() != null ? expenses.getSpender() : "N/A");
                row.createCell(5).setCellValue(expenses.getDescription() != null ? expenses.getDescription() : "N/A");
                row.createCell(6).setCellValue(expenses.getTransactionType() != null ? expenses.getTransactionType().name() : "N/A");
                row.createCell(7).setCellValue(expenses.getPaymentType() != null ? expenses.getPaymentType().name() : "N/A");
                row.createCell(8).setCellValue(expenses.getCategory().getName() != null ? expenses.getCategory().getName() : "N/A");
//                row.createCell(9).setCellValue(expenses.getCreatedAt() != null ? expenses.getCreatedAt() : null);
                row.createCell(9).setCellValue(
                        expenses.getCreatedAt() != null
                                ? expenses.getCreatedAt().format(java.time.format.DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm"))
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

    private void setBorders(CellStyle style) {
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
    }


    @Transactional
    @Override
    public int uploadExpenseSheet(MultipartFile file) {
        try (InputStream inputStream = file.getInputStream();
             XSSFWorkbook workbook = new XSSFWorkbook(inputStream)) {

            // Company mavjudligini tekshirish
            Company company = companyRepository.findOne()
                    .orElse(null);

            if (company == null) {
                throw new ResourceNotFoundException("Tashkilot topilmadi!");
            }

            Sheet sheet = workbook.getSheetAt(0);
            int uploadedCount = 0;

            // Header qatorini o'tkazib yuborish (0-qator)
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                try {
                    Expenses expense = new Expenses();

                    // ID ustunini o'tkazib yuborish (0-ustun)

                    // 1-ustun: Kirim/Chiqim Nomi
                    Cell nameCell = row.getCell(1);
                    if (nameCell != null) {
                        expense.setName(getCellValueAsString(nameCell));
                    }

                    // 2-ustun: Miqdori
                    Cell amountCell = row.getCell(2);
                    if (amountCell != null) {
                        expense.setAmount(getCellValueAsDouble(amountCell));
                    }

                    // 3-ustun: Qubul qiluvchi
                    Cell receiverCell = row.getCell(3);
                    if (receiverCell != null) {
                        expense.setReceiver(getCellValueAsString(receiverCell));
                    }

                    // 4-ustun: Sarflovchi
                    Cell spenderCell = row.getCell(4);
                    if (spenderCell != null) {
                        expense.setSpender(getCellValueAsString(spenderCell));
                    }

                    // 5-ustun: Izoh
                    Cell descriptionCell = row.getCell(5);
                    if (descriptionCell != null) {
                        expense.setDescription(getCellValueAsString(descriptionCell));
                    }

                    // 6-ustun: Transaksiya Turi
                    Cell transactionTypeCell = row.getCell(6);
                    if (transactionTypeCell != null) {
                        String transactionTypeStr = getCellValueAsString(transactionTypeCell);
                        if (!transactionTypeStr.equals("N/A")) {
                            try {
                                expense.setTransactionType(TransactionType.valueOf(transactionTypeStr));
                            } catch (IllegalArgumentException e) {
                                log.warn("Noto'g'ri transaksiya turi: " + transactionTypeStr + " (" + (i + 1) + "-qator)");
                            }
                        }
                    }

                    // 7-ustun: To'lov turi
                    Cell paymentTypeCell = row.getCell(7);
                    if (paymentTypeCell != null) {
                        String paymentTypeStr = getCellValueAsString(paymentTypeCell);
                        if (!paymentTypeStr.equals("N/A")) {
                            try {
                                expense.setPaymentType(PaymentType.valueOf(paymentTypeStr));
                            } catch (IllegalArgumentException e) {
                                log.warn("Noto'g'ri to'lov turi: " + paymentTypeStr + " (" + (i + 1) + "-qator)");
                            }
                        }
                    }

                    // 8-ustun: Kategoriya
                    Cell categoryCell = row.getCell(8);
                    if (categoryCell != null) {
                        String categoryName = getCellValueAsString(categoryCell);
                        if (!categoryName.equals("N/A")) {
                            // Kategoriya nomi bo'yicha qidirish
                            Category category = categoryRepository.findByName(categoryName)
                                    .orElse(null);
                            if (category == null) {
                                // Agar kategoriya topilmasa, yangi kategoriya yaratish
                                category = Category.builder()
                                        .name(categoryName)
                                        .build();
                                category = categoryRepository.save(category);
                            }
                            expense.setCategory(category);
                        }
                    }

                    // 9-ustun: Sana (createdAt)
                    Cell createdAtCell = row.getCell(9); // 10th column (index 9)
                    if (createdAtCell != null) {
                        try {
                            LocalDateTime createdAt = parseCellToLocalDateTime(createdAtCell);
                            if (createdAt != null) {
                                expense.setCreatedAt(createdAt);
                            } else {
                                log.warn("Sana noto'g'ri yoki bo'sh: " + (i + 1) + "-qator");
                            }
                        } catch (Exception e) {
                            log.warn("Sana parse qilishda xatolik (" + (i + 1) + "-qator): " + e.getMessage());
                        }
                    }
                    // Company o'rnatish
                    expense.setCompany(company);

                    // Validatsiya
                    if (expense.getName() == null || expense.getName().trim().isEmpty()) {
                        log.warn("Xarajat nomi bo'sh: " + (i + 1) + "-qator");
                        continue;
                    }

                    if (expense.getAmount() == null || expense.getAmount() <= 0) {
                        log.warn("Noto'g'ri miqdor: " + (i + 1) + "-qator");
                        continue;
                    }

                    if (expense.getTransactionType() == null) {
                        log.warn("Transaksiya turi ko'rsatilmagan: " + (i + 1) + "-qator");
                        continue;
                    }

                    if (expense.getPaymentType() == null) {
                        log.warn("To'lov turi ko'rsatilmagan: " + (i + 1) + "-qator");
                        continue;
                    }
                    if (expense.getCreatedAt() == null) {
                        log.warn("Sana ko'rsatilmagan: " + (i + 1) + "-qator");
                        continue;
                    }

                    // ===== COMPANY BALANCE YANGILASH =====
                    Double amount = expense.getAmount();
                    PaymentType paymentType = expense.getPaymentType();
                    TransactionType transactionType = expense.getTransactionType();

                    // Joriy balanslarni olish
                    Double currentCashBalance = company.getCashBalance() != null ? company.getCashBalance() : 0.0;
                    Double currentCardBalance = company.getCardBalance() != null ? company.getCardBalance() : 0.0;

                    TransactionType finalTransactionType = expense.getTransactionType();
                    // Balance yangilash
                    if (paymentType == PaymentType.NAQD) {
                        if (finalTransactionType == TransactionType.INCOME) {
                            company.setCashBalance(currentCashBalance + amount);
                            log.info("Naqd pul balansi yangilandi (KIRIM): +" + amount + " (Yangi balans: " + (currentCashBalance + amount) + ")");
                        } else if (finalTransactionType == TransactionType.OUTCOME) {
                            company.setCashBalance(currentCashBalance - amount);
                            log.info("Naqd pul balansi yangilandi (CHIQIM): -" + amount + " (Yangi balans: " + (currentCashBalance - amount) + ")");
                        }
                    } else if (paymentType == PaymentType.BANK) {
                        if (finalTransactionType == TransactionType.INCOME) {
                            company.setCardBalance(currentCardBalance + amount);
                            log.info("Karta balansi yangilandi (KIRIM): +" + amount + " (Yangi balans: " + (currentCardBalance + amount) + ")");
                        } else if (finalTransactionType == TransactionType.OUTCOME) {
                            company.setCardBalance(currentCardBalance - amount);
                            log.info("Karta balansi yangilandi (CHIQIM): -" + amount + " (Yangi balans: " + (currentCardBalance - amount) + ")");
                        }
                    }

                    // Company balansini saqlash
                    companyRepository.save(company);

                    // Expense-ni saqlash
                    Expenses savedExpense = expensesRepository.save(expense);

//                    // Transaction record yaratish
//                    Transaction transaction = Transaction.builder()
//                            .transactionType(transactionType)
//                            .paymentType(paymentType)
//                            .amount(amount)
//                            .description("Excel upload: " + expense.getName())
//                            .expenses(savedExpense)
//                            .company(company)
//                            .build();
                    Transaction tran = new Transaction();
                    tran.setTransactionType(transactionType);
                    tran.setPaymentType(paymentType);
                    tran.setAmount(amount);
                    tran.setDescription(expense.getName());
                    tran.setExpenses(savedExpense);
                    tran.setCompany(company);
                    tran.setCreatedAt(expense.getCreatedAt());

                    transactionRepository.save(tran);
                    uploadedCount++;

                    log.info("Xarajat va Transaction saqlandi: " + expense.getName() + " - " + amount + " (" + transactionType + "/" + paymentType + ")");

                } catch (Exception e) {
                    log.error("Qator ishlovchi xatolik (" + (i + 1) + "-qator): ", e);
                    continue;
                }
            }

            log.info("Excel yuklash tugallandi. Jami saqlangan: " + uploadedCount + " ta");
            return uploadedCount;

        } catch (IOException e) {
            throw new RuntimeException("Excel fayl o'qishda xatolik", e);
        }
    }

    // Helper methods
    private String getCellValueAsString(Cell cell) {
        if (cell == null) return null;

        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                } else {
                    return String.valueOf((long) cell.getNumericCellValue());
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            default:
                return null;
        }
    }

    private Double getCellValueAsDouble(Cell cell) {
        if (cell == null) return null;

        switch (cell.getCellType()) {
            case NUMERIC:
                return cell.getNumericCellValue();
            case STRING:
                try {
                    String value = cell.getStringCellValue().trim();
                    if (value.isEmpty()) return null;
                    return Double.parseDouble(value);
                } catch (NumberFormatException e) {
                    return null;
                }
            default:
                return null;
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
    public ApiResponse getExpensesDataByMonth(TransactionType transactionType, LocalDate from, LocalDate to) {
        // Pass null values directly to the query - let the database handle it
        LocalDateTime startDate = (from != null) ? from.atStartOfDay() : null;
        LocalDateTime endDate = (to != null) ? to.atTime(LocalTime.MAX) : null;

        List<GetExpenseData> dataList = expensesRepository.getExpensesDataByMonth(transactionType, startDate, endDate);

        if (dataList.isEmpty()) {
            return ApiResponse.builder()
                    .status(HttpStatus.OK)
                    .data(new ArrayList<>())
                    .build();
        }

        List<GetExpenseDataDto> dtoList = dataList.stream()
                .map(d -> new GetExpenseDataDto(
                        d.getCategoryName(),
                        d.getAmount(),
                        d.getPaymentType()))
                .toList();

        return ApiResponse.builder()
                .status(HttpStatus.OK)
                .message(RestConstants.SUCCESS)
                .data(dtoList)
                .build();
    }

}
