package com.school.canteen.util;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

/**
 * Builds a safe {@link Pageable} from raw request parameters.
 *
 * List endpoints previously returned every row ever created, so a long-standing account's
 * order history became one unbounded payload — slow over mobile data and a memory risk on
 * the server. Clamping here means a hostile or careless {@code size=100000} cannot be used
 * to pull the whole table.
 */
public final class PageRequests {

    public static final int DEFAULT_SIZE = 50;
    public static final int MAX_SIZE = 200;

    private PageRequests() {
    }

    public static Pageable of(Integer page, Integer size) {
        int safePage = (page == null || page < 0) ? 0 : page;
        int safeSize = (size == null || size < 1) ? DEFAULT_SIZE : Math.min(size, MAX_SIZE);
        return PageRequest.of(safePage, safeSize);
    }
}
