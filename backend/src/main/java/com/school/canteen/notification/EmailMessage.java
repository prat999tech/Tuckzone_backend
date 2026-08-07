package com.school.canteen.notification;

/**
 * A single email to one recipient.
 *
 * @param textBody plain-text alternative for clients that cannot (or choose not to)
 *                 render HTML. Every sender must offer it: some corporate mail filters and
 *                 accessibility tools show only this part.
 */
public record EmailMessage(String to, String subject, String htmlBody, String textBody) {

    /** Derives a plain-text fallback from the HTML when the caller has not composed one. */
    public EmailMessage(String to, String subject, String htmlBody) {
        this(to, subject, htmlBody, stripTags(htmlBody));
    }

    private static String stripTags(String html) {
        if (html == null || html.isBlank()) {
            return "";
        }
        return html.replaceAll("(?is)<(script|style)[^>]*>.*?</\\1>", " ")
                .replaceAll("(?s)<[^>]+>", " ")
                .replaceAll("&nbsp;", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }
}
