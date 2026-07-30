package com.school.canteen.repository;

import com.school.canteen.entity.Expense;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ExpenseRepository extends JpaRepository<Expense, UUID> {

    List<Expense> findByExpenseDateBetweenOrderByExpenseDateDesc(LocalDate from, LocalDate to);

    /** coalesce so a period with no expenses returns zero rather than null. */
    @Query("""
            select coalesce(sum(e.amount), 0)
              from Expense e
             where e.expenseDate between :from and :to
            """)
    BigDecimal totalBetween(@Param("from") LocalDate from, @Param("to") LocalDate to);
}
