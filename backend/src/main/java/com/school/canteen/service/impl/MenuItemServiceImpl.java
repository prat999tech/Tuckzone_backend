package com.school.canteen.service.impl;

import com.school.canteen.dto.menu.MenuItemRequest;
import com.school.canteen.dto.menu.MenuItemResponse;
import com.school.canteen.entity.MenuItem;
import com.school.canteen.enums.MenuType;
import com.school.canteen.exception.BadRequestException;
import com.school.canteen.exception.ResourceNotFoundException;
import com.school.canteen.mapper.MenuItemMapper;
import com.school.canteen.repository.DailyMenuItemRepository;
import com.school.canteen.repository.MenuItemRepository;
import com.school.canteen.service.MenuItemService;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class MenuItemServiceImpl implements MenuItemService {

    /** JPG/JPEG, PNG and WEBP only — matches what every mainstream phone camera and the web
     *  file picker actually produce, without opening this up to SVG (XSS risk via inline
     *  scripts) or arbitrary file types. */
    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of("image/jpeg", "image/png", "image/webp");
    private static final long MAX_IMAGE_BYTES = 3L * 1024 * 1024; // 3 MB

    private final MenuItemRepository menuItemRepository;
    private final MenuItemMapper menuItemMapper;
    private final DailyMenuItemRepository dailyMenuItemRepository;

    public MenuItemServiceImpl(MenuItemRepository menuItemRepository, MenuItemMapper menuItemMapper,
                               DailyMenuItemRepository dailyMenuItemRepository) {
        this.menuItemRepository = menuItemRepository;
        this.menuItemMapper = menuItemMapper;
        this.dailyMenuItemRepository = dailyMenuItemRepository;
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
    @Transactional
    public MenuItemResponse activate(UUID id) {
        MenuItem item = findOrThrow(id);
        item.setActive(true);
        return menuItemMapper.toResponse(item);
    }

    @Override
    @Transactional
    public void permanentlyDelete(UUID id) {
        MenuItem item = findOrThrow(id);
        // Scheduling entries are disposable, unlike order history — remove them so nothing
        // references the row before it goes.
        dailyMenuItemRepository.deleteByMenuItem_Id(id);
        // order_items.menu_item_id is nullable with ON DELETE SET NULL (see V14): any past
        // order keeps its own snapshot of the name/price/quantity/etc., so deleting the
        // catalog row here cannot lose or corrupt order history.
        menuItemRepository.delete(item);
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

    @Override
    @Transactional
    public MenuItemResponse uploadImage(UUID id, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Choose an image to upload");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_IMAGE_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new BadRequestException("Please upload a JPG, PNG, or WEBP image.");
        }
        if (file.getSize() > MAX_IMAGE_BYTES) {
            throw new BadRequestException("Image size must be less than 3 MB.");
        }

        MenuItem item = findOrThrow(id);
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException ex) {
            throw new BadRequestException("Could not read the uploaded image");
        }
        // Set together, and only after every validation above has passed — the item never
        // has one without the other, and a rejected upload leaves whatever image it had
        // (uploaded or legacy URL) completely untouched.
        item.setImageData(bytes);
        item.setImageContentType(contentType);
        return menuItemMapper.toResponse(item);
    }

    @Override
    @Transactional
    public MenuItemResponse removeImage(UUID id) {
        MenuItem item = findOrThrow(id);
        item.setImageData(null);
        item.setImageContentType(null);
        item.setImageUrl(null);
        return menuItemMapper.toResponse(item);
    }

    @Override
    @Transactional(readOnly = true)
    public MenuItemImage getImage(UUID id) {
        MenuItem item = findOrThrow(id);
        if (!item.hasUploadedImage()) {
            throw new ResourceNotFoundException("This item has no uploaded image");
        }
        return new MenuItemImage(item.getImageData(), item.getImageContentType());
    }

    private MenuItem findOrThrow(UUID id) {
        return menuItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Menu item not found: " + id));
    }
}
