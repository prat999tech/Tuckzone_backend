package com.school.canteen.mapper;

import com.school.canteen.dto.UserSummary;
import com.school.canteen.entity.StudentProfile;
import com.school.canteen.entity.TeacherProfile;
import com.school.canteen.entity.User;
import com.school.canteen.repository.StudentProfileRepository;
import com.school.canteen.repository.TeacherProfileRepository;
import org.springframework.stereotype.Component;

/**
 * Manual entity -> DTO mapping. Explicit (no annotation-processor magic), so you can see
 * exactly which fields cross the boundary — and, crucially, that the password hash never
 * does.
 */
@Component
public class UserMapper {

    private final StudentProfileRepository studentProfileRepository;
    private final TeacherProfileRepository teacherProfileRepository;

    public UserMapper(StudentProfileRepository studentProfileRepository,
                      TeacherProfileRepository teacherProfileRepository) {
        this.studentProfileRepository = studentProfileRepository;
        this.teacherProfileRepository = teacherProfileRepository;
    }

    public UserSummary toSummary(User user) {
        StudentProfile studentProfile = studentProfileRepository.findByUser_Id(user.getId()).orElse(null);
        TeacherProfile teacherProfile = teacherProfileRepository.findByUser_Id(user.getId()).orElse(null);

        return new UserSummary(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getMobile(),
                user.getRole(),
                user.getStatus(),
                user.getCreatedAt(),
                studentProfile != null ? studentProfile.getAdmissionNumber() : null,
                studentProfile != null ? studentProfile.getStudentClass() : null,
                studentProfile != null ? studentProfile.getSection() : null,
                studentProfile != null ? studentProfile.getRollNumber() : null,
                teacherProfile != null ? teacherProfile.getEmployeeId() : null,
                teacherProfile != null ? teacherProfile.getDepartment() : null);
    }
}
