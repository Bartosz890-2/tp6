package go.server;

import java.io.DataOutputStream;
import java.net.ServerSocket;
import java.net.Socket;

import go.logic.Protocol;

/**
 * Główna klasa serwera gry Go.
 * Odpowiada za nasłuchiwanie na porcie TCP, akceptowanie przychodzących połączeń od klientów
 * oraz parowanie ich w dwuosobowe sesje gry.
 * <p>
 * Dla każdej pary graczy uruchamiany jest osobny wątek {@link GameSession},
 * co pozwala na obsługę wielu rozgrywek jednocześnie.
 */
public class GoServer {

    /**
     * Punkt wejścia aplikacji serwera.
     * Tworzy instancję serwera, co skutkuje rozpoczęciem nasłuchiwania.
     *
     * @param args argumenty wiersza poleceń (nieużywane).
     */
    public static void main(String[] args){
        new GoServer();
    }

    /**
     * Konstruktor uruchamiający serwer.
     * Otwiera gniazdo serwera na porcie zdefiniowanym w {@link Protocol#Port}
     * i wchodzi w nieskończoną pętlę oczekiwania na graczy.
     * <p>
     * Algorytm parowania:
     * <ol>
     * <li>Zaaakceptuj połączenie pierwszego gracza.</li>
     * <li>Wyślij mu identyfikator {@link Protocol#Player1}.</li>
     * <li>Zaaakceptuj połączenie drugiego gracza.</li>
     * <li>Wyślij mu identyfikator {@link Protocol#Player2}.</li>
     * <li>Utwórz obiekt {@link GameSession} dla tej pary i uruchom go w nowym wątku.</li>
     * </ol>
     */
    public GoServer(){
        //utworzenie gniazda serwera na określonym porcie
        try(ServerSocket serverSocket = new ServerSocket(Protocol.Port)) {
            while(true){
                System.out.println("Oczekiwanie na graczy");

                //oczekiwanie na pierwszego gracza
                Socket p1 = serverSocket.accept();
                System.out.println("Gracz 1 dołączył (" + p1.getInetAddress() + ")");
                new DataOutputStream(p1.getOutputStream()).writeInt(Protocol.Player1);

                //oczekiwanie na drugiego gracza
                Socket p2 = serverSocket.accept();
                System.out.println("Gracz 2 dołączył (" + p2.getInetAddress() + ")");
                new DataOutputStream(p2.getOutputStream()).writeInt(Protocol.Player2);

                //utworzenie sesji gry dla pary graczy
                GameSession game = new GameSession(p1, p2);
                new Thread(game).start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}