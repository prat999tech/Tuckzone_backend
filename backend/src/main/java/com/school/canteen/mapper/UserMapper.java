package com.school.canteen.mapper;

import com.school.canteen.dto.UserSummary;
import com.school.canteen.entity.StudentProfile;
import com.school.canteen.entity.TeacherProfile;
import com.school.canteen.entity.User;
import com.school.canteen.repository.StudentProfileRepository;
import com.school.canteen.repository.TeacherProfileRepository;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
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

    /** Single-user mapping; issues at most two profile lookups. */
    public UserSummary toSummary(User user) {
        return toSummary(user,
                studentProfileRepository.findByUser_Id(user.getId()).orElse(null),
                teacherProfileRepository.findByUser_Id(user.getId()).orElse(null));
    }

    /**
     * Batch mapping for lists.
     *
     * Mapping a list one-by-one issued two extra queries per user, so listing 1000 users
     * cost ~2001 round trips. This resolves every profile in two queries regardless of
     * how many users are being mapped.
     */
    public List<UserSummary> toSummaries(List<User> users) {
        if (users.isEmpty()) {
            return List.of();
        }
        Set<UUID> userIds = users.stream().map(User::getId).collect(Collectors.toSet());

        Map<UUID, StudentProfile> studentsByUserId = studentProfileRepository.findByUser_IdIn(userIds)
                .stream()
                .collect(Collectors.toMap(profile -> profile.getUser().getId(), Function.identity()));
        Map<UUID, TeacherProfile> teachersByUserId = teacherProfileRepository.findByUser_IdIn(userIds)
                .stream()
                .collect(Collectors.toMap(profile -> profile.getUser().getId(), Function.identity()));

        return users.stream()
                .map(user -> toSummary(user,
                        studentsByUserId.get(user.getId()),
                        teachersByUserId.get(user.getId())))
                .toList();
    }

    private UserSummary toSummary(User user, StudentProfile studentProfile,
                                  TeacherProfile teacherProfile) {
        return new UserSummary(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getMobile(),
                user.getRole(),
                user.getStatus(),
                user.isEmailVerified(),
                user.getCreatedAt(),
                studentProfile != null ? studentProfile.getAdmissionNumber() : null,
                studentProfile != null ? studentProfile.getStudentClass() : null,
                studentProfile != null ? studentProfile.getSection() : null,
                studentProfile != null ? studentProfile.getRollNumber() : null,
                studentProfile != null ? studentProfile.getSeatNumber() : null,
                studentProfile != null ? studentProfile.getParentMobile() : null,
                teacherProfile != null ? teacherProfile.getEmployeeId() : null,
                teacherProfile != null ? teacherProfile.getDepartment() : null);
    }
}
