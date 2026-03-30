package example.meridian.payments.model;

public record BillPayment(
    String billPaymentId,
    String memberId,
    String fromAccountId,
    String payeeId,
    long amountCents,
    java.time.Instant requestedAt,
    Transfer.TransferStatus status) {}
