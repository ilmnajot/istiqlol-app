package org.example.moliyaapp.mapper;

import lombok.RequiredArgsConstructor;
import org.example.moliyaapp.dto.*;
import org.example.moliyaapp.entity.*;
import org.example.moliyaapp.exception.ResourceNotFoundException;
import org.example.moliyaapp.repository.MonthlyFeeRepository;
import org.example.moliyaapp.repository.TeacherContractRepository;
import org.example.moliyaapp.repository.TeacherTableRepository;
import org.example.moliyaapp.utils.RestConstants;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Component
public class EmployeeMapper {

    private final TeacherTableRepository teacherTableRepository;
    private final TeacherTableMapper teacherTableMapper;
    private final TeacherContractRepository teacherContractRepository;
    private final TeacherContractMapper teacherContractMapper;


    public User toEmployeeEntity(RegisterDto.RegisterEmployee dto, Set<UserRole> userRoles) {
        if (dto == null) return null;
        User user = new User();
        user.setFullName(dto.getFullName());
        user.setEmail(dto.getEmail() != null ? dto.getEmail() : null);
        user.setPhoneNumber(dto.getPhone());
        user.setContractNumber(dto.getContractNumber());
        user.setRole(userRoles);
        return user;
    }

    public UserDto.EmployeeResponse toEmployeeResponse(User user) {

        List<TeacherTable> tableList = this.teacherTableRepository.findAllByTeacherIdAndDeletedFalse(user.getId());
        List<TeacherTableDto> tableDtoList = teacherTableMapper.toDto(tableList);
        TeacherContract contract = this.teacherContractRepository.findByTeacherIdAndDeletedFalse(user.getId())
                .orElse(null);
        TeacherContractDto dto=null;
        if (contract != null) {
            dto = this.teacherContractMapper.toDto(contract);
        }

        UserDto.EmployeeResponse response = new UserDto.EmployeeResponse();

        response.setId(user.getId());
        response.setFullName(user.getFullName());
        response.setEmail(user.getEmail());
        response.setPhoneNumber(user.getPhoneNumber());
        response.setContractNo(user.getContractNumber());
        response.setRoleNames(user.getRole()
                .stream()
                .map(UserRole::getName)
                .collect(Collectors.toSet()));
        response.setUserStatus(user.getStatus());
        response.setTeacherTableDtoList(tableDtoList);
        response.setTeacherContractDto(dto);

        response.setCreatedAt(user.getCreatedAt());
        response.setCreatedBy(user.getCreatedBy());
        response.setUpdatedAt(user.getUpdatedAt());
        response.setUpdatedBy(user.getUpdatedBy());
        response.setDeleted(user.getDeleted());
        return response;
    }

    public List<UserDto.EmployeeResponse> toEmployeeList(List<User> userList) {
        if (userList != null && !userList.isEmpty()) {
            return userList
                    .stream()
                    .map(this::toEmployeeResponse)
                    .toList();
        }
        return new ArrayList<>();
    }

    public User toUpdateEmployee(User user, UserDto.UpdateUser dto) {
        if (dto.getFullName() != null) {
            user.setFullName(dto.getFullName());
        }
        if (dto.getPhoneNumber() != null) {
            user.setPhoneNumber(dto.getPhoneNumber());
        }
//        if (dto.getStartDate() != null) {
//            user.setStartDate(dto.getStartDate());
//        }
        if (dto.getContractNo() != null) {
            user.setContractNumber(dto.getContractNo());
        }

        return user;

    }
}
