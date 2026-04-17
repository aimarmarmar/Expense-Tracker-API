package expense_tracker.model.dto;

import expense_tracker.model.enums.ErrorType;
import lombok.*;

import java.time.LocalDateTime;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {

    LocalDateTime timestamp;
    int status;
    ErrorType error;
    String message;
    String path;
}
