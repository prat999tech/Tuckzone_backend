package com.school.canteen.service;

import com.school.canteen.dto.parent.ChildResponse;
import com.school.canteen.dto.parent.LinkChildRequest;
import java.util.List;
import java.util.UUID;

public interface ParentChildService {

    ChildResponse linkChild(UUID parentUserId, LinkChildRequest request);

    List<ChildResponse> listChildren(UUID parentUserId);

    void unlink(UUID parentUserId, UUID linkId);
}
