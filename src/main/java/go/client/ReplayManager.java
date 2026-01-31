package go.client;

import java.util.ArrayList;
import java.util.List;

import go.logic.Board;
import go.logic.GameMechanics;
import go.logic.Stone;
public class ReplayManager {
    private final List<String> moveCommands;
    private int currentMoveIndex=0;
    private final GameMechanics mechanics;
    private final Board replayBoard;
    public ReplayManager(String historyLog) {
        this.replayBoard = new Board(19);
        this.mechanics = new GameMechanics();
        this.moveCommands = parseHistory(historyLog);
    }
    private List<String> parseHistory(String history) {
        List<String> commands = new ArrayList<>();
        if (history == null || history.isEmpty()) {
            return commands;
        }
        String[] parts = history.split(";");
        for (String part : parts) {
            if (!part.trim().isEmpty()) {
                commands.add(part.trim());
            }
        }
        return commands;
    }
    public boolean next() {
        if (currentMoveIndex >= moveCommands.size()) {
            return false;
        }

        String command = moveCommands.get(currentMoveIndex);
        applyCommandToBoard(command);
        currentMoveIndex++;
        return true;
    }
    public boolean previous() {
        if (currentMoveIndex <= 0) {
            return false;
        }

        currentMoveIndex--;
        clearBoard();
        for (int i = 0; i < currentMoveIndex; i++) {
            applyCommandToBoard(moveCommands.get(i));
        }
        return true;
    }
    public void resetToStart() {
        currentMoveIndex = 0;
        clearBoard();
    }
    public void jumpToEnd(){
        resetToStart();
        for(String cmd: moveCommands){
            applyCommandToBoard(cmd);
        }
        currentMoveIndex=moveCommands.size();
    }
    private void clearBoard() {
        for(int x=0; x<replayBoard.getSize(); x++) {
            for(int y=0; y<replayBoard.getSize(); y++) {
                replayBoard.setField(x, y, Stone.EMPTY);
            }
        }
        mechanics.blackCaptures = 0;
        mechanics.whiteCaptures = 0;
    }
    private void applyCommandToBoard(String command) {
        char colorChar = command.charAt(0);
        Stone color = (colorChar == 'B') ? Stone.BLACK : Stone.WHITE;
        int startBracket = command.indexOf('[');
        int endBracket = command.indexOf(']');
        
        if (startBracket == -1 || endBracket == -1) return;
        
        String content = command.substring(startBracket + 1, endBracket);
        if (content.equals("PASS")) {
            System.out.println("Replay: " + color + " PASS");
        } 
        else if (content.equals("SURRENDER") || content.equals("QUIT")) {
            System.out.println("Replay: " + color + " zakończył grę (" + content + ")");
        } 
        else {
            int[] coords = TranslateCoordinate.translate(content);
            
            if (coords != null) {
                int x = coords[0];
                int y = coords[1];
                replayBoard.setField(x, y, color);
                mechanics.CheckCaptures(replayBoard, x, y, color);
            }
        }
    }
    public Board getBoard() {
        return replayBoard;
    }
    
    public int getCurrentMoveIndex() {
        return currentMoveIndex;
    }
    
    public int getTotalMoves() {
        return moveCommands.size();
    }
}
