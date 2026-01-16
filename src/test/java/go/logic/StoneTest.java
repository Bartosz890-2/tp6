package go.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

class StoneTest {

    @Test
    void blackStoneOpponentIsWhite() {
        assertEquals(Stone.WHITE, Stone.BLACK.opponent());
    }

    @Test
    void whiteStoneOpponentIsBlack() {
        assertEquals(Stone.BLACK, Stone.WHITE.opponent());
    }

    @Test
    void emptyStoneOpponentIsEmpty() {
        assertEquals(Stone.EMPTY, Stone.EMPTY.opponent());
    }

    @Test
    void stoneEnumHasThreeValues() {
        Stone[] stones = Stone.values();
        assertEquals(3, stones.length);
    }

    @Test
    void stoneValuesContainBlackWhiteEmpty() {
        Stone[] stones = Stone.values();
        boolean hasBlack = false;
        boolean hasWhite = false;
        boolean hasEmpty = false;

        for (Stone stone : stones) {
            if (stone == Stone.BLACK) hasBlack = true;
            if (stone == Stone.WHITE) hasWhite = true;
            if (stone == Stone.EMPTY) hasEmpty = true;
        }

        assertTrue(hasBlack);
        assertTrue(hasWhite);
        assertTrue(hasEmpty);
    }
}
