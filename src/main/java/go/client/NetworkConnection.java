package go.client;

import java.awt.Point;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;

import go.logic.Board;
import go.logic.Protocol;

/**
 * Klasa implementująca wzorzec Fasady (Facade) dla warstwy sieciowej klienta.
 * Ukrywa szczegóły implementacji gniazd (Socket) i strumieni danych,
 * udostępniając proste metody do wysyłania i odbierania komunikatów protokołu gry.
 */
public class NetworkConnection {
    private Socket socket;
    private DataInputStream fromServer;
    private DataOutputStream toServer;

    /**
     * Domyślny konstruktor.
     */
    public NetworkConnection() {
    }

    public void sendGameMode(int mode) throws IOException {
        toServer.writeInt(mode);
        toServer.flush();
    }
    /**
     * Nawiązuje połączenie TCP z serwerem gry (localhost) na porcie określonym w Protokole.
     * Inicjalizuje strumienie wejścia i wyjścia.
     *
     * @throws IOException gdy nie uda się nawiązać połączenia.
     */
    public void connect() throws IOException {
        socket = new Socket("localhost", Protocol.Port);
        fromServer = new DataInputStream(socket.getInputStream());
        toServer = new DataOutputStream(socket.getOutputStream());
    }

    /**
     * Pobiera ID przypisane graczowi przez serwer.
     *
     * @return ID gracza (1 lub 2).
     * @throws IOException przy błędzie odczytu.
     */
    public int getPlayerId() throws IOException {
        return fromServer.readInt();
    }

    /**
     * Blokuje wykonanie do momentu otrzymania sygnału o dołączeniu drugiego gracza.
     *
     * @throws IOException przy błędzie odczytu.
     */
    public void waitForSecondPlayer() throws IOException {
        fromServer.readInt();
    }

    /**
     * Odczytuje wartość typu boolean z serwera.
     *
     * @return odczytana wartość logiczna.
     * @throws IOException przy błędzie odczytu.
     */
    public boolean readBoolean() throws IOException {
        return fromServer.readBoolean();
    }

    /**
     * Wysyła do serwera komunikat o spasowaniu ruchu (PASS).
     *
     * @throws IOException przy błędzie zapisu.
     */
    public void sendPassMessage() throws IOException {
        toServer.writeInt(Protocol.PASS);
        toServer.flush();
    }

    /**
     * Wysyła do serwera komunikat o akceptacji propozycji martwych grup.
     *
     * @throws IOException przy błędzie zapisu.
     */
    public void sendAccept() throws IOException {
        toServer.writeInt(Protocol.ACCEPT_PROPOSAL);
        toServer.flush();
    }

    /**
     * Wysyła do serwera propozycję martwych grup w fazie negocjacji.
     *
     * @param currentProposal lista punktów oznaczonych jako martwe.
     * @throws IOException przy błędzie zapisu.
     */
    public void sendProposal(ArrayList<Point> currentProposal) throws IOException {
        toServer.writeInt(Protocol.SEND_PROPOSAL);
        toServer.writeInt(currentProposal.size());

        for (Point p : currentProposal) {
            toServer.writeInt(p.x);
            toServer.writeInt(p.y);
        }

        toServer.flush();
    }

    /**
     * Odbiera od serwera propozycję martwych grup przeciwnika.
     *
     * @return lista punktów proponowana jako martwe.
     * @throws IOException przy błędzie odczytu.
     */
    public ArrayList<Point> readProposal() throws IOException {
        ArrayList<Point> currentProposal = new ArrayList<>();
        int count = fromServer.readInt();
        int x;
        int y;

        for (int i = 0; i < count; i++) {
            x = fromServer.readInt();
            y = fromServer.readInt();
            currentProposal.add(new Point(x, y));
        }

        return currentProposal;
    }

    /**
     * Wysyła do serwera komunikat o poddaniu gry (SURRENDER).
     *
     * @throws IOException przy błędzie zapisu.
     */
    public void sendSurrenderMessage() throws IOException {
        toServer.writeInt(Protocol.SURRENDER);
        toServer.flush();
    }

    /**
     * Wysyła do serwera komunikat o wyjściu z gry (QUIT).
     *
     * @throws IOException przy błędzie zapisu.
     */
    public void sendQuitMessage() throws IOException {
        toServer.writeInt(Protocol.QUIT);
        toServer.flush();
    }

    /**
     * Wysyła współrzędne ruchu (postawienia kamienia) do serwera.
     *
     * @param coordinates tablica dwuelementowa [x, y].
     * @throws IOException przy błędzie zapisu.
     */
    public void sendSpaceInformation(int[] coordinates) throws IOException {
        toServer.writeInt(Protocol.MOVE);
        toServer.writeInt(coordinates[0]);
        toServer.writeInt(coordinates[1]);
        toServer.flush();
    }

    /**
     * Odczytuje typ przychodzącego komunikatu (nagłówek protokołu).
     *
     * @return kod komunikatu (zgodny ze stałymi w klasie Protocol).
     * @throws IOException przy błędzie odczytu.
     */
    public int readMessage() throws IOException {
        return fromServer.readInt();
    }

    /**
     * Odbiera pełny stan planszy od serwera i aktualizuje przekazany obiekt Board.
     *
     * @param board obiekt planszy do zaktualizowania.
     * @throws IOException przy błędzie odczytu.
     */
    public void receiveBoardFromServer(Board board) throws IOException {
        Protocol.receiveBoard(board, fromServer);
    }

    /**
     * Odbiera aktualną liczbę jeńców obu graczy.
     *
     * @return tablica int[], gdzie [0] to jeńcy czarnego, a [1] to jeńcy białego.
     * @throws IOException przy błędzie odczytu.
     */
    public int[] getCaptures() throws IOException {
        int black = fromServer.readInt();
        int white = fromServer.readInt();

        return new int[]{black, white};
    }

    /**
     * Odbiera współrzędne ruchu wykonanego przez przeciwnika.
     *
     * @return tablica int[] zawierająca [x, y].
     * @throws IOException przy błędzie odczytu.
     */
    public int[] getCoordinates() throws IOException {
        int x = fromServer.readInt();
        int y = fromServer.readInt();

        return new int[]{x, y};
    }

    /**
     * Wysyła wiadomość czatu do serwera.
     *
     * @param message treść wiadomości.
     * @throws IOException przy błędzie zapisu.
     */
    public void sendMessage(String message) throws IOException {
        toServer.writeInt(Protocol.MESSAGE);
        toServer.writeUTF(message);
        toServer.flush();
    }

    /**
     * Odbiera wiadomość czatu od serwera.
     *
     * @return treść wiadomości.
     * @throws IOException przy błędzie odczytu.
     */
    public String receiveMessage() throws IOException {
        return fromServer.readUTF();
    }
    public ArrayList<GameRecordDTO> fetchGameList() throws IOException {
        ArrayList<GameRecordDTO> games = new ArrayList<>();
        Socket historySocket = new Socket("localhost", Protocol.Port);
        DataOutputStream out = new DataOutputStream(historySocket.getOutputStream());
        DataInputStream in = new DataInputStream(historySocket.getInputStream());
        try {
            out.writeInt(Protocol.HISTORY_MODE);
            out.flush();
            int count = in.readInt();
            System.out.println("Pobieranie historii: znaleziono " + count + " gier.");

            for (int i = 0; i < count; i++) {
                long id = in.readLong();
                String date = in.readUTF();
                String winner = in.readUTF();
                int bScore = in.readInt();
                int wScore = in.readInt();
                String type = in.readUTF();
                String history = in.readUTF(); 

                String scoreStr = "B:" + bScore + " | W:" + wScore;
                games.add(new GameRecordDTO(id, date, winner, scoreStr, type, history));
            }
        } finally {
            historySocket.close(); 
        }
        
        return games;
    }
}