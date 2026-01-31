package go.ui.fx;

import java.awt.Point;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;

import go.logic.Board;
import go.ui.GameView;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.input.KeyCode;

/**
 * Implementacja interfejsu widoku gry (GameView) oparta na bibliotece JavaFX.
 * <p>
 * Klasa ta pełni rolę mostu między logiką gry (działającą w osobnym wątku) a interfejsem graficznym (wątek JavaFX Application Thread).
 * Wykorzystuje kolejki blokujące ({@link LinkedBlockingQueue}) do synchronizacji wejścia użytkownika (kliknięcia, komendy)
 * z oczekującą logiką klienta.
 */
public class GUIView implements GameView{
    private final BoardCanvas canvas;
    private final SidePanel sidePanel;

    /** Kolejka przechowująca ruchy i komendy gry (np. "A15", "pass", "done"). */
    private final LinkedBlockingQueue<String> inputQueue = new LinkedBlockingQueue<>();

    /** Kolejka przechowująca wiadomości czatu wpisane przez użytkownika. */
    private final LinkedBlockingQueue<String> chatQueue = new LinkedBlockingQueue<>();

    /** Flaga określająca, czy widok powinien reagować na kliknięcia myszką na planszy. */
    private volatile boolean waitingForInput = false;

    /**
     * Konstruktor widoku GUI.
     * Inicjalizuje komponenty i konfiguruje obsługę zdarzeń (kliknięcia, przyciski).
     *
     * @param canvas    komponent płótna odpowiedzialny za rysowanie planszy.
     * @param sidePanel komponent panelu bocznego (logi, przyciski, czat).
     */
    public GUIView(BoardCanvas canvas, SidePanel sidePanel)
    {
        this.canvas = canvas;
        this.sidePanel = sidePanel;
        setupEvents();
    }

    /**
     * Przekazuje do płótna (canvas) listę punktów do wyróżnienia (np. martwe grupy).
     * Wywołanie jest opakowane w {@link Platform#runLater} dla bezpieczeństwa wątków.
     *
     * @param points zbiór punktów do podświetlenia.
     */
    public void highlightGroups(Set<Point> points){
        Platform.runLater( ()-> canvas.setHighlightedPoints(points));
    }

    /**
     * Przełącza widoczność przycisków w panelu bocznym w zależności od fazy gry.
     * W trybie negocjacji ukrywa przyciski "Pass/Surrender" a pokazuje "Done/Accept".
     *
     * @param active true włącza tryb negocjacji (przyciski Done/Accept), false przywraca tryb gry.
     */
    @Override
    public void setNegotiationMode(boolean active) {
        Platform.runLater(() -> {
            // Jeśli active == true, chowamy Pass/Surrender, pokazujemy Done/Accept

            sidePanel.getPassBtn().setVisible(!active);
            sidePanel.getPassBtn().setManaged(!active);

            sidePanel.getSurrenderBtn().setVisible(!active);
            sidePanel.getSurrenderBtn().setManaged(!active);

            sidePanel.getDoneBtn().setVisible(active);
            sidePanel.getDoneBtn().setManaged(active);

            sidePanel.getAcceptBtn().setVisible(active);
            sidePanel.getAcceptBtn().setManaged(active);
        });
    }

    /**
     * Konfiguruje listenery (słuchacze) zdarzeń dla elementów interfejsu.
     * Obsługuje:
     * <ul>
     * <li>Kliknięcia na planszy (konwersja pikseli na koordynaty np. "C4").</li>
     * <li>Przyciski Pass, Surrender, Done, Accept.</li>
     * <li>Wysyłanie czatu (przycisk i Enter).</li>
     * </ul>
     */
    private void setupEvents(){
        // obsluga klikniec na planszy
        canvas.setOnMouseClicked( e->{
            if(!waitingForInput) return; // ignoruj klikniecia, jesli nie czekamy na wejscie
            int x=canvas.toBoardCoord(e.getX());
            int y=canvas.toBoardCoord(e.getY());
            // konwersja na notacje literowo-cyfrowa
            char letter =(char)('A'+x);
            int number = y+1;
            inputQueue.offer(""+letter+number);
        });
        // obsluga przyciskow panelu bocznego
        sidePanel.getPassBtn().setOnAction( e-> inputQueue.offer("pass"));
        sidePanel.getSurrenderBtn().setOnAction( e-> inputQueue.offer("surrender"));
        // obsługa wysyłania wiadomości czatu
        sidePanel.getSendBtn().setOnAction( e-> {
            String message = sidePanel.getChatInput().getText().trim();
            if(!message.isEmpty()){
                chatQueue.offer(message);
                sidePanel.getChatInput().clear();
            }
        });
        // obsługa wysyłania wiadomości klawiszem Enter
        sidePanel.getChatInput().setOnKeyPressed( e-> {
            if(e.getCode() == KeyCode.ENTER){
                String message = sidePanel.getChatInput().getText().trim();
                if(!message.isEmpty()){
                    chatQueue.offer(message);
                    sidePanel.getChatInput().clear();
                }
            }
        });

        sidePanel.getDoneBtn().setOnAction(e -> inputQueue.offer("done"));

        sidePanel.getAcceptBtn().setOnAction(e -> inputQueue.offer("accept"));
    }

    // --- Metody interfejsu GameView ---

    /**
     * Przerysowuje planszę na podstawie przekazanego modelu.
     *
     * @param board aktualny stan planszy.
     */
    @Override
    public void showBoard(Board board){
        Platform.runLater( ()-> canvas.draw(board) );
    }

    /**
     * Wyświetla komunikat systemowy w logu gry (panel boczny).
     *
     * @param message treść komunikatu.
     */
    @Override
    public void showMessage(String message){
        Platform.runLater( ()-> sidePanel.addLog(message) );
    }

    /**
     * Wyświetla wiadomość czatu w oknie rozmowy.
     *
     * @param author  autor wiadomości (np. "Ty", "Przeciwnik").
     * @param message treść wiadomości.
     */
    @Override
    public void showChatMessage(String author, String message){
        Platform.runLater( ()-> {
            sidePanel.getChatArea().appendText("[" + author + "]: " + message + "\n");
            sidePanel.getChatArea().setScrollTop(Double.MAX_VALUE);
        });
    }

    /**
     * Blokuje wątek wywołujący do momentu otrzymania wejścia od użytkownika.
     * Wejściem może być współrzędna ruchu (kliknięcie) lub komenda z przycisku.
     *
     * @return ciąg znaków reprezentujący ruch (np. "A10") lub komendę ("pass", "done").
     */
    @Override
    public String getInput(){
        //wyczyszczenie klikniec z poprzednich tur
        inputQueue.clear();
        waitingForInput = true;
        // pobranie wejscia z kolejki (operacja blokująca)
        try {
            String input = inputQueue.take();
            waitingForInput = false;
            return input;
        } catch (InterruptedException e) {
            return "quit";
        }
        finally {
            waitingForInput = false;
        }
    }

    /**
     * Pobiera wiadomość z kolejki czatu bez blokowania (zwraca null jeśli pusta).
     *
     * @return wiadomość lub null.
     */
    public String getChatMessage() {
        return chatQueue.poll();
    }

    /**
     * Oczekuje (blokuje wątek) na wpisanie wiadomości czatu przez użytkownika.
     * Używane przez wątek obsługi czatu w GoClient.
     *
     * @return treść wiadomości.
     * @throws InterruptedException w przypadku przerwania wątku.
     */
    public String waitForChatMessage() throws InterruptedException {
        return chatQueue.take();
    }

    /**
     * Wyświetla okno dialogowe z informacją o końcu gry i zamyka aplikację po jego potwierdzeniu.
     *
     * @param title   tytuł okna (np. "Wygrałeś!").
     * @param message treść z wynikami punktowymi.
     */
    public void showEndGameDialog(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Koniec Gry");
            alert.setHeaderText(title);
            alert.setContentText(message);
            alert.showAndWait();
            System.exit(0); // Zamknij aplikację po zamknięciu dialogu
        });
    }

    /**
     * Steruje widocznością samego przycisku "Accept".
     * Używane w fazie negocjacji, aby ukryć przycisk, gdy gracz edytuje planszę (i musi wysłać "Done"),
     * a pokazać go, gdy otrzymał propozycję od rywala.
     *
     * @param active true, aby pokazać przycisk; false, aby ukryć.
     */
    @Override
    public void setAcceptButtonActive(boolean active) {
        Platform.runLater(() -> {
            sidePanel.getAcceptBtn().setVisible(active);
            sidePanel.getAcceptBtn().setManaged(active);
        });
    }

@Override
    public int askForGameMode() {
        FutureTask<Integer> query = new FutureTask<>(() -> {
            List<String> choices = Arrays.asList("Gra z Botem", "Gra Online (PvP)", "Historia Gier (Replay)");
            ChoiceDialog<String> dialog = new ChoiceDialog<>("Gra z Botem", choices);
            dialog.setTitle("Wybór trybu gry");
            dialog.setHeaderText("Witaj w Go!");
            dialog.setContentText("Wybierz tryb:");

            Optional<String> result = dialog.showAndWait();

            if (result.isPresent()) {
                String choice = result.get();
                if (choice.equals("Gra Online (PvP)")) return 2;
                if (choice.equals("Historia Gier (Replay)")) {
                    new GameListWindow().show(); 
                    return -1; 
                }
                return 1;
            }
            return 0;
        });
        Platform.runLater(query);
        try {
            return query.get();
        } catch (Exception e) {
            e.printStackTrace();
            return 1;
        }
    }
}