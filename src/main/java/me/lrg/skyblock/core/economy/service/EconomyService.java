package me.lrg.skyblock.core.economy.service;

import me.lrg.skyblock.core.economy.config.EconomySettings;
import me.lrg.skyblock.core.economy.model.BankAccount;
import me.lrg.skyblock.core.economy.model.BankLevel;
import me.lrg.skyblock.core.economy.model.EconomyFailure;
import me.lrg.skyblock.core.economy.model.EconomyResult;
import me.lrg.skyblock.core.economy.model.EconomyTransaction;
import me.lrg.skyblock.core.economy.model.InterestRunResult;
import me.lrg.skyblock.core.economy.model.PlayerIdentity;
import me.lrg.skyblock.core.economy.repository.EconomyRepository;
import me.lrg.skyblock.core.manager.CoinManager;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantLock;

public final class EconomyService {
    private final EconomyRepository repository;
    private final CoinManager coinManager;
    private final EconomySettings settings;
    private final ConcurrentMap<UUID, ReentrantLock> locks = new ConcurrentHashMap<>();

    public EconomyService(EconomyRepository repository, CoinManager coinManager, EconomySettings settings) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.coinManager = Objects.requireNonNull(coinManager, "coinManager");
        this.settings = Objects.requireNonNull(settings, "settings");
    }

    public long getWalletBalance(UUID uuid) {
        Objects.requireNonNull(uuid, "uuid");
        EconomyRepository.OptionalLongValue value = repository.walletBalance(uuid);
        return value.present() ? value.value() : 0L;
    }

    public BankAccount getBankAccount(UUID uuid) {
        Objects.requireNonNull(uuid, "uuid");
        return repository.bankAccount(uuid);
    }

    public Optional<PlayerIdentity> findPlayer(String name) {
        if (name == null || name.isBlank()) {
            return Optional.empty();
        }
        return repository.findPlayerByName(name);
    }

    public EconomyResult depositWallet(UUID uuid, long amount, String reason, String operationId) {
        if (!validWrite(uuid, amount, operationId)) {
            return EconomyResult.failure(EconomyFailure.INVALID_AMOUNT);
        }
        return withLock(uuid, () -> sync(
                uuid,
                repository.adjustWallet(uuid, amount, "WALLET_DEPOSIT", reason, operationId)
        ));
    }

    public EconomyResult withdrawWallet(UUID uuid, long amount, String reason, String operationId) {
        if (!validWrite(uuid, amount, operationId)) {
            return EconomyResult.failure(EconomyFailure.INVALID_AMOUNT);
        }
        return withLock(uuid, () -> sync(
                uuid,
                repository.adjustWallet(uuid, -amount, "WALLET_WITHDRAW", reason, operationId)
        ));
    }

    public EconomyResult setWallet(UUID uuid, long amount, String reason, String operationId) {
        if (uuid == null
                || !EconomyOperationValidator.isValidBalance(amount)
                || !EconomyOperationValidator.isValidOperationId(operationId)) {
            return EconomyResult.failure(EconomyFailure.INVALID_AMOUNT);
        }

        return withLock(uuid, () -> {
            long current = getWalletBalance(uuid);
            long delta;
            try {
                delta = Math.subtractExact(amount, current);
            } catch (ArithmeticException exception) {
                return EconomyResult.failure(EconomyFailure.INVALID_AMOUNT);
            }

            if (delta == 0L) {
                return EconomyResult.success(0L, 0L, current, repository.bankAccount(uuid).balance());
            }

            return sync(uuid, repository.adjustWallet(
                    uuid,
                    delta,
                    "WALLET_SET",
                    reason,
                    operationId
            ));
        });
    }

    public EconomyResult depositBank(UUID uuid, long amount, String operationId) {
        if (!validWrite(uuid, amount, operationId)) {
            return EconomyResult.failure(EconomyFailure.INVALID_AMOUNT);
        }

        return withLock(uuid, () -> {
            BankAccount account = repository.bankAccount(uuid);
            long capacity = settings.level(account.level()).capacity();
            if (capacity < 0L || account.balance() < 0L || account.balance() > capacity) {
                return EconomyResult.failure(EconomyFailure.DATABASE_ERROR);
            }
            return sync(uuid, repository.depositToBank(uuid, amount, capacity, operationId));
        });
    }

    public EconomyResult withdrawBank(UUID uuid, long amount, String operationId) {
        if (!validWrite(uuid, amount, operationId)) {
            return EconomyResult.failure(EconomyFailure.INVALID_AMOUNT);
        }
        return withLock(uuid, () -> sync(uuid, repository.withdrawFromBank(uuid, amount, operationId)));
    }

    public EconomyResult transfer(UUID source, UUID target, long amount, String operationId) {
        if (source == null
                || target == null
                || source.equals(target)
                || !EconomyOperationValidator.isValidPositiveAmount(amount)
                || !EconomyOperationValidator.isValidOperationId(operationId)) {
            return EconomyResult.failure(EconomyFailure.INVALID_AMOUNT);
        }

        long fee = settings.calculateTransferFee(amount);
        if (fee < 0L || !EconomyOperationValidator.canAddWithoutOverflow(amount, fee)) {
            return EconomyResult.failure(EconomyFailure.INVALID_AMOUNT);
        }

        UUID first = source.toString().compareTo(target.toString()) <= 0 ? source : target;
        UUID second = first.equals(source) ? target : source;
        ReentrantLock firstLock = lock(first);
        ReentrantLock secondLock = lock(second);
        firstLock.lock();
        secondLock.lock();
        try {
            EconomyResult result = repository.transfer(
                    source,
                    target,
                    amount,
                    fee,
                    settings.transferDailyLimit(),
                    LocalDate.now(settings.zoneId()),
                    operationId
            );
            if (result.success()) {
                coinManager.setCoins(source, result.walletBalance());
                EconomyRepository.OptionalLongValue targetBalance = repository.walletBalance(target);
                if (targetBalance.present()) {
                    coinManager.setCoins(target, targetBalance.value());
                }
            }
            return result;
        } finally {
            secondLock.unlock();
            firstLock.unlock();
        }
    }

    public EconomyResult upgradeBank(UUID uuid, String operationId) {
        if (uuid == null || !EconomyOperationValidator.isValidOperationId(operationId)) {
            return EconomyResult.failure(EconomyFailure.INVALID_AMOUNT);
        }

        return withLock(uuid, () -> {
            BankAccount account = repository.bankAccount(uuid);
            BankLevel next = settings.nextLevel(account.level());
            if (next == null) {
                return EconomyResult.failure(EconomyFailure.MAX_BANK_LEVEL);
            }
            if (!EconomyOperationValidator.isValidPositiveAmount(next.upgradeCost())) {
                return EconomyResult.failure(EconomyFailure.INVALID_AMOUNT);
            }
            return sync(uuid, repository.upgradeBank(
                    uuid,
                    account.level(),
                    next.level(),
                    next.upgradeCost(),
                    operationId
            ));
        });
    }

    public List<EconomyTransaction> history(UUID uuid, int limit) {
        Objects.requireNonNull(uuid, "uuid");
        return repository.history(uuid, limit);
    }

    public InterestRunResult runInterest(LocalDate date) {
        Objects.requireNonNull(date, "date");
        return repository.applyDailyInterest(
                date,
                settings.interestRate(),
                level -> settings.level(level).capacity()
        );
    }

    public Optional<LocalDate> latestInterestRunDate() {
        return repository.latestInterestRunDate();
    }

    public EconomySettings settings() {
        return settings;
    }

    private boolean validWrite(UUID uuid, long amount, String operationId) {
        return uuid != null
                && EconomyOperationValidator.isValidPositiveAmount(amount)
                && EconomyOperationValidator.isValidOperationId(operationId);
    }

    private EconomyResult sync(UUID uuid, EconomyResult result) {
        if (result.success()) {
            coinManager.setCoins(uuid, result.walletBalance());
        }
        return result;
    }

    private EconomyResult withLock(UUID uuid, Operation operation) {
        ReentrantLock lock = lock(uuid);
        lock.lock();
        try {
            return operation.run();
        } finally {
            lock.unlock();
        }
    }

    private ReentrantLock lock(UUID uuid) {
        return locks.computeIfAbsent(uuid, ignored -> new ReentrantLock());
    }

    @FunctionalInterface
    private interface Operation {
        EconomyResult run();
    }
}
