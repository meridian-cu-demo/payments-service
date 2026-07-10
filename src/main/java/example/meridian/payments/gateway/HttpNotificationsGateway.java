package example.meridian.payments.gateway;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/** Calls notifications-service. Contract: api-contracts/openapi/notifications.yaml. */
@Component
public class HttpNotificationsGateway implements NotificationsGateway {

  private static final Logger log = LoggerFactory.getLogger(HttpNotificationsGateway.class);

  private final RestClient client;

  public HttpNotificationsGateway(@Value("${notifications.base-url}") String baseUrl) {
    this.client = RestClient.create(baseUrl);
  }

  @Override
  public void transferPosted(String memberId, String transferId) {
    try {
      client
          .post()
          .uri("/notifications")
          .body(
              Map.of(
                  "templateKey", "transfer-posted",
                  "memberId", memberId,
                  "channel", "EMAIL",
                  "variables", Map.of("transferId", transferId)))
          .retrieve()
          .toBodilessEntity();
    } catch (Exception e) {
      // A failed confirmation must never fail the transfer itself — log and move on.
      log.warn("event=notificationFailed transferId={} reason={}", transferId, e.getMessage());
    }
  }
}
