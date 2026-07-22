package com.school.canteen.enums;

/** Direction of a wallet ledger entry. Amount is always stored positive; this says which
 *  way the balance moved. */
public enum TransactionType {
    CREDIT,
    DEBIT
}
