package go.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

class DirectionTest {
    // Testy poszczególnych kierunków oraz ich współrzędnych
    @Test
    void upDirectionHasCorrectCoordinates() {
        Direction up = Direction.UP;
        assertEquals(0, up.getDx());
        assertEquals(-1, up.getDy());
    }

    @Test
    void downDirectionHasCorrectCoordinates() {
        Direction down = Direction.DOWN;
        assertEquals(0, down.getDx());
        assertEquals(1, down.getDy());
    }

    @Test
    void leftDirectionHasCorrectCoordinates() {
        Direction left = Direction.LEFT;
        assertEquals(-1, left.getDx());
        assertEquals(0, left.getDy());
    }

    @Test
    void rightDirectionHasCorrectCoordinates() {
        Direction right = Direction.RIGHT;
        assertEquals(1, right.getDx());
        assertEquals(0, right.getDy());
    }
    @Test
    void directionEnumHasFourValues() {
        Direction[] directions = Direction.values();
        assertEquals(4, directions.length);
    }

    @Test
    void allDirectionsReturned() {
        Direction[] directions = Direction.values();
        boolean hasUp = false;
        boolean hasDown = false;
        boolean hasLeft = false;
        boolean hasRight = false;

        for (Direction d : directions) {
            if (d == Direction.UP) hasUp = true;
            if (d == Direction.DOWN) hasDown = true;
            if (d == Direction.LEFT) hasLeft = true;
            if (d == Direction.RIGHT) hasRight = true;
        }

        assertTrue(hasUp);
        assertTrue(hasDown);
        assertTrue(hasLeft);
        assertTrue(hasRight);
    }

    @Test
    void oppositeDirectionsHaveOppositeCoordinates() {
        assertEquals(Direction.UP.getDy(), -Direction.DOWN.getDy());
        assertEquals(Direction.LEFT.getDx(), -Direction.RIGHT.getDx());
    }
}
