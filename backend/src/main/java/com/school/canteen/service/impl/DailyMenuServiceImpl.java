package com.school.canteen.service.impl;

import com.school.canteen.dto.menu.DailyMenuItemRequest;
import com.school.canteen.dto.menu.DailyMenuItemResponse;
import com.school.canteen.dto.menu.DailyMenuUpdateRequest;
import com.school.canteen.entity.DailyMenuItem;
import com.school.canteen.entity.MenuItem;
import com.school.canteen.enums.FoodType;
import com.school.canteen.enums.MenuCategory;
import com.school.canteen.exception.BadRequestException;
import com.school.canteen.exception.DuplicateResourceException;
import com.school.canteen.exception.ResourceNotFoundException;
import com.school.canteen.mapper.DailyMenuMapper;
import com.school.canteen.repository.DailyMenuItemRepository;
import com.school.canteen.repository.MenuItemRepository;
import com.school.canteen.service.DailyMenuService;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DailyMenuServiceImpl implements DailyMenuService {

    private final DailyMenuItemRepository dailyMenuItemRepository;
    private final MenuItemRepository menuItemRepository;
    private final DailyMenuMapper dailyMenuMapper;

    public DailyMenuServiceImpl(DailyMenuItemRepository dailyMenuItemRepository,
                                MenuItemRepository menuItemRepository,
                                DailyMenuMapper dailyMenuMapper) {
        this.dailyMenuItemRepository = dailyMenuItemRepository;
        this.menuItemRepository = menuItemRepository;
        this.dailyMenuMapper = dailyMenuMapper;
    }

    @Override
    @Transactional
    public DailyMenuItemResponse addItem(DailyMenuItemRequest request) {
        MenuItem menuItem = menuItemRepository.findById(request.menuItemId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Menu item not found: " + request.menuItemId()));
        if (!menuItem.isActive()) {
            throw new BadRequestException("Cannot add a retired (inactive) item to the menu");
        }
        if (dailyMenuItemRepository.existsByMenuDateAndMenuItem_Id(
                request.menuDate(), request.menuItemId())) {
            throw new DuplicateResourceException(
                    "This item is already on the menu for " + request.menuDate());
        }

        DailyMenuItem entry = new DailyMenuItem();
        entry.setMenuDate(request.menuDate());
        entry.setMenuItem(menuItem);
        entry.setTotalQuantity(request.totalQuantity());
        entry.setRemainingQuantity(request.totalQuantity()); // nothing sold yet
        entry.setAvailable(true);
        dailyMenuItemRepository.save(entry);
        return dailyMenuMapper.toResponse(entry);
    }

    @Override
    @Transactional
    public DailyMenuItemResponse update(UUID id, DailyMenuUpdateRequest request) {
        // Locked read (SELECT ... FOR UPDATE): this method recomputes remaining stock from
        // a read, so without the lock an order placed between the read and the write would
        // be erased from the stock count and the canteen would oversell.
        DailyMenuItem entry = dailyMenuItemRepository.lockById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Daily menu entry not found: " + id));

        // Preserve what's already been consumed when the admin changes the day's total.
        int consumed = entry.getTotalQuantity() - entry.getRemainingQuantity();
        if (request.totalQuantity() < consumed) {
            throw new BadRequestException(
                    "Total cannot be below already-ordered quantity (" + consumed + ")");
        }
        entry.setTotalQuantity(request.totalQuantity());
        entry.setRemainingQuantity(request.totalQuantity() - consumed);
        entry.setAvailable(request.available());
        return dailyMenuMapper.toResponse(entry);
    }

    @Override
    @Transactional
    public void remove(UUID id) {
        DailyMenuItem entry = findOrThrow(id);
        // Deleting an entry that live orders depend on breaks stock accounting: a later
        // cancellation would try to restore stock to a row that no longer exists and would
        // silently lose it. Pull the item from sale instead of deleting it.
        if (dailyMenuItemRepository.hasActiveOrders(entry.getMenuDate(), entry.getMenuItem().getId())) {
            throw new BadRequestException(
                    "This item already has orders for " + entry.getMenuDate()
                            + "; mark it unavailable instead of removing it");
        }
        dailyMenuItemRepository.delete(entry);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DailyMenuItemResponse> listForDate(LocalDate date) {
        return dailyMenuItemRepository.findByMenuDate(date).stream()
                .map(dailyMenuMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<DailyMenuItemResponse> getMenu(LocalDate date, FoodType foodType,
                                               MenuCategory category, String query) {
        String normalizedQuery = (query == null || query.isBlank())
                ? null
                : query.trim().toLowerCase(Locale.ROOT);

        return dailyMenuItemRepository
                .findByMenuDateAndAvailableTrueAndMenuItem_ActiveTrue(date).stream()
                .filter(entry -> foodType == null || entry.getMenuItem().getFoodType() == foodType)
                .filter(entry -> category == null || entry.getMenuItem().getCategory() == category)
                .filter(entry -> normalizedQuery == null
                        || entry.getMenuItem().getName().toLowerCase(Locale.ROOT)
                                .contains(normalizedQuery))
                .map(dailyMenuMapper::toResponse)
                .toList();
    }

    private DailyMenuItem findOrThrow(UUID id) {
        return dailyMenuItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Daily menu entry not found: " + id));
    }
}
