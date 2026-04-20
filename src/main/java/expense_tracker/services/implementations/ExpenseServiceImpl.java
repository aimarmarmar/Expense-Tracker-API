package expense_tracker.services.implementations;

import expense_tracker.exception.BadRequestException;
import expense_tracker.exception.NotFoundException;
import expense_tracker.model.dto.common.PagedResponseDto;
import expense_tracker.model.dto.expense.*;
import expense_tracker.model.entity.Expense;
import expense_tracker.model.entity.User;
import expense_tracker.repository.ExpenseRepository;
import expense_tracker.services.ExpenseService;
import expense_tracker.services.UserService;
import expense_tracker.util.ExpenseCodeGenerator;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.UUID;


@Slf4j
@Service
@AllArgsConstructor
public class ExpenseServiceImpl implements ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final UserService userService;


    public ExpenseResponseDto createExpense(CreateExpenseRequestDto request, String username) {

        User user = userService.getActiveUserByUsername(username);

        String code = ExpenseCodeGenerator.generate();

        Expense expense = new Expense();
        LocalDateTime now = LocalDateTime.now();

        expense.setUser(user);
        expense.setExpenseCode(code);
        expense.setAmount(request.getAmount());
        expense.setCategory(request.getCategory());
        expense.setDescription(request.getDescription());
        expense.setExpenseDate(request.getExpenseDate());
        expense.setCreatedAt(now);
        expense.setUpdatedAt(now);
        expense.setDeleted(false);
        expense.setDeletedAt(null);

        Expense savedExpense = expenseRepository.save(expense);

        return mapToResponse(savedExpense);
    }

    public List<ExpenseResponseDto> getExpensesByUsername(String username) {

        List<Expense> expenses = expenseRepository
                .findByUserUsernameAndDeletedFalse(username);

        return expenses.stream()
                .map(this::mapToResponse)
                .toList();
    }

    public ExpenseResponseDto updateExpense(
            UUID expenseId,
            UpdateExpenseRequestDto request,
            String username
    ) {
        Expense expense = expenseRepository
                .findByIdAndDeletedFalse(expenseId)
                .orElseThrow(() -> new NotFoundException("Expense not found"));

        validateOwnership(expense, username);

        if (request.getAmount() != null) {
            expense.setAmount(request.getAmount());
        }

        if (request.getCategory() != null) {
            expense.setCategory(request.getCategory());
        }

        if (request.getDescription() != null) {
            expense.setDescription(request.getDescription().trim());
        }

        if (request.getExpenseDate() != null) {
            if (request.getExpenseDate().isAfter(LocalDate.now())) {
                throw new BadRequestException("Expense date cannot be in the future");
            }
            expense.setExpenseDate(request.getExpenseDate());
        }

        expense.setUpdatedAt(LocalDateTime.now());

        return mapToResponse(expenseRepository.save(expense));
    }

    public void deleteExpense(UUID expenseId, String username) {

        Expense expense = expenseRepository
                .findByIdAndDeletedFalse(expenseId)
                .orElseThrow(() -> new NotFoundException("Expense not found"));

        validateOwnership(expense, username);

        expense.setDeleted(true);
        expense.setUpdatedAt(LocalDateTime.now());
        expense.setDeletedAt(LocalDateTime.now());

        expenseRepository.save(expense);

    }

    public PagedResponseDto<ExpenseResponseDto> fetchUserExpenses(
            String username,
            String filter,
            String startDate,
            String endDate,
            int page,
            int size,
            String sort
    ) {

        LocalDate[] dates = parseDateRange(startDate, endDate);
        LocalDate start = dates[0];
        LocalDate end = dates[1];

        LocalDate[] filtered = applyPresetFilter(filter, start, end);
        start = filtered[0];
        end = filtered[1];

        Pageable pageable = buildPageable(page, size, sort);

        Page<Expense> expenses;

        if (start != null && end != null) {
            expenses = expenseRepository
                    .findByUserUsernameAndDeletedFalseAndExpenseDateBetween(
                            username, start, end, pageable);
        } else {
            expenses = expenseRepository.findByUserUsernameAndDeletedFalse(username, pageable);
        }

        return PagedResponseDto.<ExpenseResponseDto>builder()
                .data(expenses.getContent().stream()
                        .map(this::mapToResponse)
                        .toList())
                .page(page)
                .size(size)
                .totalElements(expenses.getTotalElements())
                .totalPages(expenses.getTotalPages())
                .build();
    }

    public ExpenseSummaryDto getSummary(
            String username,
            String filter,
            String startDate,
            String endDate
    ) {
        LocalDate[] dates = parseDateRange(startDate, endDate);
        LocalDate start = dates[0];
        LocalDate end = dates[1];

        LocalDate[] filtered = applyPresetFilter(filter, start, end);
        start = filtered[0];
        end = filtered[1];

        String filterType;
        if (startDate != null && endDate != null) {
            filterType = "custom";
        } else if (filter == null || filter.isBlank()) {
            filterType = "all";
        } else {
            filterType = filter.toLowerCase();
        }

        Object[] summaryResult =
                expenseRepository.getExpenseSummaryWithFilter(username, start, end);

        if (summaryResult.length == 1 && summaryResult[0] instanceof Object[]) {
            summaryResult = (Object[]) summaryResult[0];
        }

        BigDecimal totalExpenses = toBigDecimal(summaryResult[0])
                .setScale(2, RoundingMode.HALF_UP);

        List<ExpenseSummaryDto.CategoryItem> categories =
                expenseRepository.getExpenseSummaryByCategoryWithFilter(username, start, end)
                        .stream()
                        .map(row -> ExpenseSummaryDto.CategoryItem.builder()
                                .name(row[0].toString())
                                .value(toBigDecimal(row[1])
                                        .setScale(2, RoundingMode.HALF_UP))
                                .build())
                        .toList();

        return ExpenseSummaryDto.builder()
                .period(ExpenseSummaryDto.Period.builder()
                        .filter(filterType)
                        .startDate(start)
                        .endDate(end)
                        .build())
                .currency("IDR")
                .totalExpenses(totalExpenses)
                .categories(categories)
                .build();
    }

    public ExpenseResponseDto mapToResponse(Expense expense) {
        return ExpenseResponseDto.builder()
                .id(expense.getId())
                .expenseCode(expense.getExpenseCode())
                .amount(expense.getAmount())
                .category(expense.getCategory().name())
                .description(expense.getDescription())
                .expenseDate(expense.getExpenseDate())
                .createdAt(expense.getCreatedAt())
                .updatedAt(expense.getUpdatedAt())
                .build();
    }

    private LocalDate[] parseDateRange(String startDate, String endDate) {

        if ((startDate != null && endDate == null) ||
                (startDate == null && endDate != null)) {
            throw new BadRequestException("startDate and endDate must be provided together");
        }

        if (startDate == null && endDate == null) {
            return new LocalDate[]{null, null};
        }

        try {
            LocalDate start = LocalDate.parse(startDate);
            LocalDate end = LocalDate.parse(endDate);

            if (start.isAfter(end)) {
                throw new BadRequestException("startDate cannot be after endDate");
            }

            return new LocalDate[]{start, end};

        } catch (DateTimeParseException e) {
            throw new BadRequestException("Invalid date format. Use yyyy-MM-dd");
        }
    }

    private LocalDate[] applyPresetFilter(String filter, LocalDate start, LocalDate end) {

        if (start != null && end != null) {
            return new LocalDate[]{start, end};
        }

        if (filter == null || filter.isBlank()) {
            return new LocalDate[]{start, end};
        }

        LocalDate today = LocalDate.now();

        switch (filter.toLowerCase()) {
            case "week":
                return new LocalDate[]{today.minusWeeks(1), today};
            case "month":
                return new LocalDate[]{today.minusMonths(1), today};
            case "3months":
                return new LocalDate[]{today.minusMonths(3), today};
            default:
                throw new BadRequestException("Invalid filter value. Use: week, month, 3months");
        }
    }

    private Pageable buildPageable(int page, int size, String sort) {

        if (page < 1) {
            throw new BadRequestException("Page must be greater than or equal to 1");
        }

        if (size < 1) {
            throw new BadRequestException("Size must be greater than 0");
        }

        if (sort == null || sort.isBlank()) {
            sort = "createdAt,desc";
        }

        String[] parts = sort.split(",");
        String field = parts[0];
        String direction = parts.length > 1 ? parts[1] : "asc";

        List<String> allowed = List.of("amount", "expenseDate", "createdAt");

        if (!allowed.contains(field)) {
            throw new BadRequestException("Invalid sort field");
        }

        Sort.Direction dir = direction.equalsIgnoreCase("desc")
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;

        return PageRequest.of(page - 1, size, Sort.by(dir, field));
    }

    private void validateOwnership(Expense expense, String username) {
        if (!expense.getUser().getUsername().equals(username)) {
            throw new NotFoundException("Expense not found for user: " + username);
        }
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value == null) return BigDecimal.ZERO;

        if (value instanceof BigDecimal bd) {
            return bd;
        }

        if (value instanceof Number num) {
            return BigDecimal.valueOf(num.doubleValue());
        }

        throw new IllegalArgumentException("Unsupported number type: " + value.getClass());
    }
}
