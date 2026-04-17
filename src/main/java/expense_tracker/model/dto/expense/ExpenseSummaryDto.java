package expense_tracker.model.dto.expense;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ExpenseSummaryDto {

    private Period period;
    private String currency;
    private BigDecimal totalExpenses;
    private List<CategoryItem> categories;

    @Getter
    @Setter
    @Builder
    public static class Period {
        private String filter;
        private LocalDate startDate;
        private LocalDate endDate;
    }

    @Getter
    @Setter
    @Builder
    public static class CategoryItem {
        private String name;
        private BigDecimal value;
    }
}