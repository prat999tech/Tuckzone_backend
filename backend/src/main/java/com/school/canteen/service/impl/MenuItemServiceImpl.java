package com.school.canteen.service.impl;

import com.school.canteen.dto.menu.MenuItemRequest;
import com.school.canteen.dto.menu.MenuItemResponse;
import com.school.canteen.entity.MenuItem;
import com.school.canteen.enums.MenuType;
import com.school.canteen.exception.ResourceNotFoundException;
import com.school.canteen.mapper.MenuItemMapper;
import com.school.canteen.repository.MenuItemRepository;
import com.school.canteen.service.MenuItemService;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MenuItemServiceImpl implements MenuItemService {

    private final MenuItemRepository menuItemRepository;
    private final MenuItemMapper menuItemMapper;

    public MenuItemServiceImpl(MenuItemRepository menuItemRepository, MenuItemMapper menuItemMapper) {
        this.menuItemRepository = menuItemRepository;
        this.menuItemMapper = menuItemMapper;
    }

    @Override
    @Transactional
    public MenuItemResponse create(MenuItemRequest request) {
        MenuItem item = menuItemMapper.toEntity(request);
        menuItemRepository.save(item);
        return menuItemMapper.toResponse(item);
    }

    @Override
    @Transactional
    public MenuItemResponse update(UUID id, MenuItemRequest request) {
        MenuItem item = findOrThrow(id);
        menuItemMapper.applyEditableFields(item, request);
        return menuItemMapper.toResponse(item); // managed entity flushes on commit
    }

    @Override
    @Transactional
    public void deactivate(UUID id) {
        MenuItem item = findOrThrow(id);
        item.setActive(false);
    }

    @Override
    @Transactional(readOnly = true)
    public MenuItemResponse get(UUID id) {
        return menuItemMapper.toResponse(findOrThrow(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<MenuItemResponse> list(boolean includeInactive, MenuType menuType) {
        List<MenuItem> items;
        if (menuType != null) {
            items = includeInactive
                    ? menuItemRepository.findAll().stream()
                            .filter(item -> item.getMenuType() == menuType)
                            .toList()
                    : menuItemRepository.findByMenuTypeAndActiveTrue(menuType);
        } else {
            items = includeInactive ? menuItemRepository.findAll() : menuItemRepository.findByActiveTrue();
        }
        return items.stream().map(menuItemMapper::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MenuItemResponse> listFixedMenu(String query) {
        String normalizedQuery = (query == null || query.isBlank())
                ? null
                : query.trim().toLowerCase(Locale.ROOT);

        return menuItemRepository.findByMenuTypeAndActiveTrueAndAvailableTrue(MenuType.FIXED).stream()
                .filter(item -> normalizedQuery == null
                        || item.getName().toLowerCase(Locale.ROOT).contains(normalizedQuery))
                .map(menuItemMapper::toResponse)
                .toList();
    }

    private MenuItem findOrThrow(UUID id) {
        return menuItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Menu item not found: " + id));
    }
}
