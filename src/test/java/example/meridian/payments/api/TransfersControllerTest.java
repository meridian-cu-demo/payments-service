package example.meridian.payments.api;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import example.meridian.payments.gateway.AccountsGateway;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class TransfersControllerTest {

  @Autowired private MockMvc mvc;
  @MockBean private AccountsGateway accounts;

  private void accountWithBalance(long cents) {
    when(accounts.account(anyString()))
        .thenReturn(Optional.of(new AccountsGateway.AccountSnapshot("acc-2001", cents, List.of("mem-1001"))));
  }

  @Test
  void happyTransferPostsAndReportsRemainingLimit() throws Exception {
    accountWithBalance(1_000_000);
    mvc.perform(
            post("/transfers")
                .header("X-Member-Id", "mem-1001")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"fromAccountId":"acc-2001","toContactId":"ct-77","amount":{"amount":"250.00","currency":"CAD"}}
                    """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.status").value("POSTED"))
        .andExpect(jsonPath("$.dailyLimitRemaining.amount").value("2750.00"));
  }

  @Test
  void overDailyLimitRejectsWithLimitCode() throws Exception {
    accountWithBalance(100_000_000);
    mvc.perform(
            post("/transfers")
                .header("X-Member-Id", "mem-limit")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"fromAccountId":"acc-2001","toContactId":"ct-77","amount":{"amount":"3000.01","currency":"CAD"}}
                    """))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.code").value("DAILY_LIMIT_EXCEEDED"));
  }

  @Test
  void insufficientFundsRejects() throws Exception {
    accountWithBalance(1_000);
    mvc.perform(
            post("/transfers")
                .header("X-Member-Id", "mem-1001")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"fromAccountId":"acc-2001","toContactId":"ct-77","amount":{"amount":"250.00","currency":"CAD"}}
                    """))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.code").value("INSUFFICIENT_FUNDS"));
  }

  @Test
  void unknownTransferIsLegacyErrorShape() throws Exception {
    mvc.perform(get("/transfers/trf-nope"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("TRANSFER_NOT_FOUND"));
  }
}
