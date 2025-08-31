package org.example.moliyaapp.mapper;

import org.example.moliyaapp.dto.ReminderDto;
import org.example.moliyaapp.entity.Reminder;
import org.example.moliyaapp.entity.StudentContract;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class ReminderMapper {

    public ReminderDto toDto(Reminder reminder) {
        return ReminderDto.builder()
                .id(reminder.getId())
                .studentContractId(reminder.getStudentContract().getId())
                .month(reminder.getMonth())
                .isReminded(reminder.getIsReminded())
                .estimatedTime(reminder.getEstimatedTime())
                .comment(reminder.getComment())
                .createdAt(reminder.getCreatedAt())
                .updatedAt(reminder.getUpdatedAt())
                .createdBy(reminder.getCreatedBy())
                .updatedBy(reminder.getUpdatedBy())
                .deleted(reminder.getDeleted())
                .build();
    }
    public ReminderDto.ReminderFilterResponse toReminderDto(Reminder reminder){
        StudentContract studentContract = reminder.getStudentContract();
        return ReminderDto.ReminderFilterResponse.builder()
                .id(reminder.getId())
                .studentContractId(studentContract.getId())
                .month(reminder.getMonth())
                .isReminded(reminder.getIsReminded())
                .estimatedTime(reminder.getEstimatedTime())
                .comment(reminder.getComment())
                .studentName(studentContract.getStudentFullName()!=null?studentContract.getStudentFullName():null)
                .gender(studentContract.getGender()!=null?studentContract.getGender():null)
                .grade(studentContract.getGrade()!=null?studentContract.getGrade():null)
                .status(studentContract.getStatus()!=null?studentContract.getStatus():null)
                .stGrade(studentContract.getStGrade()!=null?studentContract.getStGrade():null)
                .academicYear(studentContract.getAcademicYear()!=null?studentContract.getAcademicYear():null)
                .createdAt(reminder.getCreatedAt())
                .updatedAt(reminder.getUpdatedAt())
                .createdBy(reminder.getCreatedBy())
                .updatedBy(reminder.getUpdatedBy())
                .deleted(reminder.getDeleted())
                .build();
    }

    public Reminder toEntity(ReminderDto.ReminderCreateAndUpdateDto dto) {
        if (dto == null) return null;
        return Reminder.builder()
                .month(dto.getMonth())
                .isReminded(dto.getIsReminded())
                .estimatedTime(dto.getEstimatedTime())
                .comment(dto.getComment())
                .build();
    }

    public void toUpdate(Reminder reminder, ReminderDto.ReminderCreateAndUpdateDto dto) {
        if (dto == null) return;

        if (dto.getMonth() != null) {
            reminder.setMonth(dto.getMonth());
        }
        if (dto.getIsReminded() != null) {
            reminder.setIsReminded(dto.getIsReminded());
        }
        if (dto.getEstimatedTime() != null) {
            reminder.setEstimatedTime(dto.getEstimatedTime());
        }
        if (dto.getComment() != null && !dto.getComment().trim().isEmpty()) {
            reminder.setComment(dto.getComment());
        }
    }

    public List<ReminderDto> toDto(List<Reminder> reminderList) {
        if (reminderList != null) {
            return reminderList
                    .stream()
                    .map(this::toDto)
                    .toList();
        }
        return new ArrayList<>();
    }
    public List<ReminderDto.ReminderFilterResponse> toReminderDto(List<Reminder> reminderList) {
        if (reminderList != null) {
            return reminderList
                    .stream()
                    .map(this::toReminderDto)
                    .toList();
        }
        return new ArrayList<>();
    }
}
