package com.school.canteen.service;

import com.school.canteen.entity.User;
import com.school.canteen.enums.NotificationEvent;
import java.util.Map;

/** Records notifications owed to users. Delivery is handled separately. */
public interface NotificationService {

    /**
     * Queues a notification for one user.
     *
     * Must be called from inside the transaction that performs the business change: the
     * outbox row then commits atomically with it, so a notification can never describe an
     * order that rolled back, nor be lost because the order committed and the send threw.
     */
    void notifyUser(User user, NotificationEvent event, String title, String body,
                    Map<String, String> data);

    /**
     * Queues the same announcement for every active customer (students, teachers and
     * parents), used when the canteen opens or closes ordering.
     *
     * @return how many users were notified
     */
    int notifyAllCustomers(NotificationEvent event, String title, String body,
                           Map<String, String> data);
}
