package go.ui;

import java.awt.Point;
import java.util.Set;

import go.logic.Board;

/**
 * Interfejs definiujący kontrakt dla widoku gry (User Interface).
 * <p>
 * Pozwala na odseparowanie logiki klienta (klasa {@code GoClient}) od sposobu prezentacji danych.
 * Dzięki temu ta sama logika gry może współpracować zarówno z interfejsem tekstowym (konsola),
 * jak i graficznym (JavaFX), bez konieczności modyfikacji kodu sterującego.
 */
public interface GameView {

    /**
     * Wyświetla lub odświeża stan planszy na podstawie przekazanego modelu.
     *
     * @param board aktualny obiekt planszy zawierający układ kamieni.
     */
    void showBoard(Board board);

    /**
     * Prezentuje użytkownikowi komunikat systemowy (np. informację o turze, błędzie, wyniku).
     *
     * @param message treść komunikatu.
     */
    void showMessage(String message);

    /**
     * Wyświetla wiadomość czatu otrzymaną od przeciwnika lub systemu.
     *
     * @param author  nazwa autora wiadomości (np. "Przeciwnik").
     * @param message treść wiadomości.
     */
    void showChatMessage(String author, String message);

    /**
     * Pobiera akcję od użytkownika.
     * W zależności od implementacji może to być odczyt ze standardowego wejścia
     * lub pobranie zdarzenia kliknięcia/przycisku z kolejki zdarzeń GUI.
     *
     * @return ciąg znaków reprezentujący ruch (np. "A10") lub komendę (np. "pass", "done").
     */
    String getInput();

    /**
     * Wyróżnia wizualnie podany zbiór punktów na planszy.
     * Używane w fazie negocjacji do oznaczania martwych grup kamieni (np. czerwonymi krzyżykami).
     *
     * @param points zbiór współrzędnych do wyróżnienia.
     */
    void highlightGroups(Set<Point> points);

    /**
     * Przełącza tryb widoku między normalną rozgrywką a fazą negocjacji końcowej.
     * Powinno to skutkować zmianą widocznych przycisków (np. ukrycie "Pass", pokazanie "Done").
     *
     * @param active true, aby włączyć tryb negocjacji; false, aby wrócić do trybu gry.
     */
    void setNegotiationMode(boolean active);

    /**
     * Wyświetla okno dialogowe lub komunikat końcowy z wynikiem gry.
     * Zazwyczaj jest to ostatnia akcja przed zamknięciem aplikacji.
     *
     * @param title   tytuł okna/nagłówek (np. "WYGRAŁEŚ").
     * @param message szczegółowa treść z punktacją.
     */
    void showEndGameDialog(String title, String message);

    /**
     * Steruje aktywnością/widocznością przycisku "Accept" w fazie negocjacji.
     * Pozwala na wymuszenie na graczu wysłania nowej propozycji ("Done") po edycji planszy,
     * zanim będzie mógł zaakceptować układ.
     *
     * @param active true, aby przycisk był dostępny; false, aby go ukryć/zablokować.
     */
    void setAcceptButtonActive(boolean active);

    int askForGameMode();
}