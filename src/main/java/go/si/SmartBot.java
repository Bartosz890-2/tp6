package go.si;

import go.logic.Board;
import go.logic.GameMechanics;
import go.logic.Stone;

import java.awt.*;
import java.sql.Array;
import java.util.ArrayList;



public class SmartBot implements BotStrategy{
    private SmartBotHeuristics smartBotHeuristics;
    private final GameMechanics mechanics;
    private final Board sandboxBoard = new Board(19);
    private int moveCounter = 0;

    public SmartBot(GameMechanics mechanics) {
        this.mechanics= mechanics;
        smartBotHeuristics = new SmartBotHeuristics(mechanics);
    }


    @Override
    public Point calculateBestMove(Board board, Stone color) {
        moveCounter++;
        return runSymulationAndChooseBestPoint(board, color);
    }

    //dla pobranych najlepszych punktow przeprowadzamy symulacje ruchow w przod i wybieramy punkt, ktory da nam
    //najlepszy bilans punktowy
    private Point runSymulationAndChooseBestPoint(Board board, Stone color) {
        ArrayList<CandidateRecord> candidates = smartBotHeuristics.findBestCandidates(board, color);

        if (candidates.isEmpty()) return null;

        double[] simulatedScores = new double[candidates.size()];

        int bestCandidateIndex = 0;
        double bestBalance = Double.NEGATIVE_INFINITY;

        for (int i = 0; i < candidates.size(); i++) {
            CandidateRecord candidate = candidates.get(i);

            double balance = getBestOpponentRespondScore(board, candidate, color);

            simulatedScores[i] = balance;

            if (balance > bestBalance) {
                bestBalance = balance;
                bestCandidateIndex = i;
            }
        }

        if (moveCounter > 15 && bestBalance < -10000) {
            System.out.println("Bot pasuje (Bilans: " + bestBalance + ", Ruch: " + moveCounter + ")");
            return null;
        }

        ArrayList<Integer> candidatesAlternativesIndices = new ArrayList<>();
        double tolerance = 2.0;

        for (int i = 0; i < candidates.size(); i++) {
            if (simulatedScores[i] >= bestBalance - tolerance) {
                candidatesAlternativesIndices.add(i);
            }
        }

        if (candidatesAlternativesIndices.isEmpty()) {
            return candidates.get(bestCandidateIndex).point();
        }
        else {
            int randomIndex = (int)(Math.random() * candidatesAlternativesIndices.size());

            int finalCandidateIndex = candidatesAlternativesIndices.get(randomIndex);
            return candidates.get(finalCandidateIndex).point();
        }
    }

    //symulacja dla jednego punktu, zwracamy bilans (zysk - koszt)
    private double getBestOpponentRespondScore(Board board, CandidateRecord candidateRecord, Stone color) {
        board.copyBoard(sandboxBoard);
        ArrayList<CandidateRecord> opponentCandidates;
//        ArrayList<CandidateRecord> candidates = new ArrayList<>();

        sandboxBoard.setField(candidateRecord.point().x, candidateRecord.point().y, color);
        sandboxBoard.setLastMove(candidateRecord.point().x, candidateRecord.point().y);

        opponentCandidates = smartBotHeuristics.findBestCandidates(sandboxBoard, color.opponent());
        if (opponentCandidates.isEmpty()) {
            return 0;
        }

        return candidateRecord.score() - opponentCandidates.get(0).score();
    }
}
