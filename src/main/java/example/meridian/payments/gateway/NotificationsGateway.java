package example.meridian.payments.gateway;

/** Member communication goes through notifications-service — never sent directly (§4.5, ADR-0003). */
public interface NotificationsGateway {

  void transferPosted(String memberId, String transferId);
}
