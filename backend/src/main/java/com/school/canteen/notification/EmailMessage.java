package com.school.canteen.notification;

/** A single email to one recipient. */
public record EmailMessage(String to, String subject, String htmlBody) {
}
