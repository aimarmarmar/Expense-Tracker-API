package expense_tracker.controller;

import expense_tracker.model.dto.common.PagedResponseDto;
import expense_tracker.model.dto.expense.*;
import expense_tracker.services.ExpenseService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/expenses")
@RequiredArgsConstructor
@Slf4j
public class ExpenseController {

    private final ExpenseService expenseService;

    @PostMapping("/create")
    public ExpenseResponseDto createExpense(
            @Valid @RequestBody CreateExpenseRequestDto request,
            @AuthenticationPrincipal UserDetails userDetails) {

        log.info("Received request to create expense by user {}", userDetails.getUsername());

        return expenseService.createExpense(request, userDetails.getUsername());
    }

    @PatchMapping("update/{id}")
    public ExpenseResponseDto updateExpense(
            @Valid @RequestBody UpdateExpenseRequestDto request,
            @PathVariable UUID id,
            Authentication authentication
    ) {
        log.info("Received request to update expense by user {}", authentication.getName());
        return expenseService.updateExpense(id, request, authentication.getName());
    }

    @DeleteMapping("delete/{id}")
    public ResponseEntity<Void> deleteExpense(
            @Valid @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        log.info("Received request to delete expense by user {}", userDetails.getUsername());

        expenseService.deleteExpense(id, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/fetch")
    public PagedResponseDto<ExpenseResponseDto> getExpenses(
            @RequestParam(required = false) String filter,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "5") int size,
            @RequestParam(defaultValue = "expenseDate,desc") String sort,
            @AuthenticationPrincipal UserDetails user
    ) {
        return expenseService.fetchUserExpenses(
                user.getUsername(),
                filter,
                startDate,
                endDate,
                page,
                size,
                sort
        );
    }

    @GetMapping("/summary")
    public ResponseEntity<ExpenseSummaryDto> getSummary(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) String filter,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate
    ) {

        String username = (userDetails != null)
                ? userDetails.getUsername()
                : "testUser";

        return ResponseEntity.ok(
                expenseService.getSummary(
                        username,
                        filter,
                        startDate,
                        endDate
                )
        );
    }

}
