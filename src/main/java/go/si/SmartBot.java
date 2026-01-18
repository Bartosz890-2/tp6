package go.si;

import go.logic.Board;
import go.logic.GameMechanics;
import go.logic.Stone;

import java.awt.*;
import java.util.ArrayList;



public class SmartBot implements BotStrategy{
    private SmartBotHeuristics smartBotHeuristics;
    private final GameMechanics mechanics;
    private final Board sandboxBoard = new Board(19);

    public SmartBot(GameMechanics mechanics) {
        this.mechanics= mechanics;
        smartBotHeuristics = new SmartBotHeuristics(mechanics);
    }


    @Override
    public Point calculateBestMove(Board board, Stone color) {
        return runSymulationAndChooseBestPoint(board, color);
    }

    //dla pobranych najlepszych punktow przeprowadzamy symulacje ruchow w przod i wybieramy punkt, ktory da nam
    //najlepszy bilans punktowy
    private Point runSymulationAndChooseBestPoint(Board board, Stone color) {
        ArrayList<CandidateRecord> candidates = smartBotHeuristics.findBestCandidates(board, color);
        if (candidates.isEmpty()) return null;
        int bestCandidateIndex = 0;
        ArrayList<Double> scoreBalance = new ArrayList<Double>();
        int i = 0;

        for (CandidateRecord candidateRecord : candidates) {
            scoreBalance.add(getBestOpponentRespondScore(board, candidateRecord, color));
            if (scoreBalance.get(i) >= scoreBalance.get(bestCandidateIndex)) {
                bestCandidateIndex = i;
            }
            i++;
        }

        if (scoreBalance.get(bestCandidateIndex) < 0) return null;

        return candidates.get(bestCandidateIndex).point();
    }

    //symulacja dla jednego punktu, zwracamy bilans (zysk - koszt)
    private double getBestOpponentRespondScore(Board board, CandidateRecord candidateRecord, Stone color) {
        board.copyBoard(sandboxBoard);
        ArrayList<CandidateRecord> opponentCandidates = new ArrayList<>();

        sandboxBoard.setField(candidateRecord.point().x, candidateRecord.point().y, color);
        sandboxBoard.setLastMove(candidateRecord.point().x, candidateRecord.point().y);

        opponentCandidates = smartBotHeuristics.findBestCandidates(sandboxBoard, color.opponent());
        if (opponentCandidates.isEmpty()) {
            return 0;
        }
        return candidateRecord.score() - opponentCandidates.get(0).score();
    }
}
