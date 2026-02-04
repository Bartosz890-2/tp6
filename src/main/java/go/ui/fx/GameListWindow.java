package go.ui.fx;

import java.io.IOException;
import java.util.List;

import go.client.GameRecordDTO;
import go.client.NetworkConnection;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
// Okno pokazujace liste zapisanych gier, pozwalajace na odtworzenie wybranej partii
public class GameListWindow {
    // Metoda show tworzy nowe okno z tabela, w ktorej wyswietlane sa zapisane gry
    public void show() {
        Stage stage = new Stage();
        stage.setTitle("Historia Gier - Wybierz partię");

        TableView<GameRecordDTO> table = new TableView<>();
        TableColumn<GameRecordDTO, Long> colId = new TableColumn<>("ID");
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));

        TableColumn<GameRecordDTO, String> colDate = new TableColumn<>("Data");
        colDate.setCellValueFactory(new PropertyValueFactory<>("date"));
        colDate.setPrefWidth(150);

        TableColumn<GameRecordDTO, String> colType = new TableColumn<>("Tryb");
        colType.setCellValueFactory(new PropertyValueFactory<>("type"));

        TableColumn<GameRecordDTO, String> colWinner = new TableColumn<>("Zwycięzca");
        colWinner.setCellValueFactory(new PropertyValueFactory<>("winner"));

        TableColumn<GameRecordDTO, String> colScore = new TableColumn<>("Wynik");
        colScore.setCellValueFactory(new PropertyValueFactory<>("score"));
        colScore.setPrefWidth(120);

        table.getColumns().addAll(colId, colDate, colType, colWinner, colScore);

        Button playBtn = new Button("Odtwórz wybraną grę");
        playBtn.setDisable(true);
        playBtn.setStyle("-fx-font-size: 14px; -fx-padding: 10px;");
        table.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            playBtn.setDisable(newVal == null);
        });
        playBtn.setOnAction(e -> {
            GameRecordDTO selected = table.getSelectionModel().getSelectedItem();
            if (selected != null) {
                new ReplayController(selected.getMovesHistory(), "Gra #" + selected.getId());
            }
        });
        loadData(table);

        VBox layout = new VBox(10);
        layout.setPadding(new Insets(10));
        layout.getChildren().addAll(table, playBtn);

        Scene scene = new Scene(layout, 650, 450);
        stage.setScene(scene);
        stage.show();
    }

    private void loadData(TableView<GameRecordDTO> table) {
        new Thread(() -> {
            try {
                NetworkConnection net = new NetworkConnection();
                List<GameRecordDTO> games = net.fetchGameList();

                ObservableList<GameRecordDTO> data = FXCollections.observableArrayList(games);
                Platform.runLater(() -> table.setItems(data));
                
            } catch (IOException e) {
                Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Błąd");
                    alert.setHeaderText("Nie udało się pobrać historii");
                    alert.setContentText("Upewnij się, że serwer działa.\n" + e.getMessage());
                    alert.showAndWait();
                });
            }
        }).start();
    }
}