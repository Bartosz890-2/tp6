package go.server;

import java.io.DataInputStream;
import java.net.ServerSocket;
import java.net.Socket;
import go.logic.Protocol;

public class GoServer {

    // Poczekalnia dla gracza, który chce grać PvP
    private Socket waitingPlayer = null;

    public static void main(String[] args) {
        new GoServer();
    }

    public GoServer() {
        System.out.println("Serwer Go START na porcie " + Protocol.Port);

        try (ServerSocket serverSocket = new ServerSocket(Protocol.Port)) {
            while (true) {
                try {
                    // 1. Akceptujemy połączenie
                    Socket clientSocket = serverSocket.accept();
                    System.out.println("Nowe połączenie: " + clientSocket.getInetAddress());

                    // 2. CZYTAMY TRYB GRY (To jest kluczowe!)
                    DataInputStream input = new DataInputStream(clientSocket.getInputStream());
                    int gameType = input.readInt(); // Tu serwer dowiaduje się: 1=BOT, 2=PvP

                    if (gameType == 1) {
                        System.out.println(" -> Klient wybrał grę z BOTEM.");
                        BotGameSession botSession = new BotGameSession(clientSocket);
                        new Thread(botSession).start();
                    }
                    else if (gameType == 2) {
                        System.out.println(" -> Klient wybrał grę MULTIPLAYER.");
                        handleMultiplayerQueue(clientSocket);
                    }
                    else {
                        System.out.println("Nieznany tryb: " + gameType);
                        clientSocket.close();
                    }
                } catch (Exception e) {
                    System.out.println("Błąd połączenia: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleMultiplayerQueue(Socket clientSocket) {
        if (waitingPlayer == null) {
            waitingPlayer = clientSocket;
            System.out.println("    Gracz w poczekalni.");
        } else {
            System.out.println("    Mamy parę! Start PvP.");
            GameSession gameSession = new GameSession(waitingPlayer, clientSocket);
            new Thread(gameSession).start();
            waitingPlayer = null;
        }
    }
}