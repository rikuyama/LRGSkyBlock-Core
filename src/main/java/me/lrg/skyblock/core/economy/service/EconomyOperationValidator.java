package me.lrg.skyblock.core.economy.service;

/**
 * Economyの書き込み操作へ渡す入力値を共通検証する。
 * Repositoryへ不正値を到達させず、全コマンドで同じ基準を使うためのクラス。
 */
public final class EconomyOperationValidator {

    public static final int MAX_OPERATION_ID_LENGTH = 96;

    private EconomyOperationValidator() {
    }

    public static boolean isValidPositiveAmount(long amount) {
        return amount > 0L;
    }

    public static boolean isValidBalance(long amount) {
        return amount >= 0L;
    }

    public static boolean isValidOperationId(String operationId) {
        return operationId != null
                && !operationId.isBlank()
                && operationId.length() <= MAX_OPERATION_ID_LENGTH;
    }

    public static boolean canAddWithoutOverflow(long left, long right) {
        if (left < 0L || right < 0L) {
            return false;
        }
        return left <= Long.MAX_VALUE - right;
    }
}
