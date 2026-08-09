package com.school.canteen.menu;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.school.canteen.IntegrationTestBase;
import com.school.canteen.TestDataFactory;
import com.school.canteen.dto.menu.MenuItemRequest;
import com.school.canteen.dto.menu.MenuItemResponse;
import com.school.canteen.exception.BadRequestException;
import com.school.canteen.exception.ResourceNotFoundException;
import com.school.canteen.repository.MenuItemRepository;
import com.school.canteen.service.MenuItemService;
import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;

/**
 * Sections 27-34: uploaded images replace the old "paste an Image URL" field, are stored
 * persistently (this app has no object storage — see docs — so bytes live in Postgres,
 * which already survives every Render redeploy), validated for type/size, and a legacy
 * item's existing external URL is preserved untouched until it gets a real upload.
 */
class MenuItemImageIntegrationTest extends IntegrationTestBase {

    @Autowired private MenuItemService menuItemService;
    @Autowired private MenuItemRepository menuItemRepository;

    private static final byte[] TINY_PNG = {
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 1, 2, 3,
    };

    @Test
    @DisplayName("uploading a valid image stores it and the response imageUrl points at the serving endpoint")
    void uploadStoresImageAndComputesUrl() {
        MenuItemResponse item = menuItemService.create(TestDataFactory.menuItem(BigDecimal.TEN, BigDecimal.ONE));
        MockMultipartFile file = new MockMultipartFile("file", "lunch.png", "image/png", TINY_PNG);

        MenuItemResponse updated = menuItemService.uploadImage(item.id(), file);

        assertThat(updated.imageUrl()).contains("/api/menu-items/" + item.id() + "/image");

        MenuItemService.MenuItemImage stored = menuItemService.getImage(item.id());
        assertThat(stored.contentType()).isEqualTo("image/png");
        assertThat(stored.data()).isEqualTo(TINY_PNG);
    }

    @Test
    @DisplayName("an unsupported file type is rejected with a specific message, not a generic failure")
    void rejectsUnsupportedContentType() {
        MenuItemResponse item = menuItemService.create(TestDataFactory.menuItem(BigDecimal.TEN, BigDecimal.ONE));
        MockMultipartFile file = new MockMultipartFile("file", "menu.pdf", "application/pdf", TINY_PNG);

        assertThatThrownBy(() -> menuItemService.uploadImage(item.id(), file))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("JPG, PNG, or WEBP");
    }

    @Test
    @DisplayName("an oversized image is rejected before it's ever persisted")
    void rejectsOversizedImage() {
        MenuItemResponse item = menuItemService.create(TestDataFactory.menuItem(BigDecimal.TEN, BigDecimal.ONE));
        byte[] tooBig = new byte[3 * 1024 * 1024 + 1];
        MockMultipartFile file = new MockMultipartFile("file", "huge.png", "image/png", tooBig);

        assertThatThrownBy(() -> menuItemService.uploadImage(item.id(), file))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("less than 3 MB");

        assertThatThrownBy(() -> menuItemService.getImage(item.id()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("a rejected upload leaves whatever image the item already had completely untouched")
    void rejectedUploadDoesNotClearExistingImage() {
        MenuItemResponse item = menuItemService.create(TestDataFactory.menuItem(BigDecimal.TEN, BigDecimal.ONE));
        menuItemService.uploadImage(item.id(),
                new MockMultipartFile("file", "lunch.png", "image/png", TINY_PNG));

        MockMultipartFile badFile = new MockMultipartFile("file", "menu.pdf", "application/pdf", TINY_PNG);
        assertThatThrownBy(() -> menuItemService.uploadImage(item.id(), badFile))
                .isInstanceOf(BadRequestException.class);

        // The original upload is still there — a rejected replacement never deleted it first.
        assertThat(menuItemService.getImage(item.id()).data()).isEqualTo(TINY_PNG);
    }

    @Test
    @DisplayName("removing an image reverts to no image at all")
    void removeImageClearsIt() {
        MenuItemResponse item = menuItemService.create(TestDataFactory.menuItem(BigDecimal.TEN, BigDecimal.ONE));
        menuItemService.uploadImage(item.id(),
                new MockMultipartFile("file", "lunch.png", "image/png", TINY_PNG));

        MenuItemResponse afterRemoval = menuItemService.removeImage(item.id());

        assertThat(afterRemoval.imageUrl()).isNull();
        assertThatThrownBy(() -> menuItemService.getImage(item.id()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("a legacy external image URL survives an ordinary field edit untouched")
    void legacyImageUrlSurvivesAnUnrelatedUpdate() {
        MenuItemResponse item = menuItemService.create(TestDataFactory.menuItem(BigDecimal.TEN, BigDecimal.ONE));
        // Simulates data from before this feature existed — no service method sets imageUrl
        // anymore (see MenuItemRequest's javadoc), so a direct repository write stands in
        // for "a row migrated from the old paste-a-URL flow".
        var entity = menuItemRepository.findById(item.id()).orElseThrow();
        entity.setImageUrl("https://images.example.com/lunch.jpg");
        menuItemRepository.save(entity);

        MenuItemRequest editRequest = new MenuItemRequest("Renamed", "Test item", BigDecimal.valueOf(15),
                BigDecimal.ONE, item.menuType(), true, null);
        MenuItemResponse updated = menuItemService.update(item.id(), editRequest);

        assertThat(updated.name()).isEqualTo("Renamed");
        assertThat(updated.imageUrl()).isEqualTo("https://images.example.com/lunch.jpg");
    }
}
