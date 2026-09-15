package com.campusiq.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "mock_test_questions",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_mock_test_question",
                        columnNames = {
                                "mock_test_id",
                                "question_id"
                        }
                ),
                @UniqueConstraint(
                        name = "uk_mock_test_question_order",
                        columnNames = {
                                "mock_test_id",
                                "question_order"
                        }
                )
        },
        indexes = {
                @Index(
                        name = "idx_mock_test_questions_test",
                        columnList = "mock_test_id"
                ),
                @Index(
                        name = "idx_mock_test_questions_question",
                        columnList = "question_id"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
public class MockTestQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /*
     * Mock test to which this question belongs.
     */
    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "mock_test_id",
            nullable = false
    )
    private MockTest mockTest;

    /*
     * Exact approved question used in this mock test.
     */
    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "question_id",
            nullable = false
    )
    private Question question;

    /*
     * Fixed question position inside the test.
     *
     * Example:
     * 1  - 15  = Aptitude
     * 16 - 30  = Reasoning
     * 31 - 60  = Technical
     */
    @jakarta.persistence.Column(
            name = "question_order",
            nullable = false
    )
    private Integer questionOrder;
}