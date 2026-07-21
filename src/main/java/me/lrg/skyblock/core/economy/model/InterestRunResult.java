package me.lrg.skyblock.core.economy.model;

import java.time.LocalDate;

public record InterestRunResult(boolean executed, LocalDate date, int affectedAccounts, long totalInterest) {
}
