package go.server;

import java.awt.Point;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;

import go.database.GameRepository;
import go.database.GameResult;
import go.logic.Board;
import go.logic.GameMechanics;
import go.logic.Protocol;
import go.logic.Stone;
import go.si.SmartBot;

/**
 * Klasa reprezentująca sesję gry z Botem (Player vs Environment/AI).
 * <p>
 * Odpowiada za:
 * <ul>
 * <li>Komunikację sieciową z JEDNYM klientem (człowiek).</li>
 * <li>Zarządzanie stanem planszy i logiką gry (GameMechanics).</li>
 * <li>Wykonywanie obliczeń ruchu bota (SmartBot) w turze przeciwnika.</li>
 * <li>Synchronizację tur (Gracz -> Bot -> Gracz).</li>
 * <li>Zapis przebiegu i wyniku gry do bazy danych (GameRepository).</li>
 * </ul>
 * Człowiek zawsze gra kamieniami Czarnymi (Player 1), a Bot Białymi (Player 2).
 */
public class BotGameSession implements Runnable {

    /** Gniazdo sieciowe połączonego gracza (człowieka). */
    private final Socket humanSocket;

    /** Plansza do gry Go o rozmiarze 19x19. */
    private final Board board;

    /** Silnik zasad gry (walidacja ruchów, bicie, liczenie punktów). */
    private final GameMechanics mechanics;

    /** Instancja sztucznej inteligencji podejmująca decyzje za drugiego gracza. */
    private final SmartBot smartBot;

    /** Repozytorium do zapisu wyników gry w bazie danych. */
    private final GameRepository gameRepository;

    /** Bufor przechowujący historię ruchów w formacie SGF-podobnym (np. "B[A1];W[D4];"). */
    private final StringBuilder historyLog = new StringBuilder();

    // Gracz to zawsze Czarny (Player 1), Bot to Biały (Player 2)
    private final Stone humanColor = Stone.BLACK;
    private final Stone botColor = Stone.WHITE;

    /** Lista punktów oznaczonych jako martwe podczas fazy negocjacji końcowej. */
    private final ArrayList<Point> currentProposalPoints = new ArrayList<>();

    /**
     * Tworzy nową sesję gry z Botem.
     *
     * @param humanSocket    aktywne połączenie sieciowe z klientem gracza.
     * @param gameRepository repozytorium do zapisu wyniku końcowego gry.
     */
    public BotGameSession(Socket humanSocket, GameRepository gameRepository) {
        this.humanSocket = humanSocket;
        this.board = new Board(19);
        this.mechanics = new GameMechanics();
        // Inicjalizujemy bota
        this.smartBot = new SmartBot(mechanics);
        this.gameRepository = gameRepository;
    }

    /**
     * Główna pętla gry z Botem.
     * <p>
     * Działa w nieskończonej pętli (do momentu zakończenia gry) i obsługuje naprzemiennie:
     * <ol>
     * <li><b>Turę Gracza:</b> Odbiera ruch z sieci, waliduje go i aktualizuje planszę.</li>
     * <li><b>Turę Bota:</b> Wywołuje algorytm bota, wykonuje ruch na planszy i odsyła go do klienta.</li>
     * </ol>
     * Obsługuje również specjalne akcje: PASS (pasowanie), QUIT (wyjście) i SURRENDER (poddanie się).
     */
    @Override
    public void run() {
        try {
            // Strumienie TYLKO dla człowieka
            DataInputStream input = new DataInputStream(humanSocket.getInputStream());
            DataOutputStream output = new DataOutputStream(humanSocket.getOutputStream());

            // Gracz jest zawsze nr 1 (Czarny) - wysyłamy ID gracza
            output.writeInt(1);
            output.flush();
            System.out.println("Gra z Botem rozpoczęta.");

            // Bot jest graczem nr 2 (ale nie wysyłamy tego do nikogo, bo bot jest lokalny)
            // Wysyłamy do klienta informację, że gra się zaczęła (np. ID przeciwnika/sygnał startu)
            output.writeInt(1);
            output.flush();

            // 0 = Człowiek (Czarny), 1 = Bot (Biały)
            int currentPlayerIndex = 0;
            int consecutivePasses = 0;

            while (true) {
                // ============================================================
                // TURA CZŁOWIEKA (CZYTAMY Z SIECI)
                // ============================================================
                if (currentPlayerIndex == 0) {
                    int messageType = input.readInt(); // Czekamy na ruch gracza

                    if (messageType == Protocol.MOVE) {
                        int x = input.readInt();
                        int y = input.readInt();

                        if (mechanics.IsMovePossible(board, x, y, humanColor)) {
                            consecutivePasses = 0;      // Resetujemy licznik pasów
                            currentPlayerIndex = 1;     // Przekazujemy turę BOTOWI
                            String moveStr = (char)('A' + x) + "" + (y + 1);
                            historyLog.append("B[").append(moveStr).append("];");
                            System.out.println("Gracz wykonał ruch: " + x + ", " + y);
                        } else {
                            System.out.println("Nielegalny ruch gracza: " + x + ", " + y);

                            output.writeInt(Protocol.INVALID_MOVE);
                            output.writeInt(x);
                            output.writeInt(y);
                            output.flush();
                        }
                    }
                    else if (messageType == Protocol.PASS) {
                        historyLog.append("B[PASS];");
                        System.out.println("Gracz pasuje.");
                        consecutivePasses++;

                        // Jeśli to drugi pas z rzędu (Gracz pasował, a wcześniej Bot pasował) -> Koniec gry
                        if (consecutivePasses >= 2) {
                            handleEndGameNegotiation(input, output);
                            break;
                        } else {
                            currentPlayerIndex = 1; // Tura bota
                        }
                    }
                    else if (messageType == Protocol.QUIT) {
                        System.out.println("Gracz opuścił grę.");
                        String winner = "White"; // Bot wygrywa walkowerem

                        // Obliczamy stan na moment wyjścia
                        mechanics.calculateGameScore(board);
                        int bScore = mechanics.getBlackTerritory() + mechanics.blackCaptures;
                        int wScore = mechanics.getWhiteTerritory() + mechanics.whiteCaptures;

                        historyLog.append("B[QUIT];");
                        GameResult result = new GameResult(winner, bScore, wScore, "Bot", historyLog.toString());
                        gameRepository.save(result);
                        System.out.println("Zapisano wynik (Quit) do bazy!");
                        break;
                    }
                    else if (messageType == Protocol.SURRENDER) {
                        System.out.println("Gracz się poddał.");
                        String winner = "White"; // Bot wygrywa

                        historyLog.append("B[SURRENDER];");
                        mechanics.calculateGameScore(board);
                        int bScore = mechanics.getBlackTerritory() + mechanics.blackCaptures;
                        int wScore = mechanics.getWhiteTerritory() + mechanics.whiteCaptures;

                        GameResult result = new GameResult(winner, bScore, wScore, "Bot", historyLog.toString());
                        gameRepository.save(result);
                        System.out.println("Zapisano wynik (Surrender) do bazy!");
                        break;
                    }
                }
                // ============================================================
                // TURA BOTA (OBLICZENIA LOKALNE)
                // ============================================================
                else {
                    System.out.println("Bot myśli...");
                    // 1. Pytamy bota o ruch (to może chwilę potrwać)
                    Point botMove = smartBot.calculateBestMove(board, botColor);

                    if (botMove != null) {
                        // --- BOT WYKONUJE RUCH ---
                        // Rejestrujemy ruch w silniku
                        mechanics.IsMovePossible(board, botMove.x, botMove.y, botColor);

                        String moveStr = (char)('A' + botMove.x) + "" + (botMove.y + 1);
                        historyLog.append("W[").append(moveStr).append("];");

                        consecutivePasses = 0;
                        System.out.println("Bot zagrał: " + botMove.x + ", " + botMove.y);

                        // 2. Wysyłamy aktualizację planszy do Człowieka
                        sendUpdateToHuman(output, botMove.x, botMove.y, Protocol.MOVE);

                        // 3. Wysyłamy informację o konkretnym ruchu (współrzędne)
                        output.writeInt(Protocol.MOVE);
                        output.writeInt(botMove.x);
                        output.writeInt(botMove.y);
                        output.flush();

                        currentPlayerIndex = 0; // Wracamy do człowieka
                    } else {
                        // --- BOT PASUJE ---
                        System.out.println("Bot pasuje.");
                        historyLog.append("W[PASS];");
                        consecutivePasses++;

                        // Informujemy człowieka o pasie bota
                        output.writeInt(Protocol.PASS);
                        output.flush();

                        if (consecutivePasses >= 2) {
                            handleEndGameNegotiation(input, output);
                            break;
                        } else {
                            currentPlayerIndex = 0; // Wracamy do człowieka
                        }
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("Błąd połączenia z graczem.");
        }
    }

    /**
     * Pomocnicza metoda wysyłająca pełny stan planszy i liczbę jeńców do klienta.
     * Używana po ruchu bota, aby zaktualizować widok gracza.
     *
     * @param output strumień wyjściowy do klienta.
     * @param lastX  współrzędna X ostatniego ruchu (niewykorzystywana w tej implementacji, ale zgodna z protokołem).
     * @param lastY  współrzędna Y ostatniego ruchu.
     * @param type   typ wiadomości (zazwyczaj Protocol.MOVE).
     * @throws IOException w przypadku błędu zapisu do strumienia.
     */
    private void sendUpdateToHuman(DataOutputStream output, int lastX, int lastY, int type) throws IOException {
        output.writeInt(Protocol.BOARD_STATE);
        Protocol.sendBoard(board, output);

        output.writeInt(Protocol.CAPTURES);
        output.writeInt(mechanics.blackCaptures);
        output.writeInt(mechanics.whiteCaptures);
        output.flush();
    }

    /**
     * Obsługuje fazę negocjacji końcowej po dwóch pasach.
     * <p>
     * W trybie Bot vs Human:
     * <ol>
     * <li>Serwer prosi gracza o zaznaczenie martwych grup.</li>
     * <li>Gracz wysyła propozycję (listę martwych kamieni).</li>
     * <li>Bot <b>automatycznie i bezwarunkowo akceptuje</b> propozycję gracza (ufa człowiekowi).</li>
     * <li>Serwer oblicza ostateczny wynik, zapisuje go do bazy i kończy grę.</li>
     * </ol>
     *
     * @param input  strumień wejściowy od klienta.
     * @param output strumień wyjściowy do klienta.
     * @throws IOException w przypadku błędu komunikacji sieciowej.
     */
    private void handleEndGameNegotiation(DataInputStream input, DataOutputStream output) throws IOException {
        System.out.println("Koniec gry. Faza zaznaczania.");

        // 1. Mówimy graczowi: "Zacznij zaznaczać"
        output.writeInt(Protocol.START_MARKING);
        output.writeBoolean(true); // true = Ty (gracz) proponujesz jako pierwszy
        output.flush();

        while (true) {
            int msg = input.readInt();
            if (msg == Protocol.SEND_PROPOSAL) {
                // Gracz wysyła propozycję (lista martwych kamieni)
                int count = input.readInt();
                currentProposalPoints.clear();
                for (int i = 0; i < count; i++) {
                    int x = input.readInt();
                    int y = input.readInt();
                    currentProposalPoints.add(new Point(x, y));
                }

                // 2. BOT ZAWSZE AKCEPTUJE PROPOZYCJĘ GRACZA
                // Zdejmujemy martwe grupy z planszy
                mechanics.takeOffDeadGroups(board, currentProposalPoints);

                // Liczymy terytorium
                mechanics.calculateGameScore(board);

                int blackScore = mechanics.getBlackTerritory() + mechanics.blackCaptures;
                int whiteScore = mechanics.getWhiteTerritory() + mechanics.whiteCaptures;
                String winner = (blackScore > whiteScore) ? "Black" : "White";
                if (blackScore == whiteScore) winner = "Draw";

                // Zapisujemy wynik do bazy
                GameResult result = new GameResult(winner, blackScore, whiteScore, "Bot", historyLog.toString());
                gameRepository.save(result);

                // Wysyłamy wynik do klienta
                output.writeInt(Protocol.GAME_OVER);
                output.writeInt(blackScore);
                output.writeInt(whiteScore);
                output.flush();
                break;
            }
        }
    }
}