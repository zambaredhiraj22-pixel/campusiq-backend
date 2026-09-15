package com.campusiq.service.impl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.campusiq.dto.StudentEligibilityData;
import com.campusiq.entity.MockTestResult;
import com.campusiq.entity.Skill;
import com.campusiq.entity.StudentProfile;
import com.campusiq.enums.SkillStatus;
import com.campusiq.exception.StudentProfileNotFoundException;
import com.campusiq.repository.MockTestResultRepository;
import com.campusiq.repository.SkillRepository;
import com.campusiq.repository.StudentProfileRepository;
import com.campusiq.service.StudentEligibilityDataProvider;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudentEligibilityDataProviderImpl
        implements StudentEligibilityDataProvider {

    private final StudentProfileRepository studentProfileRepository;

    private final SkillRepository skillRepository;

    private final MockTestResultRepository mockTestResultRepository;

    @Override
    public StudentEligibilityData getStudentEligibilityData(
            Long studentId) {

        StudentProfile studentProfile =
                studentProfileRepository
                        .findById(studentId)
                        .orElseThrow(() ->
                                new StudentProfileNotFoundException(
                                        "Student profile not found with id: "
                                                + studentId
                                )
                        );

        return mapToEligibilityData(studentProfile);
    }

    @Override
    public List<StudentEligibilityData>
            getAllStudentsEligibilityData() {

        List<StudentProfile> studentProfiles =
                studentProfileRepository.findAll();

        List<StudentEligibilityData> eligibilityDataList =
                new ArrayList<>();

        for (StudentProfile studentProfile : studentProfiles) {

            eligibilityDataList.add(
                    mapToEligibilityData(studentProfile)
            );
        }

        return eligibilityDataList;
    }

    private StudentEligibilityData mapToEligibilityData(
            StudentProfile studentProfile) {

        Set<String> verifiedSkills =
                getVerifiedSkills(studentProfile);

        Double latestMockTestScore =
                getLatestMockTestScore(studentProfile);

        StudentEligibilityData data =
                new StudentEligibilityData();

        data.setStudentId(studentProfile.getId());

        data.setTenthPercentage(
                studentProfile.getTenthPercentage());

        data.setTwelfthPercentage(
                studentProfile.getTwelfthPercentage());

        data.setDiplomaPercentage(
                studentProfile.getDiplomaPercentage());

        data.setCgpa(
                studentProfile.getCgpa());

        data.setDepartment(
                studentProfile.getDepartment());

        data.setAttendancePercentage(
                studentProfile.getAttendancePercentage());

        data.setVerifiedSkills(verifiedSkills);

        data.setLatestMockTestScore(latestMockTestScore);

        return data;
    }

    private Set<String> getVerifiedSkills(
            StudentProfile studentProfile) {

        List<Skill> verifiedSkillEntities =
                skillRepository.findByStudentProfileAndStatus(
                        studentProfile,
                        SkillStatus.VERIFIED
                );

        Set<String> verifiedSkills = new HashSet<>();

        for (Skill skill : verifiedSkillEntities) {

            verifiedSkills.add(
                    skill.getSkillName()
            );
        }

        return verifiedSkills;
    }

    private Double getLatestMockTestScore(
            StudentProfile studentProfile) {

        return mockTestResultRepository
                .findTopByStudentProfileOrderByAttemptedAtDesc(
                        studentProfile
                )
                .map(MockTestResult::getPercentage)
                .orElse(null);
    }
}