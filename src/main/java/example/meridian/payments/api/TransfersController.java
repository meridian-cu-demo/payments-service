package example.meridian.payments.api;

import example.meridian.commons.error.ApiException;
import example.meridian.payments.gateway.AccountsGateway;
import example.meridian.payments.gateway.NotificationsGateway;
import example.meridian.payments.limits.TransferLimits;
import example.meridian.payments.model.BillPayment;
import example.meridian.payments.model.Transfer;
import example.meridian.payments.model.Transfer.TransferStatus;
import example.meridian.payments.store.InMemoryTransferStore;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Contract of record: api-contracts/openapi/payments.yaml (§2.4).
 * TODO: contract v1.4.0 requires Idempotency-Key (handbook §4.4) — not yet implemented.
 */
@RestController
public class TransfersController {

  private final InMemoryTransferStore store;
  private final AccountsGateway accounts;
  private final NotificationsGateway notifications;

  public TransfersController(InMemoryTransferStore store, AccountsGateway accounts, NotificationsGateway notifications) {
    this.store = store;
    this.accounts = accounts;
    this.notifications = notifications;
  }

  record MoneyBody(String amount, String currency) {
    long cents() {
      return new BigDecimal(amount).movePointRight(2).longValueExact();
    }

    static MoneyBody fromCents(long cents) {
      return new MoneyBody(BigDecimal.valueOf(cents).movePointLeft(2).toPlainString(), "CAD");
    }
  }

  record CreateTransferBody(String fromAccountId, String toContactId, MoneyBody amount) {}

  record CreateBillPaymentBody(String fromAccountId, String payeeId, MoneyBody amount) {}

  @PostMapping("/transfers")
  @ResponseStatus(HttpStatus.CREATED)
  public Map<String, Object> createTransfer(
      @RequestHeader("X-Member-Id") String memberId, @RequestBody CreateTransferBody body) {
    long amountCents = body.amount().cents();
    Instant now = Instant.now();

    var account =
        accounts
            .account(body.fromAccountId())
            .orElseThrow(() -> new ApiException("ACCOUNT_NOT_FOUND", 404, "No account with that id."));
    if (account.balanceCents() < amountCents) {
      throw new ApiException("INSUFFICIENT_FUNDS", 422, "The account balance does not cover this transfer.");
    }

    long usedToday = store.postedTodayCents(memberId, now);
    if (usedToday + amountCents > TransferLimits.DAILY_EXTERNAL_LIMIT_CENTS) {
      throw new ApiException("DAILY_LIMIT_EXCEEDED", 422, "Daily external transfer limit exceeded (handbook §5.4).");
    }

    Transfer transfer =
        store.save(
            new Transfer(
                "trf-" + UUID.randomUUID(),
                memberId,
                body.fromAccountId(),
                body.toContactId(),
                amountCents,
                now,
                TransferStatus.POSTED));
    notifications.transferPosted(memberId, transfer.transferId());
    return transferBody(transfer, usedToday + amountCents);
  }

  @GetMapping("/transfers/{transferId}")
  public Map<String, Object> getTransfer(@PathVariable String transferId) {
    Transfer transfer =
        store.byId(transferId).orElseThrow(() -> new ApiException("TRANSFER_NOT_FOUND", 404, "No transfer with that id."));
    return transferBody(transfer, store.postedTodayCents(transfer.memberId(), Instant.now()));
  }

  @PostMapping("/bill-payments")
  @ResponseStatus(HttpStatus.CREATED)
  public Map<String, Object> createBillPayment(
      @RequestHeader("X-Member-Id") String memberId, @RequestBody CreateBillPaymentBody body) {
    long amountCents = body.amount().cents();
    accounts
        .account(body.fromAccountId())
        .orElseThrow(() -> new ApiException("ACCOUNT_NOT_FOUND", 404, "No account with that id."));

    BillPayment payment =
        store.save(
            new BillPayment(
                "bp-" + UUID.randomUUID(),
                memberId,
                body.fromAccountId(),
                body.payeeId(),
                amountCents,
                Instant.now(),
                TransferStatus.POSTED));
    return Map.of(
        "billPaymentId", payment.billPaymentId(),
        "fromAccountId", payment.fromAccountId(),
        "payeeId", payment.payeeId(),
        "amount", MoneyBody.fromCents(payment.amountCents()),
        "status", payment.status().name());
  }

  private static Map<String, Object> transferBody(Transfer transfer, long usedTodayCents) {
    long remaining = Math.max(0, TransferLimits.DAILY_EXTERNAL_LIMIT_CENTS - usedTodayCents);
    return Map.of(
        "transferId", transfer.transferId(),
        "fromAccountId", transfer.fromAccountId(),
        "toContactId", transfer.toContactId(),
        "amount", MoneyBody.fromCents(transfer.amountCents()),
        "status", transfer.status().name(),
        "dailyLimitRemaining", MoneyBody.fromCents(remaining));
  }
}
