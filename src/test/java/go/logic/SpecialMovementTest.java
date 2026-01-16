package go.logic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class SpecialMovementTest {
    @Test
    public void testKOrule() {
        Board board = new Board(4);
        GameMechanics mechanics = new GameMechanics();

        // Ustawienie sytuacji KO:
        // . B W .
        // B W . W
        // . B W .

        board.setField(1, 0, Stone.BLACK);
        board.setField(0, 1, Stone.BLACK);
        board.setField(1, 2, Stone.BLACK);

        board.setField(2, 0, Stone.WHITE);
        board.setField(3, 1, Stone.WHITE);
        board.setField(2, 2, Stone.WHITE);

        board.setField(2, 1, Stone.BLACK); // Czarny do bicia

        // 1. Biały bije czarnego w (1,1)
        assertTrue(mechanics.IsMovePossible(board, 1, 1, Stone.WHITE));

        // 2. Czarny próbuje natychmiast odbić w (2,1) - powinno być zabronione przez KO
        assertFalse(mechanics.IsMovePossible(board, 2, 1, Stone.BLACK), "Zasada KO powinna zadziałać");
    }

    @Test
    public void checkIfSuicideIsNotAllowed() {
        Board board = new Board(4);
        GameMechanics mechanics = new GameMechanics();
        board.setField(1, 0, Stone.BLACK);
        board.setField(0, 1, Stone.BLACK);
        board.setField(1, 2, Stone.BLACK);
        board.setField(2, 1, Stone.BLACK);

        assertFalse(mechanics.IsMovePossible(board, 1, 1, Stone.WHITE), "Ruch samobojczy");
    }

    @Test
    public void checkNotSuicideMove() {
        Board board = new Board(5);
        GameMechanics mechanics = new GameMechanics();
        board.setField(1, 0, Stone.BLACK);
        board.setField(0, 1, Stone.BLACK);
        board.setField(1, 2, Stone.BLACK);
        board.setField(2, 1, Stone.BLACK);
        board.setField(0, 0, Stone.BLACK);

        board.setField(2, 0, Stone.WHITE);
        board.setField(0, 2, Stone.WHITE);
        assertTrue(mechanics.IsMovePossible(board, 1, 1, Stone.WHITE));
    }

    @Test
    public void checkProperCapturesInTheCorner() {
        Board board = new Board(5);
        GameMechanics mechanics = new GameMechanics();
        board.setField(0, 0, Stone.BLACK);
        board.setField(1, 0, Stone.WHITE);
        mechanics.IsMovePossible(board, 0, 1, Stone.WHITE);

        assertEquals(Stone.EMPTY, board.getField(0, 0));
    }
}
