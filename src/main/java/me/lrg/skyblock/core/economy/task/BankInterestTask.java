package me.lrg.skyblock.core.economy.task;

import me.lrg.skyblock.core.economy.model.InterestRunResult;
import me.lrg.skyblock.core.economy.service.EconomyService;
import me.lrg.skyblock.core.economy.service.InterestDueDateCalculator;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

public final class BankInterestTask {
    private final JavaPlugin plugin;
    private final EconomyService economyService;
    private final InterestDueDateCalculator dueDateCalculator = new InterestDueDateCalculator();
    private final AtomicBoolean checking = new AtomicBoolean(false);
    private BukkitTask task;

    public BankInterestTask(JavaPlugin plugin, EconomyService economyService) {
        this.plugin = plugin;
        this.economyService = economyService;
    }

    public void start() {
        if (task != null) return;
        task = plugin.getServer().getScheduler().runTaskTimerAsynchronously(
                plugin,
                this::checkSafely,
                20L,
                20L * 60L
        );
    }

    public void stop() {
        if (task == null) return;
        task.cancel();
        task = null;
    }

    private void checkSafely() {
        if (!checking.compareAndSet(false, true)) {
            plugin.getLogger().warning("銀行利息タスクは既に実行中のため、今回の確認をスキップしました。");
            return;
        }

        try {
            ZonedDateTime now = ZonedDateTime.now(economyService.settings().zoneId());
            LocalTime scheduled = LocalTime.of(
                    economyService.settings().interestHour(),
                    economyService.settings().interestMinute()
            );
            LocalDate dueDate = dueDateCalculator.dueDate(now, scheduled);
            Optional<LocalDate> latest = economyService.latestInterestRunDate();
            if (!dueDateCalculator.shouldRun(latest, dueDate)) return;

            InterestRunResult result = economyService.runInterest(dueDate);
            if (result.executed()) {
                plugin.getLogger().info("銀行利息を実行しました。date=" + result.date()
                        + ", accounts=" + result.affectedAccounts()
                        + ", interest=" + result.totalInterest());
            }
        } catch (RuntimeException exception) {
            plugin.getLogger().log(Level.SEVERE, "銀行利息タスクに失敗しました。次回の確認で再試行します。", exception);
        } finally {
            checking.set(false);
        }
    }
}
