package com.example.finance.repository;

import com.example.finance.entity.RecurringTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface RecurringTransactionRepository extends JpaRepository<RecurringTransaction, Long> {

    List<RecurringTransaction> findByUserIdOrderByNextDueDateAsc(Long userId);

    List<RecurringTransaction> findByUserIdAndActiveTrueOrderByNextDueDateAsc(Long userId);

    @Query("SELECT r FROM RecurringTransaction r WHERE r.user.id = :userId AND r.active = true AND r.nextDueDate <= :cutoffDate ORDER BY r.nextDueDate ASC")
    List<RecurringTransaction> findDueForUser(@Param("userId") Long userId, @Param("cutoffDate") LocalDate cutoffDate);

    Optional<RecurringTransaction> findByIdAndUserId(Long id, Long userId);
}
