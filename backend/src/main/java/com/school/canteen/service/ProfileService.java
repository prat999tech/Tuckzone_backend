package com.school.canteen.service;

import com.school.canteen.dto.UserSummary;
import com.school.canteen.dto.profile.ProfileUpdateRequest;
import java.util.UUID;

/** A user viewing and editing their own profile. */
public interface ProfileService {

    UserSummary getProfile(UUID userId);

    UserSummary updateProfile(UUID userId, ProfileUpdateRequest request);
}
