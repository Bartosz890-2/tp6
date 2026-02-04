package go.si;

import go.logic.Board;
import go.logic.Stone;

import java.awt.*;

/**
 * Interfejs definiujący kontrakt dla strategii podejmowania decyzji przez bota.
 * <p>
 * Wykorzystanie tego interfejsu pozwala na implementację wzorca projektowego <b>Strategia</b>.
 * Dzięki temu serwer gry może współpracować z różnymi algorytmami sztucznej inteligencji
 * bez konieczności zmiany kodu obsługującego sesję gry.
 */
public interface BotStrategy {

    /**
     * Analizuje aktualną sytuację na planszy i oblicza najlepszy ruch dla danego koloru gracza.
     *
     * @param board aktualny stan planszy (układ kamieni).
     * @param color kolor kamieni, którymi gra bot (Stone.WHITE lub Stone.BLACK).
     * @return obiekt {@link Point} zawierający współrzędne (x, y) wybranego ruchu,
     * lub {@code null}, jeśli bot decyduje się spasować (brak opłacalnych ruchów).
     */
    public Point calculateBestMove(Board board, Stone color);
}