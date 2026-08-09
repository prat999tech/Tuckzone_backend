package com.school.canteen;

import com.school.canteen.dto.auth.ParentRegisterRequest;
import com.school.canteen.dto.auth.StudentRegisterRequest;
import com.school.canteen.dto.auth.TeacherRegisterRequest;
import com.school.canteen.dto.menu.DailyMenuItemRequest;
import com.school.canteen.dto.menu.MenuItemRequest;
import com.school.canteen.enums.MenuType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Builds valid request objects for tests.
 *
 * Every identity field is made unique per call, because the schema enforces uniqueness on
 * email, mobile, admission number and employee id — tests would otherwise collide with
 * each other inside the shared database container.
 */
public final class TestDataFactory {

    private static final AtomicInteger COUNTER = new AtomicInteger(1000);

    private TestDataFactory() {
    }

    private static int next() {
        return COUNTER.incrementAndGet();
    }

    /** Valid Indian-format mobile: 10 digits starting 6-9. */
    public static String uniqueMobile() {
        return "9" + String.format("%09d", next());
    }

    public static StudentRegisterRequest student() {
        int n = next();
        return new StudentRegisterRequest(
                "Student " + n,
                "student" + n + "@test.local",
                uniqueMobile(),
                "Password@123",
                "ADM-" + n,
                "VIII",
                "B",
                String.valueOf(n % 60),
                "S-" + n,
                null);
    }

    public static TeacherRegisterRequest teacher() {
        int n = next();
        return new TeacherRegisterRequest(
                "Teacher " + n,
                "teacher" + n + "@test.local",
                uniqueMobile(),
                "Password@123",
                "EMP-" + n,
                "Science");
    }

    public static ParentRegisterRequest parent() {
        int n = next();
        return new ParentRegisterRequest(
                "Parent " + n,
                "parent" + n + "@test.local",
                uniqueMobile(),
                "Password@123");
    }

    public static MenuItemRequest menuItem(BigDecimal price, BigDecimal costPrice) {
        int n = next();
        return new MenuItemRequest(
                "Item " + n,
                "Test item",
                price,
                costPrice,
                MenuType.DAILY,
                true,
                null,
                null);
    }

    public static DailyMenuItemRequest dailyMenu(LocalDate date, UUID menuItemId, int quantity) {
        return new DailyMenuItemRequest(date, menuItemId, quantity);
    }
}
