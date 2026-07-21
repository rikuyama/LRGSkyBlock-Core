package me.lrg.skyblock.core.economy.config;

import me.lrg.skyblock.core.economy.model.BankLevel;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class EconomySettings {
    private final ZoneId zoneId;
    private final int interestHour;
    private final int interestMinute;
    private final double interestRate;
    private final long transferDailyLimit;
    private final double transferFeeRate;
    private final List<BankLevel> bankLevels;

    private EconomySettings(
            ZoneId zoneId,
            int interestHour,
            int interestMinute,
            double interestRate,
            long transferDailyLimit,
            double transferFeeRate,
            List<BankLevel> bankLevels
    ) {
        this.zoneId = zoneId;
        this.interestHour = interestHour;
        this.interestMinute = interestMinute;
        this.interestRate = interestRate;
        this.transferDailyLimit = transferDailyLimit;
        this.transferFeeRate = transferFeeRate;
        this.bankLevels = List.copyOf(bankLevels);
    }

    public static EconomySettings load(JavaPlugin plugin) {
        String configuredZone = plugin.getConfig().getString("economy.timezone", "Asia/Tokyo");
        ZoneId zone;
        try {
            zone = ZoneId.of(configuredZone == null ? "Asia/Tokyo" : configuredZone);
        } catch (DateTimeException exception) {
            plugin.getLogger().warning("economy.timezone が不正なため Asia/Tokyo を使用します: " + configuredZone);
            zone = ZoneId.of("Asia/Tokyo");
        }

        int hour = clamp(plugin.getConfig().getInt("economy.interest.hour", 4), 0, 23);
        int minute = clamp(plugin.getConfig().getInt("economy.interest.minute", 0), 0, 59);
        double rate = plugin.getConfig().getDouble("economy.interest.rate", 0.02D);
        if (!Double.isFinite(rate) || rate < 0.0D) rate = 0.02D;
        long dailyLimit = Math.max(0L, plugin.getConfig().getLong("economy.transfer.daily-limit", 10_000_000L));
        double feeRate = plugin.getConfig().getDouble("economy.transfer.fee-rate", 0.0D);
        if (!Double.isFinite(feeRate) || feeRate < 0.0D || feeRate > 1.0D) feeRate = 0.0D;

        List<BankLevel> levels = new ArrayList<>();
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("economy.bank-levels");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                int level;
                try {
                    level = Integer.parseInt(key);
                } catch (NumberFormatException exception) {
                    continue;
                }
                long capacity = Math.max(0L, section.getLong(key + ".capacity", 0L));
                long upgradeCost = Math.max(0L, section.getLong(key + ".upgrade-cost", 0L));
                if (level >= 1 && capacity > 0L) levels.add(new BankLevel(level, capacity, upgradeCost));
            }
        }
        if (levels.isEmpty()) {
            levels.add(new BankLevel(1, 1_000_000L, 0L));
            levels.add(new BankLevel(2, 5_000_000L, 100_000L));
            levels.add(new BankLevel(3, 25_000_000L, 500_000L));
            levels.add(new BankLevel(4, 100_000_000L, 2_500_000L));
            levels.add(new BankLevel(5, 1_000_000_000L, 10_000_000L));
        }
        levels.sort(Comparator.comparingInt(BankLevel::level));
        return new EconomySettings(zone, hour, minute, rate, dailyLimit, feeRate, levels);
    }

    public ZoneId zoneId() { return zoneId; }
    public int interestHour() { return interestHour; }
    public int interestMinute() { return interestMinute; }
    public double interestRate() { return interestRate; }
    public long transferDailyLimit() { return transferDailyLimit; }
    public double transferFeeRate() { return transferFeeRate; }
    public List<BankLevel> bankLevels() { return bankLevels; }

    public BankLevel level(int level) {
        BankLevel fallback = bankLevels.getFirst();
        for (BankLevel candidate : bankLevels) {
            if (candidate.level() == level) {
                return candidate;
            }
            if (candidate.level() <= level) {
                fallback = candidate;
            }
        }
        return fallback;
    }

    public BankLevel nextLevel(int currentLevel) {
        return bankLevels.stream().filter(candidate -> candidate.level() > currentLevel).findFirst().orElse(null);
    }

    public long calculateTransferFee(long amount) {
        if (amount <= 0L || transferFeeRate <= 0.0D) return 0L;
        double raw = Math.floor(amount * transferFeeRate);
        return raw >= Long.MAX_VALUE ? Long.MAX_VALUE : Math.max(0L, (long) raw);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
