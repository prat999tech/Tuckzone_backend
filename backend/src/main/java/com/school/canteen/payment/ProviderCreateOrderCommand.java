package com.school.canteen.payment;

import java.util.Map;

/** Everything a provider needs to open an order. Amount is in the smallest currency unit
 *  (paise for INR), matching every gateway's own convention. */
public record ProviderCreateOrderCommand(long amountPaise, String currency, String receipt,
                                         Map<String, String> notes) {
}
