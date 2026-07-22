package com.school.canteen.service;

import com.school.canteen.dto.UserSummary;
import com.school.canteen.dto.auth.AuthResponse;
import com.school.canteen.dto.auth.LoginRequest;
import com.school.canteen.dto.auth.ParentRegisterRequest;
import com.school.canteen.dto.auth.RefreshRequest;
import com.school.canteen.dto.auth.StudentRegisterRequest;
import com.school.canteen.dto.auth.TeacherRegisterRequest;

/**
 * Registration + authentication use cases. Controllers depend on this interface, not the
 * implementation (Dependency Inversion) — the wiring is swappable and easily mocked.
 */
public interface AuthService {

    UserSummary registerStudent(StudentRegisterRequest request);

    UserSummary registerTeacher(TeacherRegisterRequest request);

    UserSummary registerParent(ParentRegisterRequest request);

    AuthResponse login(LoginRequest request);

    AuthResponse refresh(RefreshRequest request);
}
