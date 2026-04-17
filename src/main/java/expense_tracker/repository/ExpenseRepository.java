package expense_tracker.repository;

import expense_tracker.model.entity.Expense;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, UUID> {

    List<Expense> findByUserUsernameAndDeletedFalse(String username);
    Page<Expense> findByUserUsernameAndDeletedFalse(String username, Pageable pageable);
    Optional<Expense> findByIdAndDeletedFalse(UUID id);

    Page<Expense> findByUserUsernameAndDeletedFalseAndExpenseDateBetween(
                String username,
                LocalDate startDate,
                LocalDate endDate,
                Pageable pageable
        );

    @Query("""
    SELECT 
        COALESCE(SUM(e.amount), 0)
    FROM Expense e
    WHERE e.user.username = :username
      AND e.deleted = false
      AND (CAST(:startDate AS date) IS NULL OR e.expenseDate >= :startDate)
      AND (CAST(:endDate AS date) IS NULL OR e.expenseDate <= :endDate)
""")
    Object[] getExpenseSummaryWithFilter(
            String username,
            LocalDate startDate,
            LocalDate endDate
    );

    @Query("""
    SELECT e.category, COALESCE(SUM(e.amount), 0)
    FROM Expense e
    WHERE e.user.username = :username
      AND e.deleted = false
      AND (CAST(:startDate AS date) IS NULL OR e.expenseDate >= :startDate)
      AND (CAST(:endDate AS date) IS NULL OR e.expenseDate <= :endDate)
    GROUP BY e.category
    ORDER BY COALESCE(SUM(e.amount), 0) DESC
""")
    List<Object[]> getExpenseSummaryByCategoryWithFilter(
            String username,
            LocalDate startDate,
            LocalDate endDate
    );
}
