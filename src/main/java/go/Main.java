package go;

import go.client.GoClient;
import go.server.GoServer;
import go.ui.ConsoleView;
import go.ui.GameView;
import go.ui.fx.MainFx; 
import javafx.application.Application;

public class Main {
    public static void main(String[] args) {
        // przy podaniu argumentu "server" uruchamiamy serwer
        if (args.length > 0 && args[0].equalsIgnoreCase("server")) {
            new GoServer(); 
        } 
        else if (args.length > 0 && args[0].equalsIgnoreCase("console")) {
        //consoleView mozna wyrzucic, zostawaim narazie w razie bledow
            GameView view = new ConsoleView();
            GoClient client = new GoClient(view);
            client.connect();
        }
        // w innym przypadku uruchamiamy klienta GUI
        else {
            Application.launch(MainFx.class, args);
        }
    }
}