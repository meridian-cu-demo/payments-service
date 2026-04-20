package example.meridian.payments.store;

import example.meridian.payments.model.BillPayment;
import example.meridian.payments.model.Transfer;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.stereotype.Component;

/** In-memory only — this service is a mock (README). No persistence, ever. */
@Component
public class InMemoryTransferStore {

  private final List<Transfer> transfers = new CopyOnWriteArrayList<>();
  private final List<BillPayment> billPayments = new CopyOnWriteArrayList<>();

  public Transfer save(Transfer transfer) {
    transfers.add(transfer);
    return transfer;
  }

  public BillPayment save(BillPayment billPayment) {
    billPayments.add(billPayment);
    return billPayment;
  }

  public Optional<Transfer> byId(String transferId) {
    return transfers.stream().filter(t -> t.transferId().equals(transferId)).findFirst();
  }

  /** Sum of the member's POSTED external transfers today (UTC) — feeds the §5.4 limit check. */
  public long postedTodayCents(String memberId, Instant now) {
    LocalDate today = now.atZone(ZoneOffset.UTC).toLocalDate();
    return transfers.stream()
        .filter(t -> t.memberId().equals(memberId))
        .filter(t -> t.status() == Transfer.TransferStatus.POSTED)
        .filter(t -> t.requestedAt().atZone(ZoneOffset.UTC).toLocalDate().equals(today))
        .mapToLong(Transfer::amountCents)
        .sum();
  }
}
