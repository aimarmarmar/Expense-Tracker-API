package expense_tracker.model.enums;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorType {

    BAD_REQUEST(400,"BAD REQUEST",HttpStatus.BAD_REQUEST),
    NOT_FOUND(404,"NOT FOUND",HttpStatus.NOT_FOUND),
    INTERNAL_SERVER_ERROR(500,"INTERNAL SERVER ERROR",HttpStatus.INTERNAL_SERVER_ERROR);

    private final int code;
    private final String message;
    private final HttpStatus httpStatus;

    ErrorType(int code, String message, HttpStatus httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }
}
