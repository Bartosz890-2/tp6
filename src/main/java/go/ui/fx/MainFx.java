package go.ui.fx;

import go.client.GoClient;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

/**
 * Główna klasa startowa aplikacji klienckiej opartej na bibliotece JavaFX.
 * <p>
 * Odpowiada za:
 * <ul>
 * <li>Inicjalizację głównego okna (Stage) i sceny.</li>
 * <li>Utworzenie komponentów widoku: {@link BoardCanvas} (plansza) oraz {@link SidePanel} (panel boczny).</li>
 * <li>Połączenie widoku z logiką poprzez {@link GUIView}.</li>
 * <li>Uruchomienie logiki klienta ({@link GoClient}) w osobnym wątku, aby operacje sieciowe nie blokowały interfejsu graficznego.</li>
 * </ul>
 */
public class MainFx extends Application{

    /**
     * Metoda startowa cyklu życia aplikacji JavaFX.
     * Buduje interfejs graficzny, konfiguruje układ (Layout) oraz uruchamia wątek sieciowy klienta.
     *
     * @param stage główne okno aplikacji (dostarczane przez framework JavaFX).
     */
    @Override
    public void start(Stage stage){
        // tworzenie komponentow GUI
        BoardCanvas canvas=new BoardCanvas();
        canvas.setWidth(700);
        canvas.setHeight(700);

        SidePanel side=new SidePanel();

        // Wstrzykiwanie zależności (Dependency Injection) do widoku
        GUIView guiView=new GUIView(canvas, side);

        // uruchomienie klienta w osobnym watku
        // Jest to krytyczne, ponieważ GoClient.connect() i playGame() są operacjami blokującymi.
        // Gdyby uruchomiono je w głównym wątku, okno aplikacji "zamroziłoby się".
        Thread clientThread=new Thread( ()-> {
            GoClient client=new GoClient(guiView);
            client.connect();
        });

        // ustawienie watku tak, by zamykal sie automatycznie wraz z zamknieciem okna aplikacji
        clientThread.setDaemon(true);
        clientThread.start();

        BorderPane root=new BorderPane();
        // rozmieszczenie komponentow w oknie
        root.setCenter(canvas);
        root.setRight(side);

        Scene scene = new Scene(root);
        // Ładowanie stylów CSS (upewnij się, że plik style.css znajduje się w resources)
        scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());

        stage.setScene(scene);
        stage.setTitle("Gra w Go");
        stage.setResizable(false); // Blokada zmiany rozmiaru okna dla uproszczenia rysowania
        stage.show();
    }
}