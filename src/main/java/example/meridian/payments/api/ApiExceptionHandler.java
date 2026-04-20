package example.meridian.payments.api;

import example.meridian.commons.error.ApiException;
import example.meridian.commons.error.ErrorResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Maps ApiException onto the legacy error shape. §4.6 envelope adoption is tracked org-wide. */
@RestControllerAdvice
public class ApiExceptionHandler {

  @ExceptionHandler(ApiException.class)
  public ResponseEntity<ErrorResponse> handle(ApiException exception) {
    return ResponseEntity.status(exception.getStatus())
        .body(ErrorResponse.of(exception.getCode(), exception.getMessage()));
  }
}
