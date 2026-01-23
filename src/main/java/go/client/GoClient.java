package go.client;

import java.awt.Point;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import go.logic.Board;
import go.logic.GameMechanics;
import go.logic.Protocol;
import go.logic.Stone;
import go.ui.GameView;

/**
 * Główna klasa kontrolera klienta gry Go.
 * Zarządza logiką rozgrywki po stronie klienta, komunikacją między warstwą sieciową
 * a interfejsem użytkownika (UI) oraz przechowuje stan gry.
 */
public class GoClient {
    private final Board board = new Board(19);
    private final GameView gameView;
    private int blackCaptures = 0;
    private int whiteCaptures = 0;
    NetworkConnection network = new NetworkConnection();
    Stone myColor;

    private final GameMechanics mechanics = new GameMechanics();
    private boolean iPassed = false;
    private boolean waitingMessageShown=false;

    /**
     * Konstruktor klienta gry.
     *
     * @param gameView widok gry (interfejs), z którym klient będzie się komunikować.
     */
    public GoClient(GameView gameView){
        this.gameView = gameView;
    }

    /**
     * Nawiązuje połączenie z serwerem, ustala tożsamość gracza (kolor)
     * i uruchamia główną pętlę gry.
     */
    /**
     * Nawiązuje połączenie z serwerem.
     * PYTA UŻYTKOWNIKA O TRYB GRY (Bot vs Human),
     * wysyła wybór do serwera, ustala tożsamość i startuje grę.
     */
    public void connect(){
        try {
            // 1. Zapytaj użytkownika o tryb gry PRZED połączeniem
            // (Musisz dodać tę metodę do interfejsu GameView!)
            // Zwraca: 1 = Bot, 2 = Multiplayer
            int selectedMode = gameView.askForGameMode();

            gameView.showMessage("Łączenie z serwerem...");
            network.connect(); // Fizyczne połączenie socketu

            // 2. Wyślij wybór do serwera (Handshake)
            // (Musisz dodać tę metodę do NetworkConnection!)
            network.sendGameMode(selectedMode);

            // 3. Dalej po staremu - odbieramy ID przydzielone przez serwer
            int playerId = network.getPlayerId();
            gameView.showMessage("Połączono jako gracz " + playerId);

            if(playerId == Protocol.Player1){
                // Jeśli gramy z Botem, to "wait" trwa ułamek sekundy, bo bot jest gotowy od razu
                gameView.showMessage("Czekanie na przeciwnika...");

                // W trybie BOTnetwork.waitForSecondPlayer() może od razu zwrócić true,
                // w trybie HUMAN czeka na drugiego człowieka.
                network.waitForSecondPlayer();

                myColor = Stone.BLACK;
                gameView.showMessage("Przeciwnik gotowy. Rozpoczynam grę (Jesteś CZARNYM).");
            }
            else{
                myColor = Stone.WHITE;
                gameView.showMessage("Dołączyłeś do gry. Rozpoczynam grę (Jesteś BIAŁYM).");
            }

            startChatThread();
            playGame();
        } catch (IOException e) {
            gameView.showMessage("Błąd połączenia z serwerem: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Uruchamia osobny wątek do obsługi przychodzących i wychodzących wiadomości czatu.
     * Działa tylko, jeśli widok obsługuje GUI (GUIView).
     */
    private void startChatThread() {
        // Wątek do obsługi wiadomości z czatu (tylko dla GUIView)
        if (gameView instanceof go.ui.fx.GUIView guiView) {
            Thread chatThread = new Thread(() -> {
                try {
                    while (true) {
                        String message = guiView.waitForChatMessage();
                        if (message != null && !message.isEmpty()) {
                            try {
                                network.sendMessage(message);
                                gameView.showChatMessage("Ty: ", message);
                            } catch (IOException e) {
                                gameView.showMessage("Błąd podczas wysyłania wiadomości");
                            }
                        }
                    }
                } catch (InterruptedException e) {
                }
            });
            chatThread.setDaemon(true);
            chatThread.start();
        }
    }

    /**
     * Główna pętla rozgrywki (faza stawiania kamieni).
     * Obsługuje ruchy gracza, pasowanie, poddanie się oraz odbieranie ruchów przeciwnika.
     *
     * @throws IOException w przypadku błędu komunikacji z serwerem.
     */
    private void playGame() throws IOException {
        Stone currentTurn = Stone.BLACK;
        while(true){
            if(currentTurn == myColor){
                waitingMessageShown=false;
                // Aktualizacja stanu planszy i liczby jeńców
                gameView.showMessage("Ilość jeńców - Gracz1: " + blackCaptures + " Gracz2: " + whiteCaptures);
                gameView.showBoard(board);

                gameView.showMessage("Twój ruch. Kliknij na odpowiednie pole lub użyj przycisku");
                String input = gameView.getInput();

                if(input.startsWith("/msg ")) {
                    // wysłanie wiadomości do przeciwnika
                    String message = input.substring(5);
                    network.sendMessage(message);
                    gameView.showChatMessage("Ty", message);
                }
                else if(input.equalsIgnoreCase("pass")){
                    // Wysłanie informacji o pasie do serwera
                    iPassed = true;
                    network.sendPassMessage();
                    gameView.showMessage("Pasujesz turę.");
                    currentTurn = currentTurn.opponent();
                }
                else if(input.equalsIgnoreCase("surrender")){
                    // Wysłanie informacji o poddaniu się do serwera
                    network.sendSurrenderMessage();
                    gameView.showMessage("Poddajesz się. Koniec gry.");
                    break;
                }
                else if(input.equalsIgnoreCase("quit")){
                    // Wysłanie informacji o wyjściu z gry do serwera
                    network.sendQuitMessage();
                    gameView.showMessage("Wychodzisz z gry.");
                    break;
                }
                else {
                    // Proba przetlumaczenia współrzędnych i wysłania ruchu do serwera
                    try {
                        int[] coordinates = TranslateCoordinate.translate(input);
                        if (coordinates != null) {
                            if (coordinates[0] >= 0 && coordinates[0] < 19 && coordinates[1] >= 0 && coordinates[1] < 19) {
                                network.sendSpaceInformation(coordinates);
                                board.setField(coordinates[0], coordinates[1], myColor);
                                gameView.showBoard(board);
                                gameView.showMessage("Ruch wysłany. Czekanie na odpowiedź bota...");
                                iPassed = false;
                                currentTurn = currentTurn.opponent();
                            } else {
                                gameView.showMessage("Ruch poza planszą!");
                            }
                        } else {
                            gameView.showMessage("Nieprawidłowy format ruchu");
                        }
                    } catch (NumberFormatException e) {
                        gameView.showMessage("Nieprawidłowe współrzędne. Spróbuj ponownie.");
                    }
                }

            } else {
                // Oczekiwanie na ruch przeciwnika
                if (!waitingMessageShown) {
                    gameView.showMessage("Czekanie na ruch przeciwnika...");
                    waitingMessageShown = true;
                }

                int messageType = network.readMessage(); // odczytanie rodzaju wiadomosci od serwera

                if (messageType == Protocol.MOVE) {
                    int[] coordinates = network.getCoordinates(); // wspolrzedne ruchu przeciwnika
                    gameView.showMessage("Przeciwnik postawił kamień na: " + TranslateCoordinate.invertTranslate(coordinates[0]) + coordinates[1]);
                    currentTurn = currentTurn.opponent();
                }
                else if (messageType == Protocol.BOARD_STATE) {
                    network.receiveBoardFromServer(board); // aktualizacja stanu planszy
                    gameView.showBoard(board);
                }
                else if (messageType == Protocol.CAPTURES) {
                    int [] captures = network.getCaptures();
                    blackCaptures = captures[0];
                    whiteCaptures = captures[1];
                }
                else if (messageType== Protocol.START_MARKING) {
                    gameView.showMessage("Obaj gracze spasowali. Rozpoczynanie fazy oznaczania martwych kamieni.");
                    gameView.showMessage("Kliknij na grupy kamieni, które uważasz za martwe. Kliknij ponownie, aby odznaczyć. Wciśnij 'quit', aby zakończyć grę.");
                    boolean amIProposing = network.readBoolean(); // Serwer mówi, czy zaczynam
                    handleNegotiationLoop(amIProposing); // Wchodzimy w osobną pętlę negocjacji
                    break; // Po negocjacjach koniec gry
                }
                else if (messageType == Protocol.INVALID_MOVE) {
                    // obsługa nielegalnego ruchu przeciwnika
                    int[] coordinates = network.getCoordinates();
                    gameView.showMessage("Ruch ("+TranslateCoordinate.invertTranslate(coordinates[0])+","+coordinates[1]+") jest nielegalny! Spróbuj ponownie.");
                    currentTurn = currentTurn.opponent();
                }
                else if (messageType == Protocol.PASS) {
                    // obsługa pasa przeciwnika
                    gameView.showMessage("Przeciwnik spasował.");
                    if(iPassed){
                        gameView.showMessage("Obaj gracze spasowali. Rozpoczynanie fazy oznaczania martwych kamieni.");
                    }else{
                        currentTurn = currentTurn.opponent();
                    }

                }
                else if (messageType == Protocol.SURRENDER) {
                    // obsługa poddania się przeciwnika
                    gameView.showMessage("Przeciwnik się poddał! Wygrałeś.");
                    break;
                }
                else if (messageType == Protocol.QUIT) {
                    // obsługa wyjścia przeciwnika z gry
                    gameView.showMessage("Przeciwnik wyszedł z gry.");
                    break;
                }
                else if (messageType == Protocol.MESSAGE) {
                    // obsługa wiadomości od przeciwnika
                    String message = network.receiveMessage();
                    gameView.showChatMessage("Przeciwnik", message);
                }
            }
        }
    }

    /**
     * Obsługuje kliknięcia w fazie oznaczania martwych kamieni.
     * Dodaje lub usuwa całe grupy kamieni z listy propozycji.
     *
     * @param input        współrzędne kliknięcia w formacie tekstowym (np. "A10").
     * @param proposalList lista punktów aktualnie oznaczonych jako martwe.
     */
    private void handleMarkingClick(String input, ArrayList<Point> proposalList) {
        int[] coords = TranslateCoordinate.translate(input);
        if (coords == null) return;

        int x = coords[0];
        int y = coords[1];
        Stone clickedStone = board.getField(x, y);
        if (clickedStone == Stone.EMPTY) return;

        Set<Point> group = new HashSet<>();
        Set<Point> liberties = new HashSet<>();

        // Używamy mechaniki
        mechanics.exploreGroup(board, new Point(x, y), clickedStone, group, liberties);

        // Sprawdzamy czy grupa jest już na liście propozycji
        boolean isMarked = false;
        for (Point p : proposalList) {
            if (p.x == x && p.y == y) {
                isMarked = true;
                break;
            }
        }

        // Dodajemy lub usuwamy z podanej listy
        if (isMarked) {
            proposalList.removeAll(group);
            gameView.showMessage("Odznaczono grupę.");
        } else {
            for(Point p : group) {
                if(!proposalList.contains(p)) proposalList.add(p);
            }
            gameView.showMessage("Zaznaczono grupę.");
        }

        gameView.highlightGroups(new HashSet<>(proposalList));
        gameView.setAcceptButtonActive(false);
        gameView.showBoard(board);
    }

    /**
     * Obsługuje pętlę negocjacji martwych grup.
     * Gracze na zmianę proponują układ martwych kamieni lub go akceptują.
     *
     * @param amIProposing określa, czy gracz zaczyna jako proponujący (true) czy oczekujący (false).
     * @throws IOException w przypadku błędu sieciowego.
     */
    private void handleNegotiationLoop(boolean amIProposing) throws IOException {
        ArrayList<Point> currentProposal = new ArrayList<>(); // Lokalne zaznaczenia
        gameView.setNegotiationMode(true);
        if (amIProposing) {
            gameView.setAcceptButtonActive(false);
        }

        while (true) {
            if (amIProposing) {
                gameView.showMessage("TWOJA KOLEJ: Zaznacz martwe grupy i kliknij 'done' aby wysłać.");
                //  Faza akywna: Gracz klika
                boolean proposalReady = false;
                while (!proposalReady) {
                    String input = gameView.getInput();
                    if (input.equalsIgnoreCase("done")) {
                        proposalReady = true;
                    } else if (input.equalsIgnoreCase("accept")) {
                        // Opcja, jeśli dostałem propozycję i nic nie zmieniłem
                        network.sendAccept();
                        int msg = network.readMessage();
                        if (msg == Protocol.GAME_OVER) {
                            showResults();
                        }
                        return;
                    } else {
                        handleMarkingClick(input, currentProposal);
                    }
                }
                //  Wysyłamy propozycję
                network.sendProposal(currentProposal);
                gameView.showMessage("Propozycja wysłana. Czekanie na odpowiedź...");
                amIProposing = false; // Teraz czekamy
            }
            else {
                //  Faza pasywna: Czekanie na serwer
                gameView.showMessage("Czekanie na propozycję przeciwnika...");
                int msg = network.readMessage();

                if (msg == Protocol.RECEIVE_PROPOSAL) {
                    currentProposal = network.readProposal(); // Odbierz listę
                    gameView.highlightGroups(new HashSet<>(currentProposal)); // Pokaż na planszy
                    gameView.showBoard(board);
                    gameView.setAcceptButtonActive(true);
                    gameView.showMessage("Otrzymano propozycję. Jeśli pasuje kliknij 'accept'. Jeśli nie - popraw ją i kliknij 'done'.");
                    amIProposing = true;
                }
                else if (msg == Protocol.GAME_OVER) {
                    showResults();
                    return;
                }
            }
        }
    }

    /**
     * Pobiera wynik końcowy od serwera i wyświetla okno zakończenia gry.
     *
     * @throws IOException w przypadku błędu odczytu wyników z sieci.
     */
    private void showResults() throws IOException {
        int blackScore = network.readMessage();
        int whiteScore = network.readMessage();

        String scoreMsg = "Czarny: " + blackScore + " pkt\n" +"Biały: " + whiteScore + " pkt";

        String title;
        if (myColor == Stone.BLACK) {
            if (blackScore > whiteScore) title = "WYGRAŁEŚ!";
            else if (blackScore < whiteScore) title = "PRZEGRAŁEŚ...";
            else title = "REMIS!";
        } else {
            if (whiteScore > blackScore) title = "WYGRAŁEŚ!";
            else if (whiteScore < blackScore) title = "PRZEGRAŁEŚ...";
            else title = "REMIS!";
        }

        gameView.setNegotiationMode(false);
        gameView.showEndGameDialog(title, scoreMsg);
    }
}