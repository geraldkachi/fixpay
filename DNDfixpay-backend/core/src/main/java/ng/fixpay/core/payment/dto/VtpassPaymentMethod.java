package ng.fixpay.core.payment.dto;

public enum VtpassPaymentMethod {
    WALLET,
    USSD,
    CARD,
    NIBSS_MANDATE,
    /** Direct bank transfer via a virtual account number (e.g. Monnify). */
    BANK_TRANSFER,
    /** OPay in-app checkout. */
    OPAY
}
