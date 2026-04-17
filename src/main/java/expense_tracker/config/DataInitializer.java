//package expense_tracker.config;
//
//import expense_tracker.model.entity.Expense;
//import expense_tracker.repository.ExpenseRepository;
//import expense_tracker.util.ExpenseCodeGenerator;
//import lombok.AllArgsConstructor;
//import org.springframework.boot.CommandLineRunner;
//import org.springframework.stereotype.Component;
//
//import java.math.BigDecimal;
//import java.time.LocalDate;
//import java.time.LocalDateTime;
//import java.util.UUID;
//
//@AllArgsConstructor
//@Component
//public class DataInitializer implements CommandLineRunner {
//
//    private final ExpenseRepository expenseRepository;
//
//
//    @Override
//    public void run(String... args) throws Exception {
//        // Check if the database is empty before seeding
//        if (expenseRepository.count() < 4) {
//
//            Expense expense = new Expense(UUID.randomUUID(),
//                    new BigDecimal("500.000"),
//                    "Shopping",
//                    "buy a hat",
//                    LocalDate.now(),
//                    LocalDateTime.now(),
//                    LocalDateTime.now(),
//                    ExpenseCodeGenerator.generate());
//
//            expenseRepository.save(expense);
//        } else {
//            System.out.println("Database already contains data. Skipping seeding.");
//        }
//    }
//}
