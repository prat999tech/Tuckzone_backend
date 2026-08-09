package com.school.canteen.service.impl;

import com.school.canteen.dto.ward.WardRequest;
import com.school.canteen.dto.ward.WardResponse;
import com.school.canteen.entity.User;
import com.school.canteen.entity.Ward;
import com.school.canteen.exception.ResourceNotFoundException;
import com.school.canteen.mapper.WardMapper;
import com.school.canteen.repository.UserRepository;
import com.school.canteen.repository.WardRepository;
import com.school.canteen.service.WardService;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WardServiceImpl implements WardService {

    private final WardRepository wardRepository;
    private final UserRepository userRepository;
    private final WardMapper wardMapper;

    public WardServiceImpl(WardRepository wardRepository, UserRepository userRepository, WardMapper wardMapper) {
        this.wardRepository = wardRepository;
        this.userRepository = userRepository;
        this.wardMapper = wardMapper;
    }

    @Override
    @Transactional
    public WardResponse create(UUID parentUserId, WardRequest request) {
        User parent = userRepository.findById(parentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Parent not found"));

        Ward ward = new Ward();
        ward.setParent(parent);
        ward.setName(request.name().trim());
        ward.setStudentClass(request.studentClass().trim());
        ward.setSection(request.section().trim());
        wardRepository.save(ward);
        return wardMapper.toResponse(ward);
    }

    @Override
    @Transactional(readOnly = true)
    public List<WardResponse> list(UUID parentUserId) {
        return wardRepository.findByParent_Id(parentUserId).stream()
                .map(wardMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public WardResponse update(UUID parentUserId, UUID wardId, WardRequest request) {
        Ward ward = wardRepository.findByIdAndParent_Id(wardId, parentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Ward not found"));
        ward.setName(request.name().trim());
        ward.setStudentClass(request.studentClass().trim());
        ward.setSection(request.section().trim());
        return wardMapper.toResponse(ward);
    }

    @Override
    @Transactional
    public void delete(UUID parentUserId, UUID wardId) {
        Ward ward = wardRepository.findByIdAndParent_Id(wardId, parentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Ward not found"));
        wardRepository.delete(ward);
    }
}
