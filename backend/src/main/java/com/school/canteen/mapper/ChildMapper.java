package com.school.canteen.mapper;

import com.school.canteen.dto.parent.ChildResponse;
import com.school.canteen.entity.ParentChildLink;
import com.school.canteen.entity.StudentProfile;
import org.springframework.stereotype.Component;

@Component
public class ChildMapper {

    public ChildResponse toChildResponse(ParentChildLink link) {
        StudentProfile profile = link.getStudentProfile();
        return new ChildResponse(
                link.getId(),
                profile.getId(),
                profile.getUser().getId(),
                profile.getUser().getFullName(),
                profile.getAdmissionNumber(),
                profile.getStudentClass(),
                profile.getSection(),
                profile.getRollNumber());
    }
}
