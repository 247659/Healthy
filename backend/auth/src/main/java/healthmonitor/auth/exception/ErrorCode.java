package healthmonitor.auth.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;


@Getter
@AllArgsConstructor
public enum ErrorCode {
    EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "Email already exists"),
    INCORRECT_CREDENTIALS(HttpStatus.UNAUTHORIZED, "Invalid email or login"),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "Incorrect Access Token, access denied"),
    INCORRECT_ROLE(HttpStatus.FORBIDDEN, "Access denied, incorrect role"),
    INTERVAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error"),
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "Your session has expired. Please log in again.");

    private final HttpStatus httpStatus;
    private final String message;

}
