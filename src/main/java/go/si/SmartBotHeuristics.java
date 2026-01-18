package go.si;

import go.logic.Board;
import go.logic.Direction;
import go.logic.GameMechanics;
import go.logic.Stone;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class SmartBotHeuristics {
    private final GameMechanics mechanics;
    private final Board sandboxBoard = new Board(19);

    //stale wag uzywane do wyliczenia wartosci numerycznej danego pola
    private final static int locationScoreWeight = 5;
    private final static int captureScoreWeight = 30;
    private final static int groutSafeScoreWeight = 20;
    private final static int shapeScoreWeight = 10;
    private final static int cutOpponentScoreWeight = 10;
    private final static int connectOwnGroupScoreWeight = 15;

    //stala okreslajaca maksymalna liczbe kandydatow, ktorzy beda potem poddani symulacji
    private final static int bestCandidatesNumber = 10;

    //punkty za "dobre" i "zle" ksztalty (ksztalt oka, paszcza tygrysa, klucha, pusty trojkat)
    private final static int eyeShapeBonus = 10;
    private final static int tigerBonus = 5;
    private final static int dumplingPenalty = -10;
    private final static int triangleShapePenalty = -8;

    //stala okreslajaca liczbe punktow w zalezlnosci od lokalizacji postawionego pionka
    private final static double[] boardLinesPoints = {-1, 0, 2, 1, 0.5};
    //stala okreslajaca liczbe punktow w zaleznosci od liczby oddechow (swojej) grupy
    private final static double[] groupLibertiesPoints = {-1000,-30, -5, 10, 20};
    //stala okreslajaca liczbe punktow w zaleznosci od liczby polaczonych osobnych grup
    private final static double[] groupConnectionPoints = {0, 10, 20, 30, 50};

    private final Set<Point> groupMembersSandbox = new HashSet<>();
    private final Set<Point> groupLibertiesSandbox = new HashSet<>();
    private final Set<Point> exploredFieldsSandbox = new HashSet<>();
    private final ArrayList<CandidateRecord> bestCandidates = new ArrayList<>(19*19);
    private final ArrayList<CandidateRecord> verifiedCandidates = new ArrayList<>(bestCandidatesNumber);


    public SmartBotHeuristics(GameMechanics mechanics) {
        this.mechanics = mechanics;
    }

    //metoda sortujaca malejaco punkty wzgledem ich wyniku (score), i szukajaca 10 najlepszych kandydatow, dla
    //ktorych ruch jest legalny
    public ArrayList<CandidateRecord> findBestCandidates(Board board, Stone color) {
        Point[] candidates = new Point[10];

        for (int x = 0; x < board.getSize(); x++) {
            for (int y = 0; y < board.getSize(); y++) {
                double score = calculatePointScore(board, new Point(x, y), color);
                if (score > -100) {
                    bestCandidates.add(new CandidateRecord(new Point(x,y), score));
                    bestCandidates.sort((c1, c2) -> Double.compare(c2.score(), c1.score()));
                }
            }
        }

        for (CandidateRecord bestCandidate : bestCandidates) {
            board.copyBoard(sandboxBoard);
            if (mechanics.IsMovePossible(sandboxBoard, bestCandidate.point().x,
                    bestCandidate.point().y, color)) {
                verifiedCandidates.add(bestCandidate);
                if (verifiedCandidates.size() == bestCandidatesNumber) {
                    break;
                }
            }
        }

        return verifiedCandidates;
    }

    //metoda zwracajaca wynik pola (score), ktory jest wylicznay na podstawie stalych wag oraz
    // wynikow poszczegolnych heurystyk
    private double calculatePointScore(Board board, Point point, Stone color) {
        return (calculateCaptureScore(board, point, color) * captureScoreWeight +
                calculateLocationScore(board, point, color) * locationScoreWeight +
                calculateGroupSafeScore(board, point, color) * groutSafeScoreWeight +
                calculateShapeScore(board, point, color) * shapeScoreWeight +
                calculateCutOpponentGroupScore(board, point, color) * cutOpponentScoreWeight +
                calculateConnectOwnGroupScore(board, point, color) * connectOwnGroupScoreWeight);
    }

    //heurystyka obliczajaca wynik w zaleznosci od odleglosci od krawedzi planszy (najgorzej postawiony pionek
    //to jest przy krawedzi, najlepszy to w 3 linii liczac od krawedzi planszy)
    private double calculateLocationScore(Board board, Point point, Stone color) {
        int distance = Math.min(point.x, point.y);
        if (distance > 10) distance = board.getSize() - distance;
        if (distance >= 4) distance = 4;

        return boardLinesPoints[distance];
    }

    //heurystyka obliczajaca wynik w zaleznosci od zbitych pionkow przeciwnika
    private double calculateCaptureScore(Board board, Point point, Stone color) {
        int score = 0;
        board.copyBoard(sandboxBoard);

        if (color == Stone.BLACK) {
            score = mechanics.blackCaptures;
            mechanics.CheckCaptures(sandboxBoard, point.x, point.y, color);
            score = mechanics.blackCaptures - score;
            mechanics.subtractFromBlackCaptures(score);
        }
        else if (color == Stone.WHITE) {
            score = mechanics.whiteCaptures;
            mechanics.CheckCaptures(sandboxBoard, point.x, point.y, color);
            score = mechanics.whiteCaptures - score;
            mechanics.subtractFromBlackCaptures(score);
        }
        return score;
    }

    //heurystyka obliczajaca wynik w zaleznosci od sposobu ohrony grupy wlasnej
    private double calculateGroupSafeScore(Board board, Point point, Stone color) {
        board.copyBoard(sandboxBoard);

        sandboxBoard.setField(point.x, point.y, color);

        groupLibertiesSandbox.clear();
        groupMembersSandbox.clear();

        mechanics.exploreGroup(sandboxBoard, point, color, groupMembersSandbox, groupLibertiesSandbox);

        int liberties = groupLibertiesSandbox.size();

        if (liberties >= 4) liberties = 4;

        return groupLibertiesPoints[liberties];
    }

    // heurystyka obliczajaca wynik w zaleznosci od ulozonego ksztaltu (sa dobre i zle ksztalty)
    private double calculateShapeScore(Board board, Point point, Stone color) {
        int score = 0;
        int neighbourTeammates = 0;
        int emptyFieldNeighbors = 0;

        //zmienne pomocnicze do sprawdzania ksztaltu pustego trojkata

        boolean up    = isMyStone(board, point.x, point.y - 1, color);
        boolean down  = isMyStone(board, point.x, point.y + 1, color);
        boolean left  = isMyStone(board, point.x - 1, point.y, color);
        boolean right = isMyStone(board, point.x + 1, point.y, color);


        // Lewy-Górny róg
        if (up && left && !isMyStone(board, point.x - 1, point.y - 1, color)) score += triangleShapePenalty;

        // Prawy-Górny róg
        if (up && right && !isMyStone(board, point.x + 1, point.y - 1, color)) score += triangleShapePenalty;

        // Lewy-Dolny róg
        if (down && left && !isMyStone(board, point.x - 1, point.y + 1, color)) score += triangleShapePenalty;

        // Prawy-Dolny róg
        if (down && right && !isMyStone(board, point.x + 1, point.y + 1, color)) score += triangleShapePenalty;

        for (Direction d : Direction.values()) {
            int newX = point.x + d.getDx();
            int newY = point.y + d.getDy();

            if (board.isFieldOnBoard(newX, newY)) {
                if (board.getField(newX, newY) == color) {
                    neighbourTeammates++;
                }
                else if (board.getField(newX, newY) == Stone.EMPTY) {
                    emptyFieldNeighbors = 0;
                    for (Direction direction : Direction.values()) {
                        int emptyFieldNeighbourX = newX + direction.getDx();
                        int emptyFieldNeighbourY = newY + direction.getDy();

                        if (board.isFieldOnBoard(emptyFieldNeighbourX, emptyFieldNeighbourY) &&
                                board.getField(emptyFieldNeighbourX, emptyFieldNeighbourY) == color) emptyFieldNeighbors++;
                    }

                    if (emptyFieldNeighbors == 3) score += tigerBonus;
                    if (emptyFieldNeighbors == 4) score += eyeShapeBonus;
                }
            }
        }

        if (neighbourTeammates > 2) score += dumplingPenalty;
        return score;
    }

    //heurystyka obliczajaca wynik w zaleznosci od liczby przecietych grup przeciwnika (nie pozwalamy na
    //potencjalne polaczenie jego grup ze soba)
    private double calculateCutOpponentGroupScore(Board board, Point point, Stone color) {
        int opponents = -1;

        for (Direction d : Direction.values()) {
            int newX = point.x + d.getDx();
            int newY = point.y + d.getDy();

            if (board.isFieldOnBoard(newX, newY) && board.getField(newX, newY) == color.opponent()) opponents++;
        }

        return Math.max(0, opponents);
    }

    //heurystyka obliczajaca wynik w zaleznosci od liczby polaczonych ze soba wlasnych (osobnych) grup
    private double calculateConnectOwnGroupScore(Board board, Point point, Stone color) {
        int score = 0;
        int differentGroups = 0;
        groupLibertiesSandbox.clear();
        exploredFieldsSandbox.clear();
        groupMembersSandbox.clear();

        for (Direction d : Direction.values()) {
            int newX = point.x + d.getDx();
            int newY = point.y + d.getDy();

            if (board.isFieldOnBoard(newX, newY)) {
                Point newPoint = new Point(newX, newY);
                if (board.getField(newX, newY) == color && !exploredFieldsSandbox.contains(newPoint)) {
                    differentGroups++;
                    mechanics.exploreGroup(board, newPoint, color,
                            groupMembersSandbox, groupLibertiesSandbox);
                    exploredFieldsSandbox.addAll(groupMembersSandbox);
                    score += groupMembersSandbox.size();
                    groupMembersSandbox.clear();
                }
            }
        }

        return groupConnectionPoints[differentGroups] + (score * groupConnectionPoints[differentGroups])/10;
    }

    //metoda pomocnicza do okreslenia czy na wskazanych wspolrzednych lezy pionek bota
    private static boolean isMyStone(Board board, int x, int y, Stone color) {
        if (!board.isFieldOnBoard(x, y)) {
            return false;
        }
        return board.getField(x, y) == color;
    }

    //na razie nie uzywam tego
    private int calculateManhattanDistance(Point lastMove, Point candidatePoint) {
        return Math.abs(candidatePoint.x - lastMove.x) + Math.abs(candidatePoint.y - lastMove.y);
    }
}
