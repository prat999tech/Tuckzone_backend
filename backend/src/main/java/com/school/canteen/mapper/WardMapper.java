package com.school.canteen.mapper;

import com.school.canteen.dto.ward.WardResponse;
import com.school.canteen.entity.Ward;
import org.springframework.stereotype.Component;

@Component
public class WardMapper {

    public WardResponse toResponse(Ward ward) {
        return new WardResponse(
                ward.getId(),
                ward.getName(),
                ward.getStudentClass(),
                ward.getSection());
    }
}
