package expense_tracker.services;

import expense_tracker.model.dto.common.PagedResponseDto;
import expense_tracker.model.dto.expense.*;
import expense_tracker.model.entity.Expense;

import java.util.List;
import java.util.UUID;

public interface ExpenseService {

    ExpenseResponseDto createExpense(CreateExpenseRequestDto request, String username);
    List<ExpenseResponseDto> getExpensesByUsername(String username);
    ExpenseResponseDto updateExpense(UUID expenseId, UpdateExpenseRequestDto request, String username);
    void deleteExpense(UUID expenseId, String username);
    PagedResponseDto<ExpenseResponseDto> fetchUserExpenses(
            String username,
            String filter,
            String startDate,
            String endDate,
            int page,
            int size,
            String sort
    );
    ExpenseResponseDto mapToResponse(Expense expense);
    ExpenseSummaryDto getSummary(
            String username,
            String filter,
            String startDate,
            String endDate
    );
}
