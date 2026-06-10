package ng.fixpay.core.transfer.dto;

public record NameEnquiryResponse(
        String accountNumber,
        String accountName,
        String bankCode,
        String bankName,
        String sessionId
) {}
