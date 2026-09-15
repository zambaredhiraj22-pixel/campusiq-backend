package com.campusiq.entity;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "company_eligibility_criteria")
@Getter
@Setter
@NoArgsConstructor
public class CompanyEligibilityCriteria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Double minimum10thPercentage;

    private Double minimum12thPercentage;

    private Double minimumDiplomaPercentage;

    private Double minimumCgpa;

    private Double minimumAttendancePercentage;

    private Double minimumMockTestScore;

    @ElementCollection
    @CollectionTable(
        name = "company_allowed_departments",
        joinColumns = @JoinColumn(name = "criteria_id")
    )
    @Column(name = "department")
    private Set<String> allowedDepartments = new HashSet<>();

    @ElementCollection
    @CollectionTable(
        name = "company_required_skills",
        joinColumns = @JoinColumn(name = "criteria_id")
    )
    @Column(name = "skill_name")
    private Set<String> requiredSkills = new HashSet<>();

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "company_id",
        nullable = false,
        unique = true
    )
    private Company company;
}
