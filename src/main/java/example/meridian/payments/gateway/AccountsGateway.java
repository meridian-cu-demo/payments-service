package example.meridian.payments.gateway;

import java.util.List;
import java.util.Optional;

/** Read-side view of accounts-service, the system of record for balances. */
public interface AccountsGateway {

  Optional<AccountSnapshot> account(String accountId);

  record AccountSnapshot(String accountId, long balanceCents, List<String> holderMemberIds) {}
}
