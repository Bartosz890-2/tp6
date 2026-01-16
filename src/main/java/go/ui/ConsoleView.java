package go.ui;

import java.awt.Point;
import java.util.Scanner;
import java.util.Set;

import go.logic.Board;
import go.logic.Stone;

/**
 * Implementacja interfejsu widoku gry (GameView) oparta na konsoli tekstowej (CLI).
 * <p>
 * Służy do uruchamiania klienta gry w środowisku bez interfejsu graficznego.
 * Plansza jest rysowana za pomocą znaków ASCII, a interakcja odbywa się poprzez
 * standardowe wejście (System.in) i wyjście (System.out).
 */
public class ConsoleView implements GameView {
    private final Scanner scanner;

    /**
     * Konstruktor widoku konsolowego.
     * Inicjalizuje skaner do odczytu poleceń użytkownika.
     */
    public ConsoleView() {
        this.scanner = new Scanner(System.in);
    }

    /**
     * Metoda pusta w implementacji konsolowej.
     * W trybie tekstowym nie obsługujemy dynamicznego podświetlania grup kamieni (np. na czerwono).
     *
     * @param points zbiór punktów do wyróżnienia (ignorowany).
     */
    @Override
    public void highlightGroups(Set<Point> points) {
        // Brak implementacji w widoku tekstowym
    }

    /**
     * Metoda pusta w implementacji konsolowej.
     * W konsoli nie ma przycisków interfejsu, więc nie ma potrzeby ich ukrywania/pokazywania.
     *
     * @param active true włącza tryb negocjacji.
     */
    @Override
    public void setNegotiationMode(boolean active) {
        // Brak implementacji w widoku tekstowym
    }

    /**
     * Rysuje tekstową reprezentację planszy na standardowym wyjściu.
     * <p>
     * Legenda:
     * <ul>
     * <li>. - puste pole</li>
     * <li>X - czarny kamień</li>
     * <li>O - biały kamień</li>
     * </ul>
     * Wyświetla również współrzędne kolumn (A-S) i wierszy (1-19).
     *
     * @param board aktualny stan planszy.
     */
    @Override
    public void showBoard(Board board) {
        int size = board.getSize();

        System.out.println("  A B C D E F G H I J K L M N O P Q R S");

        for (int y = 0; y < size; y++) {
            // Wyrównanie numerów wierszy (dodatkowa spacja dla jednocyfrowych)
            System.out.printf("%2d ", (y + 1));
            for (int x = 0; x < size; x++) {
                if (board.getField(x, y) == Stone.EMPTY) System.out.print(". ");
                else if (board.getField(x, y) == Stone.WHITE) System.out.print("O ");
                else if (board.getField(x, y) == Stone.BLACK) System.out.print("X ");
            }
            System.out.println(); // Nowa linia po każdym wierszu planszy
        }
    }

    /**
     * Wypisuje komunikat systemowy na konsolę.
     *
     * @param message treść komunikatu.
     */
    @Override
    public void showMessage(String message) {
        System.out.println(message);
    }

    /**
     * Wypisuje wiadomość czatu w formacie "[Autor]: Treść".
     *
     * @param author  autor wiadomości.
     * @param message treść wiadomości.
     */
    @Override
    public void showChatMessage(String author, String message) {
        System.out.println("[" + author + "]: " + message);
    }

    /**
     * Pobiera linię tekstu wpisaną przez użytkownika.
     * Jest to operacja blokująca - czeka na wciśnięcie Enter.
     *
     * @return wpisany ciąg znaków (bez białych znaków na końcach).
     */
    @Override
    public String getInput() {
        return scanner.nextLine().trim();
    }

    /**
     * Wyświetla sformatowany blok tekstu informujący o zakończeniu gry i wynikach.
     *
     * @param title   tytuł (np. "WYGRAŁEŚ!").
     * @param message szczegóły punktacji.
     */
    @Override
    public void showEndGameDialog(String title, String message) {
        System.out.println("============================");
        System.out.println("KONIEC GRY: " + title);
        System.out.println(message);
        System.out.println("============================");
    }

    /**
     * Metoda pusta w implementacji konsolowej.
     * W konsoli nie ma interaktywnych przycisków do aktywacji/dezaktywacji.
     *
     * @param active true, aby pokazać przycisk; false, aby ukryć.
     */
    @Override
    public void setAcceptButtonActive(boolean active) {
        // Brak implementacji w widoku tekstowym
    }
}