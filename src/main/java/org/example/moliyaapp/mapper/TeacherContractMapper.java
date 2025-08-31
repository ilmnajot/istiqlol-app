package org.example.moliyaapp.mapper;

import org.example.moliyaapp.dto.TeacherContractDto;
import org.example.moliyaapp.dto.UserDto;
import org.example.moliyaapp.entity.TeacherContract;
import org.example.moliyaapp.entity.User;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class TeacherContractMapper {

    private final UserMapper userMapper;

    public TeacherContractMapper(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    public TeacherContractDto toDto(TeacherContract teacherContract) {
        User user = teacherContract.getTeacher();
        UserDto userDto = userMapper.toDto(user);
        return TeacherContractDto.builder()
                .id(teacherContract.getId())
                .userDto(userDto)
                .salaryType(teacherContract.getSalaryType())
                .monthlySalaryOrPerLessonOrPerDay(teacherContract.getMonthlySalaryOrPerLessonOrPerDay())
                .startDate(teacherContract.getStartDate())
                .endDate(teacherContract.getEndDate())
                .active(teacherContract.getActive())
                .createdAt(teacherContract.getCreatedAt())
                .updatedAt(teacherContract.getUpdatedAt())
                .createdBy(teacherContract.getCreatedBy())
                .updatedBy(teacherContract.getUpdatedBy())
                .deleted(teacherContract.getDeleted())
                .build();
    }

    public TeacherContract toEntity(TeacherContractDto.CreateContractDto teacherContractDto) {
        return TeacherContract.builder()
                .salaryType(teacherContractDto.getSalaryType())
                .monthlySalaryOrPerLessonOrPerDay(teacherContractDto.getMonthlySalaryOrPerLessonOrPerDay())
                .startDate(teacherContractDto.getStartDate())
                .endDate(teacherContractDto.getEndDate())
                .build();
    }

    public List<TeacherContractDto> toDto(List<TeacherContract> list) {
        if (list != null && !list.isEmpty()) {
            return list
                    .stream()
                    .map(this::toDto)
                    .toList();
        }
        return new ArrayList<>();
    }

    public void toUpdate(TeacherContract contract, TeacherContractDto.CreateContractDto dto) {
        if (dto.getSalaryType() != null) {
            contract.setSalaryType(dto.getSalaryType());
        }
        if (dto.getMonthlySalaryOrPerLessonOrPerDay() != null && dto.getMonthlySalaryOrPerLessonOrPerDay() > 0) {
            contract.setMonthlySalaryOrPerLessonOrPerDay(dto.getMonthlySalaryOrPerLessonOrPerDay());
        }
        if (dto.getStartDate() != null) {
            contract.setStartDate(dto.getStartDate());
        }
        contract.setEndDate(dto.getEndDate());
    }
}
