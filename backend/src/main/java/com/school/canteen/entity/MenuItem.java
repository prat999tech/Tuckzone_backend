package com.school.canteen.entity;

import com.school.canteen.enums.MenuType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;

/**
 * A catalog food item. Prices are BigDecimal (never double) so money arithmetic is exact.
 */
@Entity
@Table(name = "menu_items")
public class MenuItem extends BaseEntity {

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "price", nullable = false, precision = 8, scale = 2)
    private BigDecimal price;

    /** What the item costs the canteen to make. Null means "not recorded yet", which
     *  reports treat as zero — understating cost rather than inventing one. */
    @Column(name = "cost_price", precision = 8, scale = 2)
    private BigDecimal costPrice;

    @Enumerated(EnumType.STRING)
    @Column(name = "menu_type", nullable = false)
    private MenuType menuType;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "allergens")
    private String allergens;

    /** Soft-delete flag. Retired items stay in the DB (past orders reference them). */
    @Column(name = "active", nullable = false)
    private boolean active = true;

    /** Temporary out-of-stock toggle, distinct from {@link #active}: an unavailable item
     *  stays in the catalog and can be re-enabled, unlike a retired (inactive) one. Governs
     *  ordering directly for FIXED items; DAILY items are additionally gated per-date by
     *  {@link com.school.canteen.entity.DailyMenuItem#isAvailable()}. */
    @Column(name = "available", nullable = false)
    private boolean available = true;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public BigDecimal getCostPrice() {
        return costPrice;
    }

    public void setCostPrice(BigDecimal costPrice) {
        this.costPrice = costPrice;
    }

    public MenuType getMenuType() {
        return menuType;
    }

    public void setMenuType(MenuType menuType) {
        this.menuType = menuType;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getAllergens() {
        return allergens;
    }

    public void setAllergens(String allergens) {
        this.allergens = allergens;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }
}
