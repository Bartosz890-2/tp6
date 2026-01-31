package go.ui.fx;

import go.client.ReplayManager;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

public class ReplayController {

    private final ReplayManager replayManager;
    private final Stage stage;
    private final BoardCanvas canvas;
    private final SidePanel sidePanel;

    public ReplayController(String movesHistory, String gameTitle) {
        this.replayManager = new ReplayManager(movesHistory);
        
        this.stage = new Stage();
        this.canvas = new BoardCanvas();
        this.canvas.setWidth(700);
        this.canvas.setHeight(700);
        
        this.sidePanel = new SidePanel();
        this.sidePanel.toggleReplayMode(true);

        setupEvents();
        updateView();

        BorderPane root = new BorderPane();
        root.setCenter(canvas);
        root.setRight(sidePanel);

        Scene scene = new Scene(root);
        try {
            scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
        } catch (Exception e)  {}

        stage.setScene(scene);
        stage.setTitle("Replay: " + gameTitle);
        stage.setResizable(false);
        stage.show();
    }

    private void setupEvents() {
        sidePanel.getNextBtn().setOnAction(e -> {
            if (replayManager.next()) {
                updateView();
                sidePanel.addLog("Ruch " + replayManager.getCurrentMoveIndex() + "/" + replayManager.getTotalMoves());
            } else {
                sidePanel.addLog("Koniec nagrania.");
            }
        });

        sidePanel.getPrevBtn().setOnAction(e -> {
            if (replayManager.previous()) {
                updateView();
                sidePanel.addLog("Cofnięto do ruchu " + replayManager.getCurrentMoveIndex());
            } else {
                sidePanel.addLog("Początek gry.");
            }
        });

        sidePanel.getExitReplayBtn().setOnAction(e -> stage.close());
    }

    private void updateView() {
        canvas.draw(replayManager.getBoard());
    }
}