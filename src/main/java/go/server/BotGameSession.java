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
import go.si.SmartBot; // Upewnij się, że importujesz swojego bota

public class BotGameSession implements Runnable {
    private final Socket humanSocket;
    private final Board board;
    private final GameMechanics mechanics;
    private final SmartBot smartBot; // Nasz mózg

    private final GameRepository gameRepository;
    private final StringBuilder historyLog = new StringBuilder();
    // Gracz to zawsze Czarny (Player 1), Bot to Biały (Player 2)
    private final Stone humanColor = Stone.BLACK;
    private final Stone botColor = Stone.WHITE;

    private final ArrayList<Point> currentProposalPoints = new ArrayList<>();

    public BotGameSession(Socket humanSocket, GameRepository gameRepository) {
        this.humanSocket = humanSocket;
        this.board = new Board(19);
        this.mechanics = new GameMechanics();
        // Inicjalizujemy bota
        this.smartBot = new SmartBot(mechanics);
        this.gameRepository = gameRepository;
    }

    @Override
    public void run() {
        try {
            // Strumienie TYLKO dla człowieka
            DataInputStream input = new DataInputStream(humanSocket.getInputStream());
            DataOutputStream output = new DataOutputStream(humanSocket.getOutputStream());

            // Gracz jest zawsze nr 1 (Czarny)
            output.writeInt(1);
            output.flush();
            System.out.println("Gra z Botem rozpoczęta.");

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

                        // Informujemy klienta, że spasował (potwierdzenie)
                        // W protokole zazwyczaj wysyłamy info o ruchu przeciwnika,
                        // ale tu po prostu zmieniamy turę.

                        if (consecutivePasses >= 2) {
                            handleEndGameNegotiation(input, output);
                            break;
                        } else {
                            currentPlayerIndex = 1; // Tura bota
                        }
                    }
                    else if (messageType == Protocol.QUIT) {
                        break; // Wyjście
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
                        // Nie musimy sprawdzać IsMovePossible, bo bot wybiera tylko legalne,
                        // ale dla bezpieczeństwa w silniku można:
                        mechanics.IsMovePossible(board, botMove.x, botMove.y, botColor);
                        String moveStr = (char)('A' + botMove.x) + "" + (botMove.y + 1);
                        historyLog.append("W[").append(moveStr).append("];");
                        consecutivePasses = 0;
                        System.out.println("Bot zagrał: " + botMove.x + ", " + botMove.y);

                        // 2. Wysyłamy ruch bota do Człowieka przez sieć
                        // Żeby klient wiedział, że bot coś postawił
                        sendUpdateToHuman(output, botMove.x, botMove.y, Protocol.MOVE);

                        // Dodatkowo wysyłamy pakiet MOVE, żeby UI wiedziało co się stało
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

    // Metoda pomocnicza do wysyłania stanu planszy
    private void sendUpdateToHuman(DataOutputStream output, int lastX, int lastY, int type) throws IOException {
        output.writeInt(Protocol.BOARD_STATE);
        Protocol.sendBoard(board, output);

        output.writeInt(Protocol.CAPTURES);
        output.writeInt(mechanics.blackCaptures);
        output.writeInt(mechanics.whiteCaptures);
        output.flush();
    }

    // Prosta obsługa końca gry z botem (Bot ufa człowiekowi)
    private void handleEndGameNegotiation(DataInputStream input, DataOutputStream output) throws IOException {
        System.out.println("Koniec gry. Faza zaznaczania.");

        // 1. Mówimy graczowi: "Zacznij zaznaczać"
        output.writeInt(Protocol.START_MARKING);
        output.writeBoolean(true); // Ty proponujesz
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
                // Od razu kończymy grę
                mechanics.takeOffDeadGroups(board, currentProposalPoints);
                mechanics.calculateGameScore(board);

                int blackScore = mechanics.getBlackTerritory() + mechanics.blackCaptures;
                int whiteScore = mechanics.getWhiteTerritory() + mechanics.whiteCaptures;
                String winner = (blackScore > whiteScore) ? "Black" : "White";
                GameResult result = new GameResult(winner, blackScore, whiteScore, "Bot", historyLog.toString());
                gameRepository.save(result);
                if (blackScore == whiteScore) winner = "Draw";
                
                // Wysyłamy wynik
                output.writeInt(Protocol.GAME_OVER);
                output.writeInt(blackScore);
                output.writeInt(whiteScore);
                output.flush();
                break;
            }
        }
    }
}