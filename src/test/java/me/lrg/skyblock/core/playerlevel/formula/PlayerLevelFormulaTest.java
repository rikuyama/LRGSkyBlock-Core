package me.lrg.skyblock.core.playerlevel.formula;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PlayerLevelFormulaTest {
    @Test void levels1To40Require100Xp() {
        assertEquals(100L, PlayerLevelFormula.getRequiredXp(1));
        assertEquals(100L, PlayerLevelFormula.getRequiredXp(40));
    }
    @Test void levels41To80Require150Xp() {
        assertEquals(150L, PlayerLevelFormula.getRequiredXp(41));
        assertEquals(150L, PlayerLevelFormula.getRequiredXp(80));
    }
    @Test void eachFortyLevelTierAdds50Xp() {
        assertEquals(200L, PlayerLevelFormula.getRequiredXp(81));
        assertEquals(250L, PlayerLevelFormula.getRequiredXp(121));
    }
    @Test void rejectsZeroLevel() {
        assertThrows(IllegalArgumentException.class, () -> PlayerLevelFormula.getRequiredXp(0));
    }
}
