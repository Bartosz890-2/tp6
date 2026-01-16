package go.logic;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GameMechanicsTest {

    @Test
    void acceptsValidMoveOnEmptyField() {
        Board board = new Board(5);
        GameMechanics mechanics = new GameMechanics();

        boolean result = mechanics.IsMovePossible(board, 1, 1, Stone.BLACK);

        assertTrue(result);
        assertEquals(Stone.BLACK, board.getField(1, 1));
    }

    @Test
    void rejectsMoveOnOccupiedField() {
        Board board = new Board(5);
        GameMechanics mechanics = new GameMechanics();
        board.setField(2, 2, Stone.WHITE);

        boolean result = mechanics.IsMovePossible(board, 2, 2, Stone.BLACK);

        assertFalse(result);
        assertEquals(Stone.WHITE, board.getField(2, 2));
    }

    @Test
    void capturesAdjacentStoneWithoutLiberties() {
        Board board = new Board(3);
        GameMechanics mechanics = new GameMechanics();

        board.setField(1, 1, Stone.WHITE);
        board.setField(0, 1, Stone.BLACK);
        board.setField(1, 0, Stone.BLACK);
        board.setField(2, 1, Stone.BLACK);

        boolean result = mechanics.IsMovePossible(board, 1, 2, Stone.BLACK);

        assertTrue(result);
        assertEquals(Stone.BLACK, board.getField(1, 2));
        assertEquals(Stone.EMPTY, board.getField(1, 1));
        assertEquals(1, mechanics.blackCaptures);
    }

    @Test
    void rejectsMoveOutOfBounds() {
        Board board = new Board(5);
        GameMechanics mechanics = new GameMechanics();

        boolean result = mechanics.IsMovePossible(board, -1, 2, Stone.BLACK);
        assertFalse(result);

        result = mechanics.IsMovePossible(board, 5, 2, Stone.BLACK);
        assertFalse(result);

        result = mechanics.IsMovePossible(board, 2, -1, Stone.WHITE);
        assertFalse(result);
    }

    @Test
    void whitePlayerCanCaptureBlackStones() {
        Board board = new Board(3);
        GameMechanics mechanics = new GameMechanics();

        board.setField(1, 1, Stone.BLACK);
        board.setField(0, 1, Stone.WHITE);
        board.setField(1, 0, Stone.WHITE);
        board.setField(2, 1, Stone.WHITE);

        boolean result = mechanics.IsMovePossible(board, 1, 2, Stone.WHITE);

        assertTrue(result);
        assertEquals(Stone.EMPTY, board.getField(1, 1));
        assertEquals(1, mechanics.whiteCaptures);
    }

    @Test
    void multipleStonesCapturedTogether() {
        Board board = new Board(5);
        GameMechanics mechanics = new GameMechanics();


        board.setField(2, 1, Stone.WHITE);
        board.setField(2, 2, Stone.WHITE);
        board.setField(2, 3, Stone.WHITE);

        board.setField(1, 1, Stone.BLACK);
        board.setField(1, 2, Stone.BLACK);
        board.setField(1, 3, Stone.BLACK);
        board.setField(3, 1, Stone.BLACK);
        board.setField(3, 2, Stone.BLACK);
        board.setField(3, 3, Stone.BLACK);
        board.setField(2, 0, Stone.BLACK);


        boolean result = mechanics.IsMovePossible(board, 2, 4, Stone.BLACK);

        assertTrue(result);
        assertEquals(3, mechanics.blackCaptures);
    }

    @Test
    void stoneWithLivertiesCanBePlaced() {
        Board board = new Board(5);
        GameMechanics mechanics = new GameMechanics();

        board.setField(1, 1, Stone.BLACK);
        board.setField(2, 1, Stone.BLACK);

        boolean result = mechanics.IsMovePossible(board, 3, 1, Stone.BLACK);

        assertTrue(result);
        assertEquals(Stone.BLACK, board.getField(3, 1));
    }

    @Test
    void capturesCounterIncrementsCorrectly() {
        Board board = new Board(5);
        GameMechanics mechanics = new GameMechanics();

        assertEquals(0, mechanics.blackCaptures);
        assertEquals(0, mechanics.whiteCaptures);

        board.setField(1, 1, Stone.BLACK);
        board.setField(0, 1, Stone.WHITE);
        board.setField(1, 0, Stone.WHITE);
        board.setField(2, 1, Stone.WHITE);

        mechanics.IsMovePossible(board, 1, 2, Stone.WHITE);

        assertEquals(1, mechanics.whiteCaptures);
    }

    @Test
    void exploreGroupFindsAllConnectedStones() {
        Board board = new Board(5);
        GameMechanics mechanics = new GameMechanics();

        board.setField(2, 2, Stone.BLACK);
        board.setField(2, 3, Stone.BLACK);
        board.setField(3, 2, Stone.BLACK);
        board.setField(4, 2, Stone.BLACK);

        var groupMembers = new java.util.HashSet<java.awt.Point>();
        var liberties = new java.util.HashSet<java.awt.Point>();

        mechanics.exploreGroup(board, new java.awt.Point(2, 2), Stone.BLACK, groupMembers, liberties);

        assertEquals(4, groupMembers.size());
    }
}
