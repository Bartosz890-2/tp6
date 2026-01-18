package go.logic;

import java.awt.*;
import java.util.Arrays;
import java.util.Objects;

/**
 * Klasa reprezentująca planszę do gry w Go.
 * Przechowuje aktualny stan rozgrywki (układ kamieni) oraz udostępnia metody
 * do manipulacji tym stanem, kopiowania planszy i porównywania jej z innymi (np. dla reguły Ko).
 */
public class Board {
    /**
     * Rozmiar boku planszy (np. 19 dla planszy 19x19).
     */
    private final int size;

    private Point lastMove = new Point(-1, -1);
    /**
     * Dwuwymiarowa tablica przechowująca kamienie (lub puste pola) na planszy.
     * Pole publiczne finalne dla szybkiego dostępu w logice gry,
     * chociaż modyfikacja zawartości tablicy jest możliwa.
     */
    public final Stone[][] fields;

    /**
     * Tworzy nową planszę o zadanym rozmiarze.
     * Inicjalizuje wszystkie pola wartością {@link Stone#EMPTY}.
     *
     * @param size rozmiar planszy (np. 9, 13, 19).
     */
    public Board(int size) {
        this.size = size;
        this.fields = new Stone[size][size];

        for (int i = 0; i<size; i++) {
            for (int j = 0; j < size; j++) {
                fields[i][j] = Stone.EMPTY;
            }
        }
    }

    /**
     * Zwraca rozmiar planszy.
     *
     * @return długość boku planszy.
     */
    public int getSize() {
        return size;
    }

    /**
     * Ustawia kamień (lub puste pole) na wskazanych współrzędnych.
     *
     * @param wspX współrzędna X (kolumna).
     * @param wspY współrzędna Y (wiersz).
     * @param stone wartość do wstawienia (BLACK, WHITE, EMPTY).
     * @throws IllegalArgumentException jeśli współrzędne wykraczają poza planszę.
     */
    public void setField(int wspX, int wspY, Stone stone) {
        if (!isFieldOnBoard(wspX, wspY)) {
            throw new IllegalArgumentException("Podane pole nie nalezy do planszy!");
        }
        fields[wspX][wspY] = stone;

        if (stone != Stone.EMPTY) {
            setLastMove(wspX, wspY);
        }
    }

    /**
     * Pobiera wartość pola (kolor kamienia) ze wskazanych współrzędnych.
     *
     * @param wspX współrzędna X (kolumna).
     * @param wspY współrzędna Y (wiersz).
     * @return stan pola (BLACK, WHITE lub EMPTY).
     * @throws IllegalArgumentException jeśli współrzędne wykraczają poza planszę.
     */
    public Stone getField(int wspX, int wspY) {
        if (!isFieldOnBoard(wspX, wspY)) {
            throw new IllegalArgumentException("Podane pole nie nalezy do planszy!");
        }
        return fields[wspX][wspY];
    }

    /**
     * Sprawdza, czy podane współrzędne mieszczą się w granicach planszy.
     *
     * @param x współrzędna X.
     * @param y współrzędna Y.
     * @return true, jeśli pole należy do planszy; false w przeciwnym razie.
     */
    public boolean isFieldOnBoard(int x, int y) {
        return x >= 0 && x < size && y >= 0 && y < size;
    }

    /**
     * Porównuje ten obiekt planszy z innym obiektem.
     * Sprawdza głęboką równość, tzn. czy układ kamieni na obu planszach jest identyczny.
     * Używane m.in. do wykrywania powtórzeń pozycji (reguła Ko).
     *
     * @param o obiekt do porównania.
     * @return true, jeśli obiekty reprezentują identyczny stan gry.
     */
    @Override
    public boolean equals(Object o) {
        // 1. Sprawdzenie czy to ten sam obiekt w pamięci
        if (this == o) return true;

        // 2. Sprawdzenie czy obiekt nie jest nullem i czy jest tą samą klasą
        if (o == null || getClass() != o.getClass()) return false;

        Board board = (Board) o;

        // 4. Porównanie rozmiaru (szybkie sprawdzenie)
        if (size != board.size) return false;

        // 5. KLUCZOWE: Porównanie zawartości tablicy dwuwymiarowej
        return java.util.Arrays.deepEquals(this.fields, board.fields);
    }

    /**
     * Generuje unikalny kod skrótu (hash) dla stanu planszy.
     * Konieczne dla poprawnego działania w kolekcjach typu HashSet/HashMap.
     *
     * @return kod hash reprezentujący stan planszy.
     */
    @Override
    public int hashCode() {
        // Generujemy unikalny skrót na podstawie rozmiaru i zawartości tablicy
        int result = java.util.Objects.hash(size);
        result = 31 * result + java.util.Arrays.deepHashCode(fields);
        return result;
    }

    /**
     * Kopiuje stan (układ kamieni) z bieżącej planszy do innej instancji planszy.
     * Przydatne przy symulacjach ruchów (np. dla bota lub sprawdzania legalności ruchu).
     *
     * @param destinationBoard plansza docelowa, do której zostanie skopiowany stan.
     */
    public void copyBoard(Board destinationBoard) {
        int minSize = Math.min(this.size, destinationBoard.size);
        for (int i = 0; i < minSize; i++) {
            for (int j = 0; j < minSize; j++) {
                destinationBoard.fields[i][j] = this.fields[i][j];
            }
        }

        destinationBoard.setLastMove(lastMove.x, lastMove.y);
    }

    public void setLastMove(int x, int y) {
        lastMove.x = x;
        lastMove.y = y;
    }

    public Point getLastMove() {
        return lastMove;
    }
}