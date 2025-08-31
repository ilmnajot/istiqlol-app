package org.example.moliyaapp.mapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.moliyaapp.dto.MonthlyFeeDto;
import org.example.moliyaapp.dto.ReminderDto;
import org.example.moliyaapp.dto.StudentContractDto;
import org.example.moliyaapp.dto.StudentTariffDto;
import org.example.moliyaapp.entity.MonthlyFee;
import org.example.moliyaapp.entity.Reminder;
import org.example.moliyaapp.entity.StudentContract;
import org.example.moliyaapp.entity.StudentTariff;
import org.example.moliyaapp.enums.Months;
import org.example.moliyaapp.filter.StudentContractFilter;
import org.example.moliyaapp.repository.MonthlyFeeRepository;
import org.example.moliyaapp.repository.ReminderRepository;
import org.example.moliyaapp.utils.Utils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Component
@Slf4j
public class StudentContractMapper {


    private final StudentTariffMapper studentTariffMapper;
    private final MonthlyFeeRepository monthlyFeeRepository;
    private final MonthlyFeeMapper monthlyFeeMapper;
    private final ReminderMapper reminderMapper;
    private final ReminderRepository reminderRepository;

    public StudentContract toStudentContract(StudentContractDto.CreateStudentContractDto dto) {
        StudentContract contract = new StudentContract();
        contract.setContractedDate(dto.getContractedDate());
        contract.setStudentFullName(dto.getStudentFullName());
        contract.setBirthDate(dto.getBirthDate());
        contract.setGender(dto.getGender());
        contract.setGrade(dto.getGrade());
        contract.setStGrade(dto.getStGrade());
        contract.setLanguage(dto.getLanguage());
        contract.setGuardianFullName(dto.getGuardianFullName());
        contract.setGuardianType(dto.getGuardianType());
        contract.setGuardianJSHSHIR(dto.getGuardianJSHSHIR());
        contract.setPassportId(dto.getPassportId());
        contract.setPassportIssuedBy(dto.getPassportIssuedBy());
        contract.setAcademicYear(dto.getAcademicYear());
        contract.setPhone1(dto.getPhone1());
        contract.setPhone2(dto.getPhone2());
        contract.setAddress(dto.getAddress());
        contract.setBCN(dto.getBCN());
        contract.setComment(dto.getComment());
        contract.setContractStartDate(dto.getContractStartDate());
        contract.setContractEndDate(dto.getContractEndDate());
        contract.setClientType(dto.getClientType());
        contract.setStatus(dto.getStatus());
        contract.setInactiveDate(null);
//        contract.setUniqueId(dto.getUniqueId());
        return contract;
    }

    public StudentContractDto toStudentContractDtoResponse(StudentContract contract) {
        StudentTariff tariff = contract.getTariff();
        StudentTariffDto tariffDto = this.studentTariffMapper.toDto(tariff);
        List<MonthlyFeeDto> dtoedList=null;
        List<MonthlyFee> feeList = this.monthlyFeeRepository.findAllByStudentContractId(contract.getId());
        if (!feeList.isEmpty()) {
            dtoedList = this.monthlyFeeMapper.dtoList(feeList);
        }
        List<Reminder> reminderList = this.reminderRepository.findAllByStudentContractId(contract.getId());
        List<ReminderDto> responseList = this.reminderMapper.toDto(reminderList);

        StudentContractDto contractDto = new StudentContractDto();
        contractDto.setId(contract.getId());
        contractDto.setContractedDate(contract.getContractedDate());
        contractDto.setStudentFullName(contract.getStudentFullName());
        contractDto.setBirthDate(contract.getBirthDate());
        contractDto.setGender(contract.getGender());
        contractDto.setGrade(contract.getGrade());
        contractDto.setStGrade(contract.getStGrade());
        contractDto.setLanguage(contract.getLanguage());
        contractDto.setCompanyId(contract.getCompany() != null ? contract.getCompany().getId() : 1);
        contractDto.setGuardianFullName(contract.getGuardianFullName());
        contractDto.setGuardianType(contract.getGuardianType());
        contractDto.setGuardianJSHSHIR(contract.getGuardianJSHSHIR());
        contractDto.setPassportId(contract.getPassportId());
        contractDto.setPassportIssuedBy(contract.getPassportIssuedBy());
        contractDto.setAcademicYear(contract.getAcademicYear());
        contractDto.setPhone1(contract.getPhone1());
        contractDto.setPhone2(contract.getPhone2());
        contractDto.setAddress(contract.getAddress());
        contractDto.setComment(contract.getComment());
        contractDto.setBCN(contract.getBCN());
        contractDto.setAmount(contract.getAmount());
        contractDto.setContractStartDate(contract.getContractStartDate());
        contractDto.setContractEndDate(contract.getContractEndDate());
        contractDto.setClientType(contract.getClientType());
        contractDto.setStatus(contract.getStatus());
        contractDto.setInactiveDate(contract.getInactiveDate());
        contractDto.setCreatedAt(contract.getCreatedAt());
        contractDto.setUpdatedAt(contract.getUpdatedAt());
        contractDto.setCreatedBy(contract.getCreatedBy());
        contractDto.setUpdatedBy(contract.getUpdatedBy());
        contractDto.setDeleted(contract.getDeleted());
        contractDto.setAmount(contract.getAmount());
        contractDto.setTariffDto(tariffDto);
        contractDto.setPayments(dtoedList);
        contractDto.setReminderDtos(responseList);
        contractDto.setUniqueId(contract.getUniqueId());
        return contractDto;
    }


    public void updateStudentContract(StudentContract contract, StudentContractDto.UpdateStudentContractDto dto) {

        contract.setContractedDate(Utils.getIfExists(dto.getContractedDate(), contract.getContractedDate()));
        contract.setStudentFullName(Utils.getIfExists(dto.getStudentFullName(), contract.getStudentFullName()));
        contract.setBirthDate(Utils.getIfExists(dto.getBirthDate(), contract.getBirthDate()));
        contract.setGender(Utils.getIfExists(dto.getGender(), contract.getGender()));
        contract.setGrade(Utils.getIfExists(dto.getGrade(), contract.getGrade()));
        contract.setStGrade(Utils.getIfExists(dto.getStGrade(), contract.getStGrade()));
        contract.setLanguage(Utils.getIfExists(dto.getLanguage(), contract.getLanguage()));
        contract.setGuardianFullName(Utils.getIfExists(dto.getGuardianFullName(), contract.getGuardianFullName()));
        contract.setGuardianType(Utils.getIfExists(dto.getGuardianType(), contract.getGuardianType()));
        contract.setGuardianJSHSHIR(Utils.getIfExists(dto.getGuardianJSHSHIR(), contract.getGuardianJSHSHIR()));
        contract.setPassportId(Utils.getIfExists(dto.getPassportId(), contract.getPassportId()));
        contract.setPassportIssuedBy(Utils.getIfExists(dto.getPassportIssuedBy(), contract.getPassportIssuedBy()));
        contract.setAcademicYear(Utils.getIfExists(dto.getAcademicYear(), contract.getAcademicYear()));
        contract.setPhone1(Utils.getIfExists(dto.getPhone1(), contract.getPhone1()));
        contract.setPhone2(Utils.getIfExists(dto.getPhone2(), contract.getPhone2()));
        contract.setAddress(Utils.getIfExists(dto.getAddress(), contract.getAddress()));
        contract.setComment(Utils.getIfExists(dto.getComment(), contract.getComment()));
        contract.setBCN(Utils.getIfExists(dto.getBCN(), contract.getBCN()));
        contract.setContractStartDate(Utils.getIfExists(dto.getContractStartDate(), contract.getContractStartDate()));
        contract.setContractEndDate(Utils.getIfExists(dto.getContractEndDate(), contract.getContractEndDate()));
        contract.setClientType(Utils.getIfExists(dto.getClientType(), contract.getClientType()));
        contract.setStatus(Utils.getIfExists(dto.getStatus(), contract.getStatus()));
        contract.setInactiveDate(Utils.getIfExists(dto.getInactiveDate(), contract.getInactiveDate()));

    }

    public List<StudentContractDto> dtoList(List<StudentContract> list) {
        if (list != null && !list.isEmpty()) {
            return list
                    .stream()
                    .map(this::toStudentContractDtoResponse)
                    .toList();
        }
        return new ArrayList<>();
    }
}
