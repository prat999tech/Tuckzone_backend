package com.school.canteen.service.impl;

import com.school.canteen.dto.parent.ChildResponse;
import com.school.canteen.dto.parent.LinkChildRequest;
import com.school.canteen.entity.ParentChildLink;
import com.school.canteen.entity.StudentProfile;
import com.school.canteen.entity.User;
import com.school.canteen.exception.BadRequestException;
import com.school.canteen.exception.DuplicateResourceException;
import com.school.canteen.exception.ResourceNotFoundException;
import com.school.canteen.mapper.ChildMapper;
import com.school.canteen.repository.ParentChildLinkRepository;
import com.school.canteen.repository.StudentProfileRepository;
import com.school.canteen.repository.UserRepository;
import com.school.canteen.service.ParentChildService;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ParentChildServiceImpl implements ParentChildService {

    private final ParentChildLinkRepository linkRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final UserRepository userRepository;
    private final ChildMapper childMapper;

    public ParentChildServiceImpl(ParentChildLinkRepository linkRepository,
                                  StudentProfileRepository studentProfileRepository,
                                  UserRepository userRepository,
                                  ChildMapper childMapper) {
        this.linkRepository = linkRepository;
        this.studentProfileRepository = studentProfileRepository;
        this.userRepository = userRepository;
        this.childMapper = childMapper;
    }

    @Override
    @Transactional
    public ChildResponse linkChild(UUID parentUserId, LinkChildRequest request) {
        StudentProfile profile = studentProfileRepository
                .findByAdmissionNumber(request.admissionNumber())
                // Called on request.parentMobile() (always non-blank, enforced by
                // LinkChildRequest) rather than the student's stored value, which can now be
                // null for a student who registered without one — that must fail the match,
                // not throw.
                .filter(p -> request.parentMobile().equals(p.getParentMobile()))
                // Same generic message whether the admission number is wrong or the mobile
                // doesn't match, so an attacker can't probe which admission numbers exist.
                .orElseThrow(() -> new BadRequestException(
                        "No student matches that admission number and parent mobile"));

        if (linkRepository.existsByParent_IdAndStudentProfile_Id(parentUserId, profile.getId())) {
            throw new DuplicateResourceException("This child is already linked to your account");
        }

        User parent = userRepository.findById(parentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Parent not found"));

        ParentChildLink link = new ParentChildLink();
        link.setParent(parent);
        link.setStudentProfile(profile);
        linkRepository.save(link);
        return childMapper.toChildResponse(link);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChildResponse> listChildren(UUID parentUserId) {
        return linkRepository.findByParent_Id(parentUserId).stream()
                .map(childMapper::toChildResponse)
                .toList();
    }

    @Override
    @Transactional
    public void unlink(UUID parentUserId, UUID linkId) {
        ParentChildLink link = linkRepository.findByIdAndParent_Id(linkId, parentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Linked child not found"));
        linkRepository.delete(link);
    }
}
