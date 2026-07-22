package me.lrg.skyblock.core.economy.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EconomyOperationValidatorTest {

    @Test
    void positiveAmountRejectsZeroAndNegativeValues() {
        assertFalse(EconomyOperationValidator.isValidPositiveAmount(0L));
        assertFalse(EconomyOperationValidator.isValidPositiveAmount(-1L));
        assertTrue(EconomyOperationValidator.isValidPositiveAmount(1L));
    }

    @Test
    void balanceAllowsZeroButRejectsNegativeValues() {
        assertTrue(EconomyOperationValidator.isValidBalance(0L));
        assertTrue(EconomyOperationValidator.isValidBalance(1L));
        assertFalse(EconomyOperationValidator.isValidBalance(-1L));
    }

    @Test
    void operationIdRejectsNullBlankAndOverLimitValues() {
        assertFalse(EconomyOperationValidator.isValidOperationId(null));
        assertFalse(EconomyOperationValidator.isValidOperationId(""));
        assertFalse(EconomyOperationValidator.isValidOperationId("   "));
        assertFalse(EconomyOperationValidator.isValidOperationId("a".repeat(97)));
        assertTrue(EconomyOperationValidator.isValidOperationId("a".repeat(96)));
    }

    @Test
    void overflowCheckRejectsNegativeAndOverflowingInputs() {
        assertFalse(EconomyOperationValidator.canAddWithoutOverflow(-1L, 1L));
        assertFalse(EconomyOperationValidator.canAddWithoutOverflow(1L, -1L));
        assertFalse(EconomyOperationValidator.canAddWithoutOverflow(Long.MAX_VALUE, 1L));
        assertTrue(EconomyOperationValidator.canAddWithoutOverflow(Long.MAX_VALUE - 1L, 1L));
    }
}
