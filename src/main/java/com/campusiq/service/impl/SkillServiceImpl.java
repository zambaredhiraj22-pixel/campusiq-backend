package com.campusiq.service.impl;

import java.util.List;

import org.springframework.beans.BeanUtils;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.campusiq.dto.SkillRequest;
import com.campusiq.dto.SkillResponse;
import com.campusiq.entity.Skill;
import com.campusiq.entity.StudentProfile;
import com.campusiq.entity.User;
import com.campusiq.enums.SkillStatus;
import com.campusiq.exception.SkillAlreadyExistsException;
import com.campusiq.exception.SkillNotFoundException;
import com.campusiq.exception.StudentProfileNotFoundException;
import com.campusiq.repository.SkillRepository;
import com.campusiq.repository.StudentProfileRepository;
import com.campusiq.repository.UserRepository;
import com.campusiq.service.SkillService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class SkillServiceImpl implements SkillService {

    private final SkillRepository skillRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final UserRepository userRepository;

    @Override
    public SkillResponse addSkill(
            String username,
            SkillRequest request) {

        StudentProfile profile = getProfile(username);

        String skillName = request
                .getSkillName()
                .trim();

        if (skillRepository
                .existsByStudentProfileAndSkillNameIgnoreCase(
                        profile,
                        skillName)) {

            throw new SkillAlreadyExistsException(
                    "Skill already exists: " + skillName
            );
        }

        Skill skill = new Skill();

        skill.setSkillName(skillName);

        skill.setProficiencyLevel(
                request
                        .getProficiencyLevel()
                        .trim()
                        .toUpperCase()
        );

        skill.setEvidenceUrl(
                request.getEvidenceUrl()
        );

        skill.setStatus(
                SkillStatus.PENDING
        );

        skill.setStudentProfile(profile);

        return convertToResponse(
                skillRepository.save(skill)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<SkillResponse> getMySkills(
            String username) {

        StudentProfile profile =
                getProfile(username);

        return skillRepository
                .findByStudentProfile(profile)
                .stream()
                .map(this::convertToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SkillResponse> getPendingSkills() {

        return skillRepository
                .findByStatus(
                        SkillStatus.PENDING
                )
                .stream()
                .map(this::convertToResponse)
                .toList();
    }

    /*
     * Faculty:
     *
     * Returns every skill that has already
     * been verified by Faculty.
     *
     * The personalized AI Mock Test frontend
     * will use this data to:
     *
     * 1. Identify students.
     * 2. Group skills by studentProfileId.
     * 3. Display only verified technical skills.
     * 4. Allow Faculty to select multiple skills
     *    for a personalized AI mock test.
     */
    @Override
    @Transactional(readOnly = true)
    public List<SkillResponse> getVerifiedSkills() {

        return skillRepository
                .findByStatus(
                        SkillStatus.VERIFIED
                )
                .stream()
                .map(this::convertToResponse)
                .toList();
    }

    @Override
    public SkillResponse updateSkillStatus(
            Long skillId,
            SkillStatus status) {

        Skill skill = skillRepository
                .findById(skillId)
                .orElseThrow(
                        () ->
                                new SkillNotFoundException(
                                        "Skill not found with id: "
                                                + skillId
                                )
                );

        skill.setStatus(status);

        return convertToResponse(
                skillRepository.save(skill)
        );
    }

    private StudentProfile getProfile(
            String username) {

        User user = userRepository
                .findByUsername(username)
                .orElseThrow(
                        () ->
                                new UsernameNotFoundException(
                                        "User not found: "
                                                + username
                                )
                );

        return studentProfileRepository
                .findByUser(user)
                .orElseThrow(
                        () ->
                                new StudentProfileNotFoundException(
                                        "Student profile not found"
                                )
                );
    }

    private SkillResponse convertToResponse(
            Skill skill) {

        SkillResponse response =
                new SkillResponse();

        BeanUtils.copyProperties(
                skill,
                response
        );

        StudentProfile profile =
                skill.getStudentProfile();

        response.setStudentProfileId(
                profile.getId()
        );

        response.setStudentName(
                profile.getFullName()
        );

        response.setUsername(
                profile
                        .getUser()
                        .getUsername()
        );

        return response;
    }
}