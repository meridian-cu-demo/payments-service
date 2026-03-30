package example.meridian.payments.limits;

/**
 * Org-wide daily cap on member-initiated external transfers (handbook §5.4).
 * NOTE: not member-configurable — every member gets the same cap for now.
 */
public final class TransferLimits {

  public static final long DAILY_EXTERNAL_LIMIT_CENTS = 300_000; // $3,000.00 CAD

  private TransferLimits() {}
}
