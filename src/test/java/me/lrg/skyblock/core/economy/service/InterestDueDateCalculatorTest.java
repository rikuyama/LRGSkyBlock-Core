package me.lrg.skyblock.core.economy.service;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class InterestDueDateCalculatorTest {
    private final InterestDueDateCalculator calculator = new InterestDueDateCalculator();
    private final ZoneId zone = ZoneId.of("Asia/Tokyo");

    @Test
    void beforeScheduledTimeUsesPreviousDate() {
        ZonedDateTime now = ZonedDateTime.of(2026, 7, 22, 3, 59, 59, 0, zone);
        assertEquals(LocalDate.of(2026, 7, 21), calculator.dueDate(now, LocalTime.of(4, 0)));
    }

    @Test
    void atScheduledTimeUsesCurrentDate() {
        ZonedDateTime now = ZonedDateTime.of(2026, 7, 22, 4, 0, 0, 0, zone);
        assertEquals(LocalDate.of(2026, 7, 22), calculator.dueDate(now, LocalTime.of(4, 0)));
    }

    @Test
    void completedDueDateDoesNotRunAgain() {
        LocalDate due = LocalDate.of(2026, 7, 22);
        assertFalse(calculator.shouldRun(Optional.of(due), due));
        assertFalse(calculator.shouldRun(Optional.of(due.plusDays(1)), due));
    }

    @Test
    void missingOrOlderCompletionRuns() {
        LocalDate due = LocalDate.of(2026, 7, 22);
        assertTrue(calculator.shouldRun(Optional.empty(), due));
        assertTrue(calculator.shouldRun(Optional.of(due.minusDays(1)), due));
    }
}
