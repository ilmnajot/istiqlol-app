package org.example.moliyaapp.service;

import org.example.moliyaapp.dto.ApiResponse;
import org.example.moliyaapp.dto.RegisterDto;
import org.example.moliyaapp.dto.UserDto;
import org.example.moliyaapp.filter.EmployeeFilter;
import org.example.moliyaapp.filter.EmployeeSheetFilter;
import org.example.moliyaapp.filter.TeacherTabelSheetFilter;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.util.List;

@Component
public interface EmployeeService {
    ApiResponse createStudent(UserDto.CreateStudent student);

    ApiResponse createTeacher(UserDto.CreateEmployee teacher);

    ApiResponse getAllUserByRole(String role);

    ApiResponse createEmployee(RegisterDto.RegisterEmployee dto);

    ApiResponse getEmployee(Long id);

//    ApiResponse getBySalaryType(SalaryType salaryType, Months months);

    ApiResponse getAllActiveEmployees(Pageable pageable, EmployeeFilter role);

    ApiResponse getAllByRole(Long roleId, int page, int size);

    ApiResponse deleteEmployee(Long id);

    ApiResponse updateEmployee(Long id, UserDto.UpdateUser dto);

    ApiResponse getAllTeachersByGroup(Long groupId, int page, int size);

    ApiResponse getAllEmployeeList();

    ApiResponse getTutorsByGroup(Long groupId, int page, int size);

    ApiResponse getGroupsByTeacher(Long groupId, int page, int size);

    ApiResponse filter(EmployeeFilter filter, int page, int size);

    ApiResponse searchEmployee(String name, Pageable pageable);

    ApiResponse getAllInActiveEmployees(Pageable pageable, EmployeeFilter filter);

    ApiResponse deleteUsers(List<Long> ids);

    ByteArrayInputStream downloadExcel(EmployeeSheetFilter filter);
}
