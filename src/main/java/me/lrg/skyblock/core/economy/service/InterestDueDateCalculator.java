package me.lrg.skyblock.core.economy.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.Optional;

/** 銀行利息の対象日と実行要否を決定する純粋ロジック。 */
public final class InterestDueDateCalculator {

    public LocalDate dueDate(ZonedDateTime now, LocalTime scheduledTime) {
        Objects.requireNonNull(now, "now");
        Objects.requireNonNull(scheduledTime, "scheduledTime");
        return now.toLocalTime().isBefore(scheduledTime)
                ? now.toLocalDate().minusDays(1L)
                : now.toLocalDate();
    }

    public boolean shouldRun(Optional<LocalDate> latestCompletedDate, LocalDate dueDate) {
        Objects.requireNonNull(latestCompletedDate, "latestCompletedDate");
        Objects.requireNonNull(dueDate, "dueDate");
        return latestCompletedDate.isEmpty() || latestCompletedDate.get().isBefore(dueDate);
    }
}
