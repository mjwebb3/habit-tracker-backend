package com.tp1.habittracker.config;

import com.tp1.habittracker.domain.enums.Frequency;
import com.tp1.habittracker.domain.enums.HabitType;
import com.tp1.habittracker.domain.model.Habit;
import com.tp1.habittracker.domain.model.HabitLog;
import com.tp1.habittracker.domain.model.User;
import com.tp1.habittracker.repository.HabitLogRepository;
import com.tp1.habittracker.repository.HabitRepository;
import com.tp1.habittracker.repository.UserRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.seed", name = "enabled", havingValue = "true", matchIfMissing = true)
public class StartupDataSeeder implements CommandLineRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(StartupDataSeeder.class);

    private final UserRepository userRepository;
    private final HabitRepository habitRepository;
    private final HabitLogRepository habitLogRepository;

    @Override
    public void run(String... args) {
        if (hasExistingData()) {
            LOGGER.info("Skipping startup seed because data already exists.");
            return;
        }

        User user = userRepository.save(User.builder()
                .username("demo_user")
                .email("demo.user@example.com")
                .build());

                if (user == null || user.getId() == null) {
                        LOGGER.warn("Skipping startup seed because repositories are not returning persisted entities.");
                        return;
                }

        Habit hydrationHabit = habitRepository.save(Habit.builder()
                .userId(user.getId())
                .name("Drink 2L water")
                .type(HabitType.BOOLEAN)
                .frequency(Frequency.DAILY)
                .createdAt(Instant.now().minus(7, ChronoUnit.DAYS))
                .build());

        Habit readingHabit = habitRepository.save(Habit.builder()
                .userId(user.getId())
                .name("Read pages")
                .type(HabitType.NUMBER)
                .frequency(Frequency.DAILY)
                .createdAt(Instant.now().minus(5, ChronoUnit.DAYS))
                .build());

        Habit planningHabit = habitRepository.save(Habit.builder()
                .userId(user.getId())
                .name("Weekly planning")
                .type(HabitType.TEXT)
                .frequency(Frequency.WEEKLY)
                .createdAt(Instant.now().minus(14, ChronoUnit.DAYS))
                .build());

                if (hydrationHabit == null || readingHabit == null || planningHabit == null) {
                        LOGGER.warn("Skipping habit log seed because repositories are not returning persisted habits.");
                        return;
                }

        LocalDate today = LocalDate.now();
        habitLogRepository.saveAll(List.of(
                HabitLog.builder().habitId(hydrationHabit.getId()).date(today.minusDays(2)).value(true).build(),
                HabitLog.builder().habitId(hydrationHabit.getId()).date(today.minusDays(1)).value(true).build(),
                HabitLog.builder().habitId(hydrationHabit.getId()).date(today).value(true).build(),
                HabitLog.builder().habitId(readingHabit.getId()).date(today.minusDays(2)).value(18.0).build(),
                HabitLog.builder().habitId(readingHabit.getId()).date(today.minusDays(1)).value(24.0).build(),
                HabitLog.builder().habitId(readingHabit.getId()).date(today).value(30.0).build(),
                HabitLog.builder().habitId(planningHabit.getId()).date(today.minusDays(7)).value("Planned top 3 priorities").build(),
                HabitLog.builder().habitId(planningHabit.getId()).date(today).value("Reviewed and planned next week").build()
        ));

        LOGGER.info("Startup seed inserted: 1 user, 3 habits, 8 habit logs.");
    }

    private boolean hasExistingData() {
        return userRepository.count() > 0 || habitRepository.count() > 0 || habitLogRepository.count() > 0;
    }
}
