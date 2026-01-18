package go.si;

import go.logic.Board;
import go.logic.Stone;

import java.awt.*;

//interfejs dla mozliwosci podpiecia roznych botow o roznych heurystykach
public interface BotStrategy {
    public Point calculateBestMove(Board board, Stone color);
}
