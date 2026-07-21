package me.lrg.skyblock.core.economy.task;

import me.lrg.skyblock.core.economy.model.InterestRunResult;
import me.lrg.skyblock.core.economy.service.EconomyService;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.logging.Level;

public final class BankInterestTask {
    private final JavaPlugin plugin;
    private final EconomyService economyService;
    private BukkitTask task;

    public BankInterestTask(JavaPlugin plugin, EconomyService economyService) {
        this.plugin = plugin;
        this.economyService = economyService;
    }

    public void start() {
        if (task != null) return;
        task = plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, this::check, 20L, 20L * 60L);
    }

    public void stop() {
        if (task == null) return;
        task.cancel();
        task = null;
    }

    private void check() {
        try {
            ZonedDateTime now = ZonedDateTime.now(economyService.settings().zoneId());
            LocalTime scheduled = LocalTime.of(economyService.settings().interestHour(), economyService.settings().interestMinute());
            LocalDate dueDate = now.toLocalTime().isBefore(scheduled) ? now.toLocalDate().minusDays(1L) : now.toLocalDate();
            Optional<LocalDate> latest = economyService.latestInterestRunDate();
            if (latest.isPresent() && !latest.get().isBefore(dueDate)) return;
            InterestRunResult result = economyService.runInterest(dueDate);
            if (result.executed()) {
                plugin.getLogger().info("銀行利息を実行しました。date=" + result.date()
                        + ", accounts=" + result.affectedAccounts() + ", interest=" + result.totalInterest());
            }
        } catch (RuntimeException exception) {
            plugin.getLogger().log(Level.SEVERE, "銀行利息タスクに失敗しました。", exception);
        }
    }
}
