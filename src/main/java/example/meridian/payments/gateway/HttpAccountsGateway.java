package example.meridian.payments.gateway;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/** Calls accounts-service over HTTP. Contract: api-contracts/openapi/accounts.yaml. */
@Component
public class HttpAccountsGateway implements AccountsGateway {

  private final RestClient client;

  public HttpAccountsGateway(@Value("${accounts.base-url}") String baseUrl) {
    this.client = RestClient.create(baseUrl);
  }

  @Override
  @SuppressWarnings("unchecked")
  public Optional<AccountSnapshot> account(String accountId) {
    try {
      Map<String, Object> body = client.get().uri("/accounts/{id}", accountId).retrieve().body(Map.class);
      if (body == null) return Optional.empty();
      Map<String, Object> balance = (Map<String, Object>) body.get("balance");
      long cents = new BigDecimal((String) balance.get("amount")).movePointRight(2).longValueExact();
      List<String> holders = (List<String>) body.getOrDefault("holders", List.of());
      return Optional.of(new AccountSnapshot((String) body.get("accountId"), cents, holders));
    } catch (Exception e) {
      return Optional.empty();
    }
  }
}
