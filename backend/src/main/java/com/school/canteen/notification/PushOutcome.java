package com.school.canteen.notification;

/**
 * Result for one message in a batch.
 *
 * @param tokenInvalid true when the provider says this device registration is dead
 *                     (app uninstalled, token rotated). Those tokens are deleted rather
 *                     than retried, otherwise every future send wastes a slot on a device
 *                     that will never receive anything.
 */
public record PushOutcome(
        boolean success,
        boolean tokenInvalid,
        String error) {

    public static PushOutcome ok() {
        return new PushOutcome(true, false, null);
    }

    public static PushOutcome failed(String error) {
        return new PushOutcome(false, false, error);
    }

    public static PushOutcome invalidToken(String error) {
        return new PushOutcome(false, true, error);
    }
}
