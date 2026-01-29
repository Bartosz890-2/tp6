package go.server;
import java.awt.Point;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;
import java.util.ArrayList;

import go.database.GameRepository;
import go.database.GameResult;
import go.logic.Board;
import go.logic.GameMechanics;
import go.logic.Protocol;
import go.logic.Stone;

/**
 * Klasa reprezentująca pojedynczą sesję gry (wątek serwera).
 * Zarządza komunikacją między dwoma graczami, przetwarza ich ruchy,
 * egzekwuje zasady gry przy użyciu {@link GameMechanics} i synchronizuje stan planszy.
 * <p>
 * Klasa implementuje interfejs {@link Runnable}, co pozwala na obsługę wielu gier
 * równolegle przez serwer główny.
 */
public class GameSession implements Runnable{
    //gniazda sieciowe dla graczy
    private final Socket p1Socket;
    private final Socket p2Socket;
    private final Board board;
    private final GameMechanics mechanics;

    private final GameRepository gameRepository;
    private final StringBuilder historyLog=new StringBuilder();
    /**
     * Lista punktów oznaczonych jako martwe w fazie negocjacji.
     * Służy jako bufor przechowujący propozycję jednego gracza przed wysłaniem jej do drugiego
     * lub przed finalnym obliczeniem wyniku.
     */
    private final ArrayList<Point> currentProposalPoints = new ArrayList<>();

    /**
     * Tworzy nową sesję gry dla dwóch połączonych klientów.
     * Inicjalizuje nową, pustą planszę o rozmiarze 19x19 oraz silnik zasad gry.
     *
     * @param p1 gniazdo sieciowe pierwszego gracza (który zagra Czarnymi).
     * @param p2 gniazdo sieciowe drugiego gracza (który zagra Białymi).
     */
    public GameSession(Socket p1, Socket p2, GameRepository gameRepository){
        this.p1Socket = p1;
        this.p2Socket = p2;
        this.board = new Board(19);
        this.mechanics = new GameMechanics();
        this.gameRepository = gameRepository;
    }

    /**
     * Główna pętla wątku sesji.
     * <p>
     * Metoda ta odpowiada za:
     * <ul>
     * <li>Inicjalizację strumieni wejścia/wyjścia dla obu graczy.</li>
     * <li>Poinformowanie pierwszego gracza o rozpoczęciu gry.</li>
     * <li>Cykliczne oczekiwanie na komunikat od aktualnego gracza.</li>
     * <li>Przetwarzanie typów wiadomości: RUCH, PAS, NEGOCJACJE, PODDANIE, CZAT.</li>
     * <li>Walidację ruchów i wysyłanie aktualizacji stanu planszy do obu klientów.</li>
     * <li>Obsługę kończenia gry i obliczania ostatecznego wyniku punktowego.</li>
     * </ul>
     */
    @Override
    public void run() {
        try{
            //strumienie wejscia i wyjscia dla obu graczy
            DataInputStream input1 = new DataInputStream(p1Socket.getInputStream());
            DataOutputStream output1 = new DataOutputStream(p1Socket.getOutputStream());

            DataInputStream input2 = new DataInputStream(p2Socket.getInputStream());
            DataOutputStream output2 = new DataOutputStream(p2Socket.getOutputStream());
            //ustawienie tablic strumieni i kolorów, aby ułatwić zarządzanie w pętli gry
            DataInputStream[] inputs = {input1, input2};
            DataOutputStream[] outputs = {output1, output2};
            Stone[] colors = {Stone.BLACK, Stone.WHITE};

            output1.writeInt(1);
            output1.flush();

            output2.writeInt(2);
            output2.flush();

            output1.writeInt(1);
            output1.flush();


            System.out.println("Gra Multiplayer rozpoczęta.");

            //gracze wewnatrz programu sa numerowani 0 i 1 ze wzgledu na tablice
            int currentPlayer = 0;
            int consecutivePasses = 0;

            while (true) {
                int opponent = 1 - currentPlayer; //numer przeciwnika
                int messageType = inputs[currentPlayer].readInt();

                if (messageType == Protocol.MOVE) {
                    //odcyztanie współrzędnych ruchu
                    int x = inputs[currentPlayer].readInt();
                    int y = inputs[currentPlayer].readInt();

                    //sprwadzenie czy ruch jest legalny
                    if (mechanics.IsMovePossible(board, x, y, colors[currentPlayer])) {
                        consecutivePasses = 0; //reset liczby kolejnych passów

                        //wykonanie ruchu na planszy
                        System.out.println("Gracz " + (currentPlayer + 1) + " wykonał ruch na pozycję (" + x + "," + y + ")");
                        String moveStr = (char)('A' + x) + "" + (y + 1);
                        String colorStr = (currentPlayer == 0) ? "B" : "W"; // 0 to Black, 1 to White
                        historyLog.append(colorStr).append("[").append(moveStr).append("];");
                        //Przekazanie stanu planszy i liczby jeńców obu graczom
                        outputs[currentPlayer].writeInt(Protocol.BOARD_STATE);
                        Protocol.sendBoard(board, outputs[currentPlayer]);

                        outputs[currentPlayer].writeInt(Protocol.CAPTURES);
                        outputs[currentPlayer].writeInt(mechanics.blackCaptures);
                        outputs[currentPlayer].writeInt(mechanics.whiteCaptures);
                        outputs[currentPlayer].flush();

                        //wysłanie informacji o ruchu przeciwnikowi, tak aby UI mogło zaktualizować planszę
                        outputs[opponent].writeInt(Protocol.BOARD_STATE);
                        Protocol.sendBoard(board, outputs[opponent]);

                        outputs[opponent].writeInt(Protocol.CAPTURES);
                        outputs[opponent].writeInt(mechanics.blackCaptures);
                        outputs[opponent].writeInt(mechanics.whiteCaptures);

                        // Informacja o ruchu (żeby UI przeciwnika wiedziało, że teraz jego kolej)
                        outputs[opponent].writeInt(Protocol.MOVE);
                        outputs[opponent].writeInt(x);
                        outputs[opponent].writeInt(y);

                        outputs[opponent].flush();
                        //zmiana tury
                        currentPlayer = opponent;
                    } else {
                        //gdy ruch był nielegalny informujemy gracza i nie zmienyamy tury
                        System.out.println("Gracz " + (currentPlayer + 1) + " wykonał nielegalny ruch na pozycję (" + x + "," + y + ")");
                        outputs[currentPlayer].writeInt(Protocol.INVALID_MOVE);
                        outputs[currentPlayer].writeInt(x);
                        outputs[currentPlayer].writeInt(y);
                        outputs[currentPlayer].flush();
                    }
                } else if (messageType == Protocol.PASS) {
                    //w przypadku passu przekazujemy informację przeciwnikowi
                    System.out.println("Gracz " + (currentPlayer + 1) + " pasuje.");
                    outputs[opponent].writeInt(Protocol.PASS);
                    outputs[opponent].flush();
                    consecutivePasses++;
                    String colorStr = (currentPlayer == 0) ? "B" : "W"; // 0 to Black, 1 to White
                    historyLog.append(colorStr).append("[PASS];");
                    if(consecutivePasses >=2){
                        System.out.println("Faza negocjacji.");
                        // 1. Informujemy obu graczy, że zaczynamy oznaczanie
                        // WAŻNE: Musimy ustalić, kto zaczyna proponować. Niech zacznie ten, czyja była tura (currentPlayer).

                        // Do gracza aktywnego (ten co ma proponować)
                        outputs[currentPlayer].writeInt(Protocol.START_MARKING);
                        outputs[currentPlayer].writeBoolean(true); // true = Ty proponujesz pierwszy
                        outputs[currentPlayer].flush();

                        // Do gracza pasywnego (ten co czeka)
                        outputs[opponent].writeInt(Protocol.START_MARKING);
                        outputs[opponent].writeBoolean(false); // false = Ty czekasz
                        outputs[opponent].flush();
                    }
                    else{
                        currentPlayer = opponent;
                    }
                }
                else if (messageType == Protocol.SEND_PROPOSAL) {
                    int count = inputs[currentPlayer].readInt(); // wysylamy liczbe punktow, potem w nastepnej kolejnosci wysylamy wlasnie tyle par koordynatow

                    currentProposalPoints.clear();

                    System.out.println("Otrzymano propozycję (" + count + " kamieni) od Gracza " + currentPlayer);

                    outputs[opponent].writeInt(Protocol.RECEIVE_PROPOSAL);
                    outputs[opponent].writeInt(count); // Przekazujemy liczbe
                    for(int i=0; i<count; i++) {
                        int px = inputs[currentPlayer].readInt();
                        int py = inputs[currentPlayer].readInt();
                        currentProposalPoints.add(new Point(px, py));//w zmiennej currentProposalPoints przechowujemy tablice pionkow do usuniecia z planszy
                        //i policzenia ich jako jencow

                        outputs[opponent].writeInt(px); // Przekazujemy koordynaty
                        outputs[opponent].writeInt(py);
                    }
                    outputs[opponent].flush();
                    currentPlayer = opponent;
                }

                else if (messageType == Protocol.ACCEPT_PROPOSAL) {
                    System.out.println("Gracz " + (currentPlayer + 1) + " zaakceptował układ.");

                    // jezeli zaakceptowano propozycje, usuwamy z planszy wszystkie pionki
                    mechanics.takeOffDeadGroups(board, currentProposalPoints);

                    // Obliczenie końcowego wyniku (terytorium + jeńcy)
                    mechanics.calculateGameScore(board);
                    int blackScore = mechanics.getBlackTerritory() + mechanics.blackCaptures;
                    int whiteScore = mechanics.getWhiteTerritory() + mechanics.whiteCaptures;
                    String winner = (blackScore > whiteScore) ? "Black" : "White";
                    if (blackScore == whiteScore) winner = "Draw";
                    
                    GameResult result = new GameResult(winner, blackScore, whiteScore, "PvP", historyLog.toString());
                    gameRepository.save(result);
                    System.out.println("Wynik gry - Czarny: " + blackScore + ", Biały: " + whiteScore);

                    // Wysłanie wyników do gracza akceptującego
                    outputs[currentPlayer].writeInt(Protocol.GAME_OVER);
                    outputs[currentPlayer].writeInt(blackScore);
                    outputs[currentPlayer].writeInt(whiteScore);
                    outputs[currentPlayer].flush();

                    // Wysłanie wyników do gracza oczekującego
                    outputs[opponent].writeInt(Protocol.GAME_OVER);
                    outputs[opponent].writeInt(blackScore);
                    outputs[opponent].writeInt(whiteScore);
                    outputs[opponent].flush();

                    break;

                } else if (messageType == Protocol.SURRENDER) {
                    //gdy gracz sie podda przekazujemy informację przeciwnikowi i kończymy grę
                    System.out.println("Gracz " + (currentPlayer + 1) + " się poddał. Gracz " + (opponent + 1) + " wygrywa.");
                    outputs[opponent].writeInt(Protocol.SURRENDER);
                    outputs[opponent].flush();
                    break;
                } else if (messageType == Protocol.QUIT) {
                    //gdy gracz wychodzi z gry przekazujemy informację przeciwnikowi i kończymy grę
                    System.out.println("Gracz " + (currentPlayer + 1) + " wyszedł z gry.");
                    outputs[opponent].writeInt(Protocol.QUIT);
                    outputs[opponent].flush();
                    break;
                } else if (messageType == Protocol.MESSAGE) {
                    //obsługa wiadomości od gracza
                    String message = inputs[currentPlayer].readUTF();
                    System.out.println(message);
                    //przekazanie wiadomości przeciwnikowi
                    outputs[opponent].writeInt(Protocol.MESSAGE);
                    outputs[opponent].writeUTF(message);
                    outputs[opponent].flush();
                }
            }
        } catch (Exception e) {
            System.out.println("Któryś z graczy rozłączył się.");
        }
    }
}