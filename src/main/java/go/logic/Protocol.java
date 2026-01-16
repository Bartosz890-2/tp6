package go.logic;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Klasa narzędziowa definiująca protokół komunikacji sieciowej gry.
 * Zawiera stałe identyfikujące typy wiadomości przesyłanych między klientem a serwerem
 * oraz statyczne metody pomocnicze do serializacji i deserializacji stanu planszy.
 */
public class Protocol {
    /** Domyślny port serwera gry. */
    public static final int Port=8000;

    /** Identyfikator pierwszego gracza (zazwyczaj Czarnego). */
    public static final int Player1=1;

    /** Identyfikator drugiego gracza (zazwyczaj Białego). */
    public static final int Player2=2;

    // --- Typy Komunikatów (Nagłówki) ---

    /** Sygnał wykonania ruchu (postawienia kamienia). */
    public static final int MOVE=10;

    /** Sygnał spasowania (oddania tury bez ruchu). */
    public static final int PASS=11;

    /** Sygnał poddania partii. */
    public static final int SURRENDER=12;

    /** Sygnał wyjścia z aplikacji/zerwania połączenia. */
    public static final int QUIT=13;

    /** Sygnał zakończenia rozgrywki (np. po obu pasach i negocjacjach). */
    public static final int GAME_OVER=14;

    /** Informacja zwrotna od serwera o błędnym ruchu (np. pole zajęte, samobójstwo). */
    public static final int INVALID_MOVE=15;

    /** Nagłówek przesyłania pełnego stanu planszy (tablicy kamieni). */
    public static final int BOARD_STATE=16;

    /** Nagłówek przesyłania liczby jeńców (zbitych kamieni). */
    public static final int CAPTURES=17;

    /** Nagłówek wiadomości tekstowej (czat). */
    public static final int MESSAGE=18;

    /** Sygnał rozpoczęcia fazy oznaczania martwych grup (po dwóch pasach). */
    public static final int START_MARKING=19;

    /** Nagłówek wysyłania propozycji martwych kamieni. */
    public static final int SEND_PROPOSAL = 20;

    /** Nagłówek otrzymania propozycji martwych kamieni od przeciwnika. */
    public static final int RECEIVE_PROPOSAL = 21;

    /** Sygnał akceptacji propozycji martwych kamieni (koniec gry). */
    public static final int ACCEPT_PROPOSAL = 22;

    /**
     * Wysyła pełny stan planszy przez strumień danych.
     * Najpierw wysyła rozmiar planszy, a następnie iteruje po wszystkich polach,
     * wysyłając wartość liczbową (ordinal) kamienia na każdym polu.
     *
     * @param board obiekt planszy do wysłania.
     * @param out   strumień wyjściowy (do serwera lub klienta).
     * @throws IOException w przypadku błędu zapisu do strumienia.
     */
    public static void sendBoard(Board board, DataOutputStream out) throws IOException {
        out.writeInt(board.getSize());

        for (int x = 0; x < board.getSize(); x++) {
            for (int y = 0; y < board.getSize(); y++) {
                Stone stone = board.getField(x, y);
                out.writeInt(stone.ordinal());
            }
        }
    }

    /**
     * Odbiera pełny stan planszy ze strumienia danych i aktualizuje przekazany obiekt Board.
     * Odczytuje rozmiar (choć zakłada zgodność z istniejącą planszą) i wartości
     * wszystkich pól, ustawiając odpowiednie kolory kamieni.
     *
     * @param board obiekt planszy do zaktualizowania (musi być zainicjalizowany).
     * @param in    strumień wejściowy (od serwera lub klienta).
     * @throws IOException w przypadku błędu odczytu ze strumienia.
     */
    public static void receiveBoard(Board board, DataInputStream in) throws IOException {
        int size = in.readInt();

        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                int ordinal = in.readInt();
                Stone color = Stone.values()[ordinal];
                board.setField(x, y, color);
            }
        }
    }
}