package com.school.canteen.controller;

import com.school.canteen.dto.order.DemandRow;
import com.school.canteen.dto.order.DeliverySlotResponse;
import com.school.canteen.dto.order.OrderingWindowRequest;
import com.school.canteen.dto.order.OrderingWindowResponse;
import com.school.canteen.dto.order.UpdateCutoffTimeRequest;
import com.school.canteen.service.OrderingWindowService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Advance-ordering control.
 *
 * Customers may read whether a future date is accepting orders; only the canteen can
 * change it or see aggregated demand.
 */
@RestController
@RequestMapping("/api")
public class OrderingWindowController {

    private final OrderingWindowService orderingWindowService;

    public OrderingWindowController(OrderingWindowService orderingWindowService) {
        this.orderingWindowService = orderingWindowService;
    }

    /** Lets the app show "closed" before a customer builds a cart they cannot submit. */
    @GetMapping("/ordering-status")
    public List<OrderingWindowResponse> status(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return orderingWindowService.statusFor(date);
    }

    @PostMapping("/admin/ordering/close")
    @PreAuthorize("hasRole('CANTEEN_ADMIN')")
    public OrderingWindowResponse close(@Valid @RequestBody OrderingWindowRequest request) {
        return orderingWindowService.close(request);
    }

    @PostMapping("/admin/ordering/open")
    @PreAuthorize("hasRole('CANTEEN_ADMIN')")
    public OrderingWindowResponse open(@Valid @RequestBody OrderingWindowRequest request) {
        return orderingWindowService.open(request);
    }

    /** What has been ordered for a future date, versus what is stocked. */
    @GetMapping("/admin/demand")
    @PreAuthorize("hasRole('CANTEEN_ADMIN')")
    public List<DemandRow> demand(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return orderingWindowService.demandFor(date);
    }

    /** Changes the slot's standing daily cutoff time — the default every date falls back to
     *  unless that date has its own {@link #open} override. Takes effect immediately. */
    @PutMapping("/admin/delivery-slots/{id}/cutoff-time")
    @PreAuthorize("hasRole('CANTEEN_ADMIN')")
    public DeliverySlotResponse updateCutoffTime(@PathVariable UUID id,
                                                 @Valid @RequestBody UpdateCutoffTimeRequest request) {
        return orderingWindowService.updateCutoffTime(id, request.orderCutoffTime());
    }
}
