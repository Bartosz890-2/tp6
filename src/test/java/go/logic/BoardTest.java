package go.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

class BoardTest {
    // Test metody ustawiającej i pobierającej wartość pola na planszy
    @Test
    void setFieldOutOfBoundsThrows() {
        Board board = new Board(3);
        assertThrows(IllegalArgumentException.class, () -> board.setField(-1, 0, Stone.WHITE));
        assertThrows(IllegalArgumentException.class, () -> board.getField(3, 3));
    }
    // Test czy ustawienie pola poza granicami planszy rzuca wyjątek
    @Test
    void boardInitializationAllEmpty() {
        Board board = new Board(5);
        for (int x = 0; x < 5; x++) {
            for (int y = 0; y < 5; y++) {
                assertEquals(Stone.EMPTY, board.getField(x, y));
            }
        }
    }
    // Test czy plansza jest poprawnie inicjalizowana z wszystkimi polami jako puste
    @Test
    void isFieldOnBoardCorrectlyIdentifiesBoundaries() {
        Board board = new Board(5);
        assertTrue(board.isFieldOnBoard(0, 0));
        assertTrue(board.isFieldOnBoard(4, 4));
        assertTrue(board.isFieldOnBoard(2, 3));
        assertFalse(board.isFieldOnBoard(-1, 0));
        assertFalse(board.isFieldOnBoard(5, 0));
        assertFalse(board.isFieldOnBoard(0, -1));
        assertFalse(board.isFieldOnBoard(0, 5));
    }
    // Test metody sprawdzającej, czy dane pole należy do planszy
    @Test
    void copyBoardCorrectlyDuplicatesState() {
        Board source = new Board(5);
        Board destination = new Board(5);
        
        source.setField(1, 1, Stone.BLACK);
        source.setField(2, 2, Stone.WHITE);
        
        source.copyBoard(destination);
        
        assertEquals(Stone.BLACK, destination.getField(1, 1));
        assertEquals(Stone.WHITE, destination.getField(2, 2));
        assertEquals(Stone.EMPTY, destination.getField(3, 3));
    }

    @Test
    void boardEqualsComparesContentCorrectly() {
        Board board1 = new Board(5);
        Board board2 = new Board(5);
        
        assertTrue(board1.equals(board2));
        
        board1.setField(2, 2, Stone.BLACK);
        assertFalse(board1.equals(board2));
        
        board2.setField(2, 2, Stone.BLACK);
        assertTrue(board1.equals(board2));
    }
    // Test metody porównującej dwie plansze pod kątem równości zawartości
    @Test
    void getSizeReturnsCorrectBoardSize() {
        Board board5 = new Board(5);
        Board board19 = new Board(19);
        
        assertEquals(5, board5.getSize());
        assertEquals(19, board19.getSize());
    }
// Test metody zwracającej rozmiar planszy
    @Test
    void setMultipleFieldsAndVerifyEach() {
        Board board = new Board(5);
        Stone[] colors = {Stone.BLACK, Stone.WHITE, Stone.EMPTY, Stone.BLACK};
        
        for (int i = 0; i < colors.length; i++) {
            board.setField(i, 0, colors[i]);
        }
        
        for (int i = 0; i < colors.length; i++) {
            assertEquals(colors[i], board.getField(i, 0));
        }
    }
// Test ustawiania wielu pól i weryfikacji ich wartości
    @Test
    void overwriteFieldWithDifferentColor() {
        Board board = new Board(5);
        board.setField(2, 2, Stone.BLACK);
        assertEquals(Stone.BLACK, board.getField(2, 2));
        
        board.setField(2, 2, Stone.WHITE);
        assertEquals(Stone.WHITE, board.getField(2, 2));
    }
}
