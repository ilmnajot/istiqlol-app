package org.example.moliyaapp.service.impl;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.example.moliyaapp.dto.ApiResponse;
import org.example.moliyaapp.dto.TeacherContractDto;
import org.example.moliyaapp.entity.TeacherContract;
import org.example.moliyaapp.entity.TeacherTable;
import org.example.moliyaapp.entity.User;
import org.example.moliyaapp.exception.ResourceNotFoundException;
import org.example.moliyaapp.filter.TeacherContractFilter;
import org.example.moliyaapp.filter.TeacherContractSheetFilter;
import org.example.moliyaapp.mapper.TeacherContractMapper;
import org.example.moliyaapp.repository.TeacherContractRepository;
import org.example.moliyaapp.repository.TeacherTableRepository;
import org.example.moliyaapp.repository.UserRepository;
import org.example.moliyaapp.service.EmployeeContractGoogleSheet;
import org.example.moliyaapp.service.TeacherContractService;
import org.example.moliyaapp.utils.RestConstants;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
@Service
public class TeacherContractServiceImpl implements TeacherContractService {

    private final TeacherContractRepository teacherContractRepository;
    private final UserRepository userRepository;
    private final TeacherContractMapper teacherContractMapper;
    private final TeacherTableRepository teacherTableRepository;
    private final EmployeeContractGoogleSheet employeeContractGoogleSheet;


    @Override
    public ApiResponse addTeacherContract(TeacherContractDto.CreateContractDto dto) {
        User teacher = this.userRepository.findByIdAndDeletedFalse(dto.getTeacherId())
                .orElseThrow(() -> new ResourceNotFoundException(RestConstants.TEACHER_NOT_FOUND));
        Optional<TeacherContract> contractOptional = this.teacherContractRepository.findByTeacherIdAndDeletedFalse(teacher.getId());
        if (contractOptional.isPresent()) {
            return ApiResponse.builder()
                    .status(HttpStatus.BAD_REQUEST)
                    .message(RestConstants.CONTRACT_ALREADY_EXIST)
                    .build();
        }

        TeacherContract teacherContract = this.teacherContractMapper.toEntity(dto);
        if (dto.getStartDate() == null) {
            teacherContract.setStartDate(LocalDate.now());
        }
        teacherContract.setTeacher(teacher);
        teacherContract.setActive(true);
        teacherContract = this.teacherContractRepository.save(teacherContract);
        try {
            this.employeeContractGoogleSheet.initializeSheet();
            this.employeeContractGoogleSheet.recordEmployeeContract(teacherContract);

        } catch (Exception e) {
            log.error(e.getMessage());
        }
        TeacherContractDto contractDto = this.teacherContractMapper.toDto(teacherContract);
        return ApiResponse.builder()
                .status(HttpStatus.OK)
                .message(RestConstants.SUCCESS)
                .data(contractDto)
                .build();
    }

    @Override
    public ApiResponse getAllTeacherContractsList() {
        List<TeacherContract> contractList = this.teacherContractRepository.findAllByDeletedIsFalse();
        return ApiResponse.builder()
                .status(HttpStatus.OK)
                .message(RestConstants.SUCCESS)
                .data(this.teacherContractMapper.toDto(contractList))
                .build();
    }

    @Override
    public ApiResponse getAllTeacherContractsPage(Pageable pageable) {
        Page<TeacherContract> contractPage = this.teacherContractRepository.findAllByDeletedIsFalse(pageable);
        if (contractPage.isEmpty()) {
            return ApiResponse.builder()
                    .status(HttpStatus.OK)
                    .message(RestConstants.SUCCESS)
                    .data(new ArrayList<>())
                    .build();
        }
        return ApiResponse.builder()
                .status(HttpStatus.OK)
                .message(RestConstants.SUCCESS)
                .data(this.teacherContractMapper.toDto(contractPage.getContent()))
                .pages(contractPage.getTotalPages())
                .elements(contractPage.getTotalElements())
                .build();
    }

    @Override
    public ApiResponse getTeacherContract(Long id) {
        TeacherContract teacherContract = this.teacherContractRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException(RestConstants.CONTRACT_NOT_FOUND));
        return ApiResponse.builder()
                .status(HttpStatus.OK)
                .message(RestConstants.SUCCESS)
                .data(this.teacherContractMapper.toDto(teacherContract))
                .build();
    }

    @Override
    public ApiResponse getContractsByTeacherId(Long teacherId) {
        List<TeacherContract> teacherContract = this.teacherContractRepository.findByTeacherId(teacherId);
        return ApiResponse.builder()
                .status(HttpStatus.OK)
                .message(RestConstants.SUCCESS)
                .data(this.teacherContractMapper.toDto(teacherContract))
                .build();
    }

    @Override
    public ApiResponse deleteTeacherContract(Long id) {
        TeacherContract contract = this.teacherContractRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException(RestConstants.CONTRACT_NOT_FOUND));

        List<TeacherTable> teacherTableList = this.teacherTableRepository.findAllByTeacherContractId(contract.getId());
        if (teacherTableList.isEmpty()) {
            contract.setDeleted(true);
            contract.setActive(false);
            this.teacherContractRepository.save(contract);
            return ApiResponse.builder()
                    .status(HttpStatus.OK)
                    .message(RestConstants.SUCCESSFULLY_DELETED)
                    .build();
        } else {
            return ApiResponse.builder()
                    .status(HttpStatus.BAD_REQUEST)
                    .message("SHU SHARTNOMGA BIRIKTIRILGAN TABELLAR MAVJUD!")
                    .build();
        }
    }

    @Override
    public ApiResponse updateContract(TeacherContractDto.CreateContractDto dto, Long id) {

        User teacher = this.userRepository.findByIdAndDeletedFalse(dto.getTeacherId())
                .orElseThrow(() -> new ResourceNotFoundException(RestConstants.USER_NOT_FOUND));

        TeacherContract teacherContract = this.teacherContractRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException(RestConstants.CONTRACT_NOT_FOUND));

        this.teacherContractMapper.toUpdate(teacherContract, dto);
        teacherContract.setTeacher(teacher);
        TeacherContract contract = this.teacherContractRepository.save(teacherContract);
        try {
            this.employeeContractGoogleSheet.updateEmployeeContract(contract);
        } catch (Exception e) {
            log.info(e.getMessage());
        }
        return ApiResponse.builder()
                .status(HttpStatus.OK)
                .message(RestConstants.SUCCESSFULLY_UPDATED)
                .data(this.teacherContractMapper.toDto(contract))
                .build();
    }

    @Override
    public ApiResponse getActiveContractOfTeacher(Long teacherId) {
        TeacherContract teacherContract = this.teacherContractRepository.findActiveContractByTeacherId(teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("ACTIVE SHARTNOMALAR TOPILMADI!"));
        TeacherContractDto contractDto = this.teacherContractMapper.toDto(teacherContract);
        return ApiResponse.builder()
                .status(HttpStatus.OK)
                .message("SUCCESS")
                .data(contractDto)
                .build();
    }

    @Override
    public ApiResponse searchByName(String keyword, Pageable pageable) {
        Page<TeacherContract> teacherContracts;

        if (keyword == null || keyword.trim().isEmpty()) {
            teacherContracts = this.teacherContractRepository.findAllByDeletedIsFalse(pageable);
        } else {
            teacherContracts = this.teacherContractRepository.searchByName(keyword, pageable);
        }
        return ApiResponse.builder()
                .status(HttpStatus.OK)
                .data(this.teacherContractMapper.toDto(teacherContracts.getContent()))
                .pages(teacherContracts.getTotalPages())
                .elements(teacherContracts.getTotalElements())
                .build();
    }

    @Override
    public ApiResponse filter(TeacherContractFilter filter, Pageable pageable) {
        Page<TeacherContract> contractPage = this.teacherContractRepository.findAll(filter, pageable);
        if (contractPage.isEmpty()) {
            return ApiResponse.builder()
                    .status(HttpStatus.OK)
                    .data(new ArrayList<>())
                    .build();
        }
        return ApiResponse.builder()
                .status(HttpStatus.OK)
                .message(RestConstants.SUCCESS)
                .data(this.teacherContractMapper.toDto(contractPage.getContent()))
                .pages(contractPage.getTotalPages())
                .elements(contractPage.getTotalElements())
                .build();
    }

    @Transactional
    @Override
    public ApiResponse deleteContracts(List<Long> ids) {

        List<Long> deletedTrue = this.teacherContractRepository.findAllByIdsAndDeletedTrue(ids);
        if (deletedTrue.isEmpty()) {
            return ApiResponse.builder()
                    .status(HttpStatus.OK)
                    .message("O'CHIRISH UCHUN SHARTNOMALAR TOPILMADI!")
                    .build();
        }

        List<Long> list = this.teacherTableRepository.findAllByIds(deletedTrue);
        if (!list.isEmpty()) {
            return ApiResponse.builder()
                    .status(HttpStatus.BAD_REQUEST)
                    .message("TANLANGAN SHARTNOMALARNI O'CHIRIB BO'LMAYDI, SABABI SHU SHARTNOMALARGA TEGISHLI BO'LGAN TABELLARNI O'CHIRISHINGIZ KERAK!")
                    .build();
        }
        this.teacherContractRepository.deleteAllByIds(deletedTrue);
        return ApiResponse.builder()
                .status(HttpStatus.OK)
                .message("TANLANGAN SHARTNOMALAR BUTUNLAY O'CHIRILDI!")
                .build();
    }

    @Override
    public ApiResponse terminateAndActivateContract(Long contractId, LocalDate date, Boolean status) {
        TeacherContract contract = this.teacherContractRepository.findByIdAndDFalseAndActive(contractId)
                .orElseThrow(() -> new ResourceNotFoundException(RestConstants.CONTRACT_NOT_FOUND));

        if (contract.getActive() == Boolean.TRUE) {
            long count = this.teacherTableRepository.findByTeacherIdAndYear(contractId);
            if (count > 0) {
                return ApiResponse.builder()
                        .status(HttpStatus.BAD_REQUEST)
                        .message("SHARTNOMA YAKUNLASH UCHUN, AKTIV TABELLARNI O'CHIRISHINGIZ KERAK!")
                        .build();
            }
            contract.setActive(false);
            contract.setEndDate(date);
            contract = this.teacherContractRepository.save(contract);
            try {
                this.employeeContractGoogleSheet.initializeSheet();
                this.employeeContractGoogleSheet.updateEmployeeContract(contract);
            } catch (Exception e) {
                log.error(e.getMessage());
            }
            return ApiResponse.builder()
                    .status(HttpStatus.OK)
                    .message("SHARTNOMA MUVAFFAQIYATLI YAKUNLANDI.")
                    .build();
        } else if (contract.getActive() == Boolean.FALSE) {
            // Birinchi: Shu xodimning boshqa faol shartnomasi bor-yo'qligini tekshirish
            List<TeacherContract> activeContracts = this.teacherContractRepository
                    .findByTeacherIdAndActiveTrue(contract.getTeacher().getId());

            if (!activeContracts.isEmpty()) {
                return ApiResponse.builder()
                        .status(HttpStatus.BAD_REQUEST)
                        .message("BU XODIM BILAN ALLAQACHON FAOL SHARTNOMA MAVJUD! " +
                                "AVVAL UNI TO'XTATIB, KEYIN YANGISINI FAOLLASHTIRISHINGIZ MUMKIN.")
                        .build();
            }

            // SHARTNOMASNI TO'XTATISH
            long activeTablesCount = this.teacherTableRepository.findByTeacherIdAndYear(contractId);

            if (activeTablesCount > 0) {
                return ApiResponse.builder()
                        .status(HttpStatus.BAD_REQUEST)
                        .message("SHARTNOMA YAKUNLASH UCHUN, AKTIV TABELLARNI O'CHIRISHINGIZ KERAK!")
                        .build();
            }
            contract.setActive(true);
            contract.setEndDate(date);
            this.teacherContractRepository.save(contract);
            return ApiResponse.builder()
                    .status(HttpStatus.OK)
                    .message("SHARTNOMA AKTIV QILINDI.")
                    .build();
        }
        return ApiResponse.builder()
                .status(HttpStatus.OK)
                .message("BU SHARNOMANI STATUSI NULL")
                .build();
    }


    @Override
    public void downloadExcel(TeacherContractSheetFilter filter, OutputStream outputStream) {
        List<TeacherContract> allByStatus = this.teacherContractRepository.findAll(filter);

        log.info(allByStatus.toString());
        if (allByStatus.isEmpty()) {
            throw new ResourceNotFoundException("Ma'lumot topilmadi!");
        }

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("shartnomalar");

            // Column headers
            String[] columns = {
                    "ID",
                    "Xodim",
                    "Oylik Turi",
                    "Oylik/Kunlik SUMMA",
                    "Sh.Boshlanish sanasi",
                    "Sh.Tugash sanasi",
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
            for (TeacherContract expenses : allByStatus) {
                Row row = sheet.createRow(rowIdx++);
                row.setHeightInPoints(18); // 🔹 Increase Row Height
                Cell cell0 = row.createCell(0);
                cell0.setCellValue(expenses.getId());
                cell0.setCellStyle(dataStyle);

                Cell cell1 = row.createCell(1);
                cell1.setCellValue(expenses.getTeacher().getFullName() != null ? expenses.getTeacher().getFullName() : "N/A");
                cell1.setCellStyle(dataStyle);
                row.createCell(2).setCellValue(expenses.getSalaryType() != null ? expenses.getSalaryType().name() : "N/A");
                row.createCell(3).setCellValue(expenses.getMonthlySalaryOrPerLessonOrPerDay() != null ? expenses.getMonthlySalaryOrPerLessonOrPerDay() : 0.0);
                row.createCell(4).setCellValue(
                        expenses.getStartDate() != null
                                ? expenses.getStartDate().format(DateTimeFormatter.ofPattern("yyyy.MM.dd"))
                                : "N/A"
                );

                row.createCell(5).setCellValue(
                        expenses.getEndDate() != null
                                ? expenses.getEndDate().format(DateTimeFormatter.ofPattern("yyyy.MM.dd"))
                                : "N/A"
                );

                row.createCell(6).setCellValue(formatDateTime(expenses.getCreatedAt(), "yyyy.MM.dd HH:mm"));

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

    private String formatDateTime(LocalDateTime dt, String pattern) {
        return dt != null ? dt.format(DateTimeFormatter.ofPattern(pattern)) : "N/A";
    }


    private void setBorders(CellStyle style) {
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
    }

//    @Scheduled(cron = "0 0 0 * * *") // Every day at midnight
//    public void deactivateExpiredContracts() {
//        List<TeacherContract> expiredContracts = teacherContractRepository
//                .findExpiredContracts(LocalDate.now());
//
//        for (TeacherContract contract : expiredContracts) {
//            contract.setActive(false);
//            contract.setEndDate(LocalDate.now());
//        }
//        teacherContractRepository.saveAll(expiredContracts);
//    }

}
