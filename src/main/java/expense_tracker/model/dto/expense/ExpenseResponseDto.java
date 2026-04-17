package expense_tracker.model.dto.expense;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.PastOrPresent;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ExpenseResponseDto {

    UUID id;
    String expenseCode;
    BigDecimal amount;
    String category;
    String description;

    @PastOrPresent(message = "Expense date cannot be in the future")
    @JsonFormat(pattern = "yyyy-MM-dd")
    LocalDate expenseDate;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime createdAt;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime updatedAt;

    private Boolean deleted;

    private LocalDateTime deletedAt;
}
