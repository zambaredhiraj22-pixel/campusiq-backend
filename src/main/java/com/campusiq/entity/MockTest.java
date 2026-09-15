package com.campusiq.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "mock_tests")
@Getter
@Setter
@NoArgsConstructor
public class MockTest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    private int aptitudeQuestionCount;

    private int reasoningQuestionCount;

    private int technicalQuestionCount;

    private int durationMinutes;

    private int passPercentage = 45;

    private boolean active;
}