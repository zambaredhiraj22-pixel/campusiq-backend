package com.campusiq.service;

import java.util.List;

import com.campusiq.dto.TpoPlacementReadyStudentResponse;

public interface TpoPlacementReadyService {

    List<TpoPlacementReadyStudentResponse>
            getPlacementReadyStudents();
}