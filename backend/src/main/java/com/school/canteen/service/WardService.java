package com.school.canteen.service;

import com.school.canteen.dto.ward.WardRequest;
import com.school.canteen.dto.ward.WardResponse;
import java.util.List;
import java.util.UUID;

public interface WardService {

    WardResponse create(UUID parentUserId, WardRequest request);

    List<WardResponse> list(UUID parentUserId);

    WardResponse update(UUID parentUserId, UUID wardId, WardRequest request);

    void delete(UUID parentUserId, UUID wardId);
}
