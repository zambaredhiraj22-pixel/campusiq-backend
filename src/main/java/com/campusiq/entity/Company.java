package com.campusiq.entity;

import java.time.LocalDate;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "companies")
@Getter
@Setter
@NoArgsConstructor
public class Company {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String companyName;

    private String jobRole;

    private Double packageLpa;

    private String location;

    private LocalDate driveDate;

    private boolean active;

    public Company(String companyName,
                   String jobRole,
                   Double packageLpa,
                   String location,
                   LocalDate driveDate,
                   boolean active) {

        this.companyName = companyName;
        this.jobRole = jobRole;
        this.packageLpa = packageLpa;
        this.location = location;
        this.driveDate = driveDate;
        this.active = active;
    }
}