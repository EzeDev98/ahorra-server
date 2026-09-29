package com.ahorra.feature.behavior.repository;

import com.ahorra.feature.behavior.enums.HabitStatus;
import com.ahorra.feature.behavior.model.DetectedHabit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DetectedHabitRepository extends JpaRepository<DetectedHabit, Long> {

    Optional<DetectedHabit> findByUserIdAndCategoryId(Long userId, Long categoryId);

    List<DetectedHabit> findAllByUserIdAndStatus(Long userId, HabitStatus status);

    List<DetectedHabit> findAllByUserId(Long userId);
}
