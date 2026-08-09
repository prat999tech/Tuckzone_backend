package com.school.canteen.controller;

import com.school.canteen.service.MenuItemService;
import java.util.UUID;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Serves an uploaded food photo's raw bytes. Deliberately public (see SecurityConfig) —
 * this app authenticates with a bearer token, not a cookie, so a plain {@code <img src>}
 * tag can never present it; a food photo isn't sensitive enough to justify going through a
 * signed-URL scheme instead. Split from {@link MenuItemAdminController} because that
 * controller's whole surface requires the CANTEEN_ADMIN/SUB_ADMIN role, and this endpoint
 * must not.
 */
@RestController
@RequestMapping("/api/menu-items")
public class MenuItemImageController {

    private final MenuItemService menuItemService;

    public MenuItemImageController(MenuItemService menuItemService) {
        this.menuItemService = menuItemService;
    }

    @GetMapping("/{id}/image")
    public ResponseEntity<byte[]> image(@PathVariable UUID id) {
        MenuItemService.MenuItemImage image = menuItemService.getImage(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(image.contentType()))
                // Short enough that replacing a photo (uncommon) doesn't leave stale copies
                // visible for long, long enough to meaningfully save repeat requests for the
                // same item within one browsing session.
                .cacheControl(CacheControl.maxAge(java.time.Duration.ofMinutes(10)).cachePublic())
                .body(image.data());
    }
}
