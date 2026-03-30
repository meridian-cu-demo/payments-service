package example.meridian.payments.model;

/** An external transfer. Money is cents (long) — never floating point (§3.1). */
public record Transfer(
    String transferId,
    String memberId,
    String fromAccountId,
    String toContactId,
    long amountCents,
    java.time.Instant requestedAt,
    TransferStatus status) {

  public enum TransferStatus {
    PENDING,
    POSTED,
    REJECTED
  }
}
