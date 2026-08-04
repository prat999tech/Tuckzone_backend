package com.school.canteen.service;

import com.school.canteen.dto.UserSummary;
import com.school.canteen.dto.subadmin.SubAdminCreateRequest;
import com.school.canteen.dto.subadmin.SubAdminUpdateRequest;
import java.util.List;
import java.util.UUID;

/**
 * Super-Admin-only management of Sub Admin accounts. Every method here only ever reads or
 * writes rows whose role is SUB_ADMIN — it can never see or touch a CANTEEN_ADMIN account,
 * even if a caller supplies that account's id.
 */
public interface SubAdminService {

    UserSummary create(SubAdminCreateRequest request);

    List<UserSummary> list(Integer page, Integer size);

    UserSummary get(UUID id);

    UserSummary update(UUID id, SubAdminUpdateRequest request);

    UserSummary deactivate(UUID id);

    UserSummary activate(UUID id);

    void delete(UUID id);
}
