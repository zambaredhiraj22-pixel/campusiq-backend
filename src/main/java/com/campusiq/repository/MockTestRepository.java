package com.campusiq.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.campusiq.entity.MockTest;

public interface MockTestRepository extends JpaRepository<MockTest, Long> {

}