package com.campusiq.service;

import com.campusiq.dto.PlacementReadinessResponse;

public interface PlacementReadinessService {

    PlacementReadinessResponse getPlacementReadiness(Long studentProfileId);
}