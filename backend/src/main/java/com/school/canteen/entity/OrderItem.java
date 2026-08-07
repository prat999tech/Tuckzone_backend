package com.school.canteen.entity;

import com.school.canteen.enums.MenuType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;

/**
 * A single line in an order. item_name/unit_price/costPrice/menuType are snapshots taken
 * at order time, so historical orders are completely unaffected by later catalog edits —
 * including the catalog row being permanently deleted. menu_item is kept only as a live
 * pointer for as long as the catalog row exists (nullable, {@code ON DELETE SET NULL}); no
 * historical display or report may depend on it being non-null.
 */
@Entity
@Table(name = "order_items")
public class OrderItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "menu_item_id")
    private MenuItem menuItem;

    @Column(name = "item_name", nullable = false)
    private String itemName;

    @Column(name = "unit_price", nullable = false, precision = 8, scale = 2)
    private BigDecimal unitPrice;

    /** Snapshot of {@link MenuItem#getCostPrice()} at order time — used by the cost-of-goods
     *  report so it stays accurate after the item is deleted. */
    @Column(name = "cost_price", precision = 8, scale = 2)
    private BigDecimal costPrice;

    /** Snapshot of {@link MenuItem#getMenuType()} at order time — used by the top-items
     *  report so it stays accurate after the item is deleted. */
    @Enumerated(EnumType.STRING)
    @Column(name = "menu_type")
    private MenuType menuType;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Column(name = "line_total", nullable = false, precision = 12, scale = 2)
    private BigDecimal lineTotal;

    public Order getOrder() {
        return order;
    }

    public void setOrder(Order order) {
        this.order = order;
    }

    public MenuItem getMenuItem() {
        return menuItem;
    }

    public void setMenuItem(MenuItem menuItem) {
        this.menuItem = menuItem;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
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

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getLineTotal() {
        return lineTotal;
    }

    public void setLineTotal(BigDecimal lineTotal) {
        this.lineTotal = lineTotal;
    }
}
