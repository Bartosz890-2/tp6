package go.ui.fx;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/**
 * Panel boczny interfejsu graficznego (GUI).
 * <p>
 * Jest to kontener układu pionowego (VBox), który przechowuje:
 * <ul>
 * <li>Historię gry (logi systemowe).</li>
 * <li>Przyciski sterujące rozgrywką (Pass, Surrender, Quit).</li>
 * <li>Przyciski fazy negocjacji (Done, Accept) - domyślnie ukryte.</li>
 * <li>Okno czatu i pole do wpisywania wiadomości.</li>
 * </ul>
 * Klasa udostępnia gettery do elementów interaktywnych, aby kontroler (GUIView) mógł przypisać im akcje.
 */
public class SidePanel extends VBox {
    private final TextArea logArea=new TextArea();
    private final Button passBtn=new Button("Pass");
    private final Button surrenderBtn=new Button("Surrender");
    private final Button quitBtn = new Button("Quit");

    private final TextArea chatArea = new TextArea();
    private final TextField chatInput = new TextField();
    private final Button sendBtn = new Button("Send");

    private final Button doneBtn = new Button("Done");
    private final Button acceptBtn = new Button("Accept");
    private final Button prevBtn = new Button("< Cofnij");
    private final Button nextBtn = new Button("Dalej >");
    private final Button exitReplayBtn = new Button("Zamknij");

    /**
     * Konstruktor panelu bocznego.
     * Inicjalizuje wszystkie komponenty UI, ustawia ich style CSS,
     * konfiguruje układ (Layout) oraz domyślną widoczność przycisków.
     */
    public SidePanel(){
        // ustawienia panelu bocznego
        this.getStyleClass().add("side-panel");
        Label label = new Label("Historia Gry:");
        label.getStyleClass().add("header-label");

        logArea.getStyleClass().add("log-area");
        passBtn.getStyleClass().add("game-button");
        surrenderBtn.getStyleClass().add("game-button");
        quitBtn.getStyleClass().add("game-button");

        acceptBtn.getStyleClass().add("game-button");
        doneBtn.getStyleClass().add("game-button");

        prevBtn.getStyleClass().add("game-button");
        nextBtn.getStyleClass().add("game-button");
        exitReplayBtn.getStyleClass().add("game-button");
        // Przyciski negocjacji są domyślnie ukryte i nie zajmują miejsca w layout
        acceptBtn.setManaged(false);
        acceptBtn.setVisible(false);

        doneBtn.setManaged(false);
        doneBtn.setVisible(false);

        HBox buttonsBox = new HBox(10);
        buttonsBox.setAlignment(Pos.CENTER);
        buttonsBox.getChildren().addAll(passBtn, surrenderBtn, quitBtn, doneBtn, acceptBtn);

        HBox replayButtonsBox = new HBox(10);
        replayButtonsBox.setAlignment(Pos.CENTER);
        replayButtonsBox.getChildren().addAll(prevBtn, nextBtn, exitReplayBtn);
        toggleReplayMode(false);
        logArea.setEditable(false);
        logArea.setPrefHeight(250);
        logArea.setWrapText(true);


        Label chatLabel = new Label("Czat:");
        chatLabel.getStyleClass().add("header-label");

        chatArea.getStyleClass().add("chat-area");
        chatArea.setEditable(false);
        chatArea.setWrapText(true);

        HBox chatInputBox = new HBox(10);
        chatInputBox.setAlignment(Pos.CENTER);

        chatInput.setPromptText("Wpisz wiadomość...");
        chatInput.getStyleClass().add("chat-input");

        sendBtn.getStyleClass().add("game-button");

        chatInputBox.getChildren().addAll(chatInput, sendBtn);
        this.setSpacing(10);

        this.getChildren().addAll(label, logArea, buttonsBox, replayButtonsBox, chatLabel, chatArea, chatInputBox);
        this.setPrefWidth(350);
    }

    public void toggleReplayMode(boolean active){
        passBtn.setVisible(!active);
        passBtn.setManaged(!active);
        surrenderBtn.setVisible(!active);
        surrenderBtn.setManaged(!active);
        quitBtn.setVisible(!active);
        quitBtn.setManaged(!active);

        doneBtn.setVisible(false);
        doneBtn.setManaged(false);
        acceptBtn.setVisible(false);
        acceptBtn.setManaged(false);
        
        prevBtn.setVisible(active);
        prevBtn.setManaged(active);
        nextBtn.setVisible(active);
        nextBtn.setManaged(active);
        exitReplayBtn.setVisible(active);
        exitReplayBtn.setManaged(active);
    }
    public Button getPrevBtn() { return prevBtn; }
    public Button getNextBtn() { return nextBtn; }
    public Button getExitReplayBtn() { return exitReplayBtn; }
    /**
     * Dodaje nową wiadomość do obszaru logów (historii gry).
     * Automatycznie przewija widok do najnowszej wiadomości.
     *
     * @param message treść komunikatu do wyświetlenia.
     */
    public void addLog(String message){
        logArea.appendText(message + "\n");
        logArea.setScrollTop(Double.MAX_VALUE);
    }

    /**
     * Zwraca przycisk "Pass".
     * @return obiekt przycisku.
     */
    public Button getPassBtn(){
        return passBtn;
    }

    /**
     * Zwraca przycisk "Surrender" (Poddanie się).
     * @return obiekt przycisku.
     */
    public Button getSurrenderBtn(){
        return surrenderBtn;
    }

    /**
     * Zwraca przycisk "Quit" (Wyjście z gry).
     * @return obiekt przycisku.
     */
    public Button getQuitBtn() {
        return quitBtn;
    }

    /**
     * Zwraca przycisk "Accept" (Akceptacja propozycji w fazie negocjacji).
     * @return obiekt przycisku.
     */
    public Button getAcceptBtn() {
        return acceptBtn;
    }

    /**
     * Zwraca przycisk "Done" (Zatwierdzenie zaznaczeń w fazie negocjacji).
     * @return obiekt przycisku.
     */
    public Button getDoneBtn() {
        return doneBtn;
    }

    /**
     * Zwraca przycisk "Send" (Wysłanie wiadomości czatu).
     * @return obiekt przycisku.
     */
    public Button getSendBtn(){
        return sendBtn;
    }

    /**
     * Zwraca pole tekstowe do wpisywania wiadomości czatu.
     * @return obiekt pola tekstowego.
     */
    public TextField getChatInput(){
        return chatInput;
    }

    /**
     * Zwraca obszar tekstowy wyświetlający historię czatu.
     * @return obiekt obszaru tekstowego.
     */
    public TextArea getChatArea(){
        return chatArea;
    }
}