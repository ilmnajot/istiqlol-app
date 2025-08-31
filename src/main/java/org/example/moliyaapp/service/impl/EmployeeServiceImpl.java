package org.example.moliyaapp.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.example.moliyaapp.dto.ApiResponse;
import org.example.moliyaapp.dto.RegisterDto;
import org.example.moliyaapp.dto.UserDto;
import org.example.moliyaapp.entity.TeacherContract;
import org.example.moliyaapp.entity.User;
import org.example.moliyaapp.entity.UserRole;
import org.example.moliyaapp.enums.Role;
import org.example.moliyaapp.enums.UserStatus;
import org.example.moliyaapp.exception.AlreadyExistException;
import org.example.moliyaapp.exception.BadRequestException;
import org.example.moliyaapp.exception.ResourceNotFoundException;
import org.example.moliyaapp.filter.EmployeeFilter;
import org.example.moliyaapp.filter.EmployeeSheetFilter;
import org.example.moliyaapp.filter.TeacherContractSheetFilter;
import org.example.moliyaapp.mapper.EmployeeMapper;
import org.example.moliyaapp.mapper.UserMapper;
import org.example.moliyaapp.repository.RoleRepository;
import org.example.moliyaapp.repository.TeacherContractRepository;
import org.example.moliyaapp.repository.TeacherTableRepository;
import org.example.moliyaapp.repository.UserRepository;
import org.example.moliyaapp.service.EmployeeService;
import org.example.moliyaapp.utils.RestConstants;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmployeeServiceImpl implements EmployeeService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final RoleRepository roleRepository;
    private final EmployeeMapper employeeMapper;
    private final TeacherTableRepository teacherTableRepository;
    private final TeacherContractRepository teacherContractRepository;

    @Override
    public ApiResponse createStudent(UserDto.CreateStudent student) {
        UserRole role = this.roleRepository.findByNameAndDeletedFalse("STUDENT")
                .orElseThrow(() -> new ResourceNotFoundException(RestConstants.ROLE_NOT_FOUND));
        User userStudent = this.userMapper.toStudent(student);
        userStudent.setCreatedAt(LocalDateTime.now());
        userStudent.setRole(new HashSet<>(Set.of(role)));
        this.userRepository.save(userStudent);
        return ApiResponse.builder()
                .status(HttpStatus.OK)
                .message(RestConstants.SUCCESSFULLY_SAVED)
                .build();
    }

    @Override
    public ApiResponse createTeacher(UserDto.CreateEmployee teacher) {
        UserRole role = this.roleRepository.findByNameAndDeletedFalse(Role.EMPLOYEE.name())
                .orElseThrow(() -> new ResourceNotFoundException(RestConstants.ROLE_NOT_FOUND));
        User teacherStudent = this.userMapper.toTeacher(teacher);
        teacherStudent.setCreatedAt(LocalDateTime.now());
        teacherStudent.setRole(new HashSet<>(Set.of(role)));
        this.userRepository.save(teacherStudent);
        return ApiResponse.builder()
                .status(HttpStatus.OK)
                .message(RestConstants.SUCCESSFULLY_SAVED)
                .build();
    }

    @Override
    public ApiResponse getAllUserByRole(String role) {
        List<User> page = this.userRepository.getAllUserByRole(role);
        return ApiResponse.builder()
                .status(HttpStatus.OK)
                .data(this.userMapper.dtoList(page))
                .build();
    }

    @Override
    public ApiResponse createEmployee(RegisterDto.RegisterEmployee dto) {

        if (this.existsByContractNo(dto.getContractNumber())) {
            throw new AlreadyExistException("BU SHARTNOMA RAQAMI ALLAQACHON MAVHUD");
        }

        Optional<User> byPhoneNumber = Optional.empty();
        if (dto.getPhone() != null && !dto.getPhone().trim().isEmpty()) {
            byPhoneNumber = this.userRepository.findByPhoneNumberAndDeletedFalse(dto.getPhone());
        }
        Optional<User> byEmail = Optional.empty();
        if (dto.getEmail() != null && !dto.getEmail().trim().isEmpty()) {
            byEmail = this.userRepository.findByEmail(dto.getEmail());
        }
        if (byPhoneNumber.isPresent()) {
            throw new AlreadyExistException(RestConstants.PHONE_NUMBER_EXISTS);
        } else if (byEmail.isPresent()) {
            throw new BadRequestException(RestConstants.EMAIL_ALREADY_EXISTS);
        }

//        Set<UserRole> userRoleSet = this.roleRepository.findAllByNameIn(dto.getRoleNames());

//        User user = this.employeeMapper.toEmployeeEntity(dto, userRoleSet);
//        user.setStatus(UserStatus.ACTIVE);
//        user.setRole(userRoleSet);
//        user = this.userRepository.save(user);
//        UserDto.EmployeeResponse employeeResponse = this.employeeMapper.toEmployeeResponse(user);

        return ApiResponse.builder()
                .message(RestConstants.SUCCESSFULLY_SAVED)
                .status(HttpStatus.CREATED)
//                .data(employeeResponse)
                .build();
    }


    @Override
    public ApiResponse getEmployee(Long id) {
        User user = this.userRepository.findUserById(id)
                .orElseThrow(() -> new ResourceNotFoundException(RestConstants.USER_NOT_FOUND));
        UserDto.EmployeeResponse employeeResponse = this.employeeMapper.toEmployeeResponse(user);
        return ApiResponse.builder()
                .status(HttpStatus.OK)
                .message(RestConstants.SUCCESS)
                .data(employeeResponse)
                .build();
    }

    @Override
    public ApiResponse getAllActiveEmployees(Pageable pageable, EmployeeFilter filter) {
        Page<User> userPage = this.userRepository.findAll(filter, pageable);
        if (userPage.isEmpty()) {
            return ApiResponse.builder()
                    .status(HttpStatus.OK)
                    .data(new ArrayList<>())
                    .build();
        }
        return ApiResponse.builder()
                .status(HttpStatus.OK)
                .message(RestConstants.SUCCESS)
                .data(this.employeeMapper.toEmployeeList(userPage.getContent()))
                .build();
    }


    @Override
    public ApiResponse getAllTeachersByGroup(Long groupId, int page, int size) {
        this.userRepository.findAll();//todo
        return null;
    }


    @Override
    public ApiResponse getTutorsByGroup(Long groupId, int page, int size) {
        this.userRepository.findAll();  //todo
        return null;
    }

    @Override
    public ApiResponse getGroupsByTeacher(Long groupId, int page, int size) {
        return null;
    }

    @Override
    public ApiResponse filter(EmployeeFilter filter, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<User> userPage = this.userRepository.findAll(filter, pageable);
        if (!userPage.isEmpty()) {
            return ApiResponse.builder()
                    .status(HttpStatus.OK)
                    .message(RestConstants.SUCCESS)
                    .elements(userPage.getTotalElements())
                    .pages(userPage.getTotalPages())
                    .data(userPage.getContent().stream().map(this.employeeMapper::toEmployeeResponse).collect(Collectors.toSet()))
                    .build();
        }
        return ApiResponse.builder()
                .status(HttpStatus.OK)
                .data(new ArrayList<>())
                .build();
    }

    @Override
    public ApiResponse searchEmployee(String keyword, Pageable pageable) {
        Page<User> users = this.userRepository.searchEmployee(keyword, pageable);
        if (users.isEmpty()) {
            return ApiResponse.builder()
                    .status(HttpStatus.OK)
                    .data(new ArrayList<>())
                    .build();
        }
        return ApiResponse.builder()
                .status(HttpStatus.OK)
                .message("SUCCESS")
                .data(this.employeeMapper.toEmployeeList(users.getContent()))
                .elements(users.getTotalElements())
                .pages(users.getTotalPages())
                .build();
    }

    @Override
    public ApiResponse getAllInActiveEmployees(Pageable pageable, EmployeeFilter filter) {
//        Page<User> userPage = this.userRepository.findAll(filter, pageable);
//        if (!userPage.isEmpty()) {
//            return ApiResponse.builder()
//                    .status(HttpStatus.OK)
//                    .message(RestConstants.SUCCESS)
//                    .data(new ArrayList<>())
//                    .build();
//        }
//        return ApiResponse.builder()
//                .status(HttpStatus.OK)
//                .message(RestConstants.SUCCESS)
//                .data(this.employeeMapper.toEmployeeList(userPage.getContent()))
//                .pages(userPage.getTotalPages())
//                .elements(userPage.getTotalElements())
//                .build();
        return null;
    }

    @Override
    public ApiResponse deleteUsers(List<Long> ids) {
        List<Long> userIDs = this.userRepository.findAllById(ids);
        List<String> usernames = this.teacherContractRepository.findAllByUserIds(userIDs);
        List<String> list = this.teacherTableRepository.findAllByUserIds(userIDs);
        if (!list.isEmpty()) {
            return ApiResponse.builder()
                    .status(HttpStatus.BAD_REQUEST)
                    .message("QUYDAGI FOYDALANUVCHILARNI TABELLARI MAVJUDLIGI SABABLI O'CHIRIB BO'LMAYDI: " + String.join(", ", list))

                    .build();
        }
        if (!usernames.isEmpty()) {
            return ApiResponse.builder()
                    .status(HttpStatus.BAD_REQUEST)
                    .message("QUYDAGI FOYDALANUVCHILARNI SHARTNOMALR MAVJUDLIGI SABABLI O'CHIRIB BO'LMAYDI: " + String.join(", ", usernames))
                    .build();
        }
        if (userIDs.isEmpty()) {
            return ApiResponse.builder()
                    .status(HttpStatus.OK)
                    .message("O'CHIRISH UCHUN FOYDALANUVCHILAR TOPILMADI!")
                    .build();
        }
        this.userRepository.deleteAllById(ids);
        return ApiResponse.builder()
                .status(HttpStatus.OK)
                .message("TANLANGAN FOYDALANUVCHILAR ARXIVDA BUNUTLAY MUVAFFAQIYATLI O'CHIRILDI.")
                .build();
    }


    @Override
    public ApiResponse getAllByRole(Long positionId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<User> userPage = this.userRepository.getAllActiveByRoleId(positionId, pageable);
        List<UserDto.EmployeeResponse> employeeList = this.employeeMapper.toEmployeeList(userPage.getContent());
        return ApiResponse.builder()
                .message(RestConstants.SUCCESS)
                .pages(userPage.getTotalPages())
                .elements(userPage.getTotalElements())
                .data(employeeList)
                .build();
    }

    @Override
    public ApiResponse deleteEmployee(Long id) {
        User user = this.userRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException(RestConstants.USER_NOT_FOUND));
        List<TeacherContract> contractList = this.teacherContractRepository.findAllByIdAndActiveTrueAndDeletedFalse(user.getId());
        if (!contractList.isEmpty()) {
            return ApiResponse.builder()
                    .status(HttpStatus.BAD_REQUEST)
                    .message("BU FOYDALANUVCHIDA AKTIV SHARTNOMA MAVJUD, FOYDALANUVCHINI O'CHIRISH UCHUN AVVAL SHARTONAMI BEKOR QILISH YOKI O'CHIRISH KERAK BO'LADI! ")
                    .build();
        }
        user.setDeleted(true);
        user.setStatus(UserStatus.PENDING);
        this.userRepository.save(user);
        return ApiResponse.builder()
                .status(HttpStatus.OK)
                .message(RestConstants.SUCCESS)
                .build();
    }

    @Override
    public ApiResponse updateEmployee(Long id, UserDto.UpdateUser dto) {
        User user = this.userRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException(RestConstants.USER_NOT_FOUND));

        if (this.existsContractNo(user.getId(), dto.getContractNo())) {
            throw new AlreadyExistException("Bu shartnoma raqam allaqachon mabjud!");
        }
        UserRole userRole = this.roleRepository.findByNameAndDeletedFalse(dto.getRoleName())
                .orElseThrow(() -> new ResourceNotFoundException(RestConstants.ROLE_NOT_FOUND));

        User updateEmployee = this.employeeMapper.toUpdateEmployee(user, dto); //todo

        this.roleRepository.existsByNameAndDeletedFalse(dto.getRoleName());

        if (user.getRole() != null && !user.getRole().isEmpty()) {
            this.roleRepository.deleteUserRoleByUserId(user.getId());
        }
        updateEmployee.setRole(new HashSet<>(Set.of(userRole)));

        User save = this.userRepository.save(updateEmployee);
        UserDto.EmployeeResponse employeeResponse = this.employeeMapper.toEmployeeResponse(save);
        return ApiResponse.builder()
                .message(RestConstants.SUCCESS)
                .status(HttpStatus.OK)
                .data(employeeResponse)
                .build();
    }

    private boolean existsContractNo(Long id, String contractNo) {
        return this.userRepository.existsByContractNumberAndIdNot(contractNo, id);
    }

    private boolean existsByContractNo(String contractNo) {
        return this.userRepository.existsByContractNumber(contractNo);
    }

    public ApiResponse getAllEmployeeList() {
        List<User> all = this.userRepository.getAllActiveUsers();
        if (all.isEmpty()) {
            return ApiResponse.builder()
                    .status(HttpStatus.OK)
                    .data(new ArrayList<>())
                    .build();
        }
        return ApiResponse.builder()
                .message(RestConstants.SUCCESS)
                .data(this.employeeMapper.toEmployeeList(all))
                .status(HttpStatus.OK)
                .build();
    }


    @Override
    public ByteArrayInputStream downloadExcel(EmployeeSheetFilter filter) {
        List<User> allByStatus = this.userRepository.findAll(filter);

        log.info(allByStatus.toString());
        if (allByStatus.isEmpty()) {
            throw new ResourceNotFoundException("Ma'lumot topilmadi!");
        }

        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            XSSFSheet sheet = workbook.createSheet("Students");

            // Column headers
            String[] columns = {
                    "ID",
                    "Xodim",
                    "email",
                    "telefon raqam",
                    "Shartnonma raqami",
                    "Yaratilgan sana"
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
            dataFont.setFontHeightInPoints((short) 10);
            dataStyle.setFont(dataFont); // Don't forget to set the font!
            dataStyle.setAlignment(HorizontalAlignment.CENTER);
            dataStyle.setFillForegroundColor(IndexedColors.WHITE.getIndex());
            dataStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            setBorders(dataStyle);

            // Create Header Row with increased height
            XSSFRow headerRow = sheet.createRow(0);
            headerRow.setHeightInPoints(20);
            for (int i = 0; i < columns.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columns[i]);
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, 5000);
            }

            // Fill data rows
            int rowIdx = 1;
            for (User user : allByStatus) {
                XSSFRow row = sheet.createRow(rowIdx++);
                row.setHeightInPoints(18);

                // Create and style all cells
                XSSFCell cell0 = row.createCell(0);
                cell0.setCellValue(user.getId());
                cell0.setCellStyle(dataStyle);

                XSSFCell cell1 = row.createCell(1);
                cell1.setCellValue(user.getFullName() != null ? user.getFullName() : "N/A");
                cell1.setCellStyle(dataStyle);

                XSSFCell cell2 = row.createCell(2);
                cell2.setCellValue(user.getEmail() != null ? user.getEmail() : "N/A");
                cell2.setCellStyle(dataStyle);

                XSSFCell cell3 = row.createCell(3);
                cell3.setCellValue(user.getPhoneNumber() != null ? user.getPhoneNumber() : "N/A");
                cell3.setCellStyle(dataStyle);

                // Fixed: Create cell 4 for "Shartnonma raqami" - add your logic here
                XSSFCell cell4 = row.createCell(4);
                cell4.setCellValue(user.getContractNumber() != null ? user.getContractNumber() : "N/A"); // Replace with actual contract number field
                cell4.setCellStyle(dataStyle);

                // Fixed: Cell 5 for creation date (was incorrectly at index 4)
                XSSFCell cell5 = row.createCell(5);
                cell5.setCellValue(
                        user.getCreatedAt() != null
                                ? user.getCreatedAt().format(java.time.format.DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm"))
                                : "N/A"
                );
                cell5.setCellStyle(dataStyle);
            }

            // Auto-size columns
            for (int i = 0; i < columns.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());
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

}

