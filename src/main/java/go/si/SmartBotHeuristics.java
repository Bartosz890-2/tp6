package go.si;

import go.logic.Board;
import go.logic.Direction;
import go.logic.GameMechanics;
import go.logic.Stone;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
 * Klasa odpowiedzialna za statyczną ocenę sytuacji na planszy (funkcja oceny).
 * <p>
 * Wykorzystuje szereg heurystyk (lokalizacja, kształt, bezpieczeństwo, bicie, łączenie),
 * aby przypisać każdemu możliwemu ruchowi wartość punktową (score).
 * Klasa ta nie przeprowadza głębokiej symulacji (look-ahead), a jedynie ocenia
 * bezpośrednie skutki postawienia kamienia w danym punkcie.
 * <p>
 * Wagi zdefiniowane w tej klasie (np. {@code locationScoreWeight}, {@code captureScoreWeight})
 * definiują "osobowość" bota – czy gra agresywnie, czy terytorialnie.
 */
public class SmartBotHeuristics {
    private final GameMechanics mechanics;

    /** Pomocnicza plansza do symulacji pojedynczych ruchów (unikamy psucia głównej planszy). */
    private final Board sandboxBoard = new Board(19);

    // --- WAGI (WEIGHTS) ---
    // Definiują, jak ważne są poszczególne aspekty gry dla bota.

    /** Waga za zajęcie strategicznie dobrego miejsca (np. 3 linia). */
    private final static int locationScoreWeight = 25;
    /** Waga za zbicie kamieni przeciwnika. */
    private final static int captureScoreWeight = 40;
    /** Waga za bezpieczeństwo własnej grupy (liczba oddechów). */
    private final static int groutSafeScoreWeight = 15;
    /** Waga za tworzenie dobrych kształtów (i unikanie złych). */
    private final static int shapeScoreWeight = 20;
    /** Waga za "przyklejanie się" do przeciwnika (cięcie/blokowanie). */
    private final static int cutOpponentScoreWeight = 20;
    /** Waga za łączenie własnych grup. */
    private final static int connectOwnGroupScoreWeight = 5;

    /** Maksymalna liczba najlepszych ruchów przekazywana do dalszej symulacji w SmartBot. */
    private final static int bestCandidatesNumber = 10;

    // --- PUNKTY ZA KSZTAŁT (SHAPE BONUSES/PENALTIES) ---

    private final static int eyeShapeBonus = 10;
    private final static int tigerBonus = 5;
    /** Kara za tworzenie ciężkiego kształtu ("klucha"). */
    private final static int dumplingPenalty = -25;
    /** Kara za tworzenie pustego trójkąta (bardzo nieefektywny kształt). */
    private final static int triangleShapePenalty = -8;

    /** Punkty za odległość od krawędzi (indeks 0 = krawędź, indeks 2 = 3 linia/najlepsza). */
    private final static double[] boardLinesPoints = {-1, 0, 2, 1, 0.5};
    /** Punkty w zależności od liczby oddechów (indeks 0 = 0 oddechów/śmierć). */
    private final static double[] groupLibertiesPoints = {-1000,-30, -5, 10, 20};
    /** Punkty za liczbę połączonych grup (indeks 0=0, 1=wydłużanie, 2=łączenie). */
    private final static double[] groupConnectionPoints = {0, 10, 20, 30, 50};


    /**
     * Tworzy instancję heurystyk.
     * @param mechanics silnik zasad gry.
     */
    public SmartBotHeuristics(GameMechanics mechanics) {
        this.mechanics = mechanics;
    }

    /**
     * Skanuje całą planszę i wybiera najlepsze możliwe ruchy (kandydatów).
     * <p>
     * Algorytm:
     * <ol>
     * <li>Dla każdego pola na planszy oblicza {@code score}.</li>
     * <li>Odrzuca ruchy z tragicznym wynikiem (poniżej -100).</li>
     * <li>Sortuje kandydatów malejąco po wyniku.</li>
     * <li>Weryfikuje legalność ruchu (np. czy nie łamie zasady KO) dla czołówki.</li>
     * <li>Zwraca listę {@link #bestCandidatesNumber} najlepszych, zweryfikowanych kandydatów.</li>
     * </ol>
     *
     * @param board aktualny stan planszy.
     * @param color kolor, dla którego szukamy ruchów.
     * @return lista obiektów {@link CandidateRecord} gotowa do symulacji.
     */
    public ArrayList<CandidateRecord> findBestCandidates(Board board, Stone color) {
        // Point[] candidates = new Point[10]; // (Nieużywane)
        ArrayList<CandidateRecord> bestCandidates = new ArrayList<>(19*19);
        ArrayList<CandidateRecord> verifiedCandidates = new ArrayList<>(bestCandidatesNumber);

        // 1. Ocena każdego pola
        for (int x = 0; x < board.getSize(); x++) {
            for (int y = 0; y < board.getSize(); y++) {
                double score = calculatePointScore(board, new Point(x, y), color);
                // Filtr wstępny - odrzucamy ruchy beznadziejne (np. samobójstwa)
                if (score > -100) {
                    bestCandidates.add(new CandidateRecord(new Point(x,y), score));
                    // Sortowanie na bieżąco (można zoptymalizować sortując raz na koniec, ale przy 19x19 jest ok)
                    bestCandidates.sort((c1, c2) -> Double.compare(c2.score(), c1.score()));
                }
            }
        }

        // 2. Weryfikacja legalności (tylko dla najlepszych, żeby oszczędzić CPU)
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

    /**
     * Agreguje wyniki wszystkich cząstkowych heurystyk dla danego punktu.
     * <p>
     * Wzór: Suma (Wynik_Heurystyki * Waga_Heurystyki).
     *
     * @param board plansza.
     * @param point badany punkt.
     * @param color kolor gracza.
     * @return sumaryczna ocena ruchu.
     */
    private double calculatePointScore(Board board, Point point, Stone color) {
        return (calculateCaptureScore(board, point, color) * captureScoreWeight +
                calculateLocationScore(board, point, color) * locationScoreWeight +
                calculateGroupSafeScore(board, point, color) * groutSafeScoreWeight +
                calculateShapeScore(board, point, color) * shapeScoreWeight +
                calculateCutOpponentGroupScore(board, point, color) * cutOpponentScoreWeight +
                calculateConnectOwnGroupScore(board, point, color) * connectOwnGroupScoreWeight);
    }

    /**
     * Ocenia lokalizację na planszy.
     * Preferuje 3. linię (balans między terytorium a wpływem).
     * Unika 1. linii (krawędzi), chyba że jest to konieczne.
     */
    private double calculateLocationScore(Board board, Point point, Stone color) {
        int distance = Math.min(point.x, point.y);
        if (distance > 10) distance = board.getSize() - distance;

        // Spłaszczamy środek planszy (wszystko >= 4 linia ma taką samą wartość jak 4 linia)
        // Dzięki temu bot woli 3 linię, a potem centrum.
        if (distance >= 4) distance = 4;

        return boardLinesPoints[distance];
    }

    /**
     * Sprawdza, czy ruch prowadzi do zbicia kamieni przeciwnika.
     * Symuluje ruch na {@code sandboxBoard} i sprawdza zmianę w liczniku jeńców.
     */
    private double calculateCaptureScore(Board board, Point point, Stone color) {
        int score = 0;
        board.copyBoard(sandboxBoard);

        if (color == Stone.BLACK) {
            score = mechanics.blackCaptures;
            mechanics.CheckCaptures(sandboxBoard, point.x, point.y, color);
            score = mechanics.blackCaptures - score;
            // Cofamy zmiany w liczniku globalnym (bo to tylko symulacja)
            mechanics.subtractFromBlackCaptures(score);
        }
        else if (color == Stone.WHITE) {
            score = mechanics.whiteCaptures;
            mechanics.CheckCaptures(sandboxBoard, point.x, point.y, color);
            score = mechanics.whiteCaptures - score;
            mechanics.subtractFromWhiteCaptures(score);
        }
        return score;
    }

    /**
     * Ocenia bezpieczeństwo grupy, która powstanie po wykonaniu ruchu.
     * Sprawdza liczbę oddechów (liberties).
     * <p>
     * - Mało oddechów (1-2) -> Duża kara (ryzyko atari/śmierci).
     * - Dużo oddechów (>=4) -> Premia (stabilna grupa).
     */
    private double calculateGroupSafeScore(Board board, Point point, Stone color) {
        board.copyBoard(sandboxBoard);

        sandboxBoard.setField(point.x, point.y, color);

        Set<Point> groupLibertiesSandbox = new HashSet<>();
        Set<Point> groupMembersSandbox = new HashSet<>();

        mechanics.exploreGroup(sandboxBoard, point, color, groupMembersSandbox, groupLibertiesSandbox);

        int liberties = groupLibertiesSandbox.size();

        if (liberties >= 4) liberties = 4;

        return groupLibertiesPoints[liberties];
    }

    /**
     * Analizuje lokalny kształt tworzony przez kamienie (Pattern Matching).
     * <p>
     * Wykrywa:
     * <ul>
     * <li>Pusty trójkąt (Empty Triangle) - Kara.</li>
     * <li>Klucha (Dumpling/Heavy Shape) - Kara.</li>
     * <li>Paszcza tygrysa (Tiger's Mouth) - Premia (dobre połączenie).</li>
     * <li>Potencjalne oko (Eye Shape) - Premia.</li>
     * </ul>
     */
    private double calculateShapeScore(Board board, Point point, Stone color) {
        int score = 0;
        int neighbourTeammates = 0;
        int emptyFieldNeighbors = 0;

        // Sprawdzanie sąsiadów do wykrywania "Pustego Trójkąta"
        boolean up    = isMyStone(board, point.x, point.y - 1, color);
        boolean down  = isMyStone(board, point.x, point.y + 1, color);
        boolean left  = isMyStone(board, point.x - 1, point.y, color);
        boolean right = isMyStone(board, point.x + 1, point.y, color);

        // Detekcja Pustego Trójkąta (Bad Shape)
        // Lewy-Górny róg
        if (up && left && !isMyStone(board, point.x - 1, point.y - 1, color)) score += triangleShapePenalty;
        // Prawy-Górny róg
        if (up && right && !isMyStone(board, point.x + 1, point.y - 1, color)) score += triangleShapePenalty;
        // Lewy-Dolny róg
        if (down && left && !isMyStone(board, point.x - 1, point.y + 1, color)) score += triangleShapePenalty;
        // Prawy-Dolny róg
        if (down && right && !isMyStone(board, point.x + 1, point.y + 1, color)) score += triangleShapePenalty;

        // Analiza sąsiadów (krzyż)
        for (Direction d : Direction.values()) {
            int newX = point.x + d.getDx();
            int newY = point.y + d.getDy();

            if (board.isFieldOnBoard(newX, newY)) {
                if (board.getField(newX, newY) == color) {
                    neighbourTeammates++;
                }
                else if (board.getField(newX, newY) == Stone.EMPTY) {
                    emptyFieldNeighbors = 0;
                    // Sprawdzamy "sąsiadów sąsiada" żeby wykryć oko/tygrysa
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

        // Kara za "kluchę" (zbyt gęste upakowanie własnych kamieni - overconcentration)
        if (neighbourTeammates > 2) score += dumplingPenalty;
        return score;
    }

    /**
     * Premiuje ruchy, które "przyklejają się" do kamieni przeciwnika.
     * Jest to heurystyka agresywna, zachęcająca do walki w zwarciu i cięcia grup przeciwnika.
     */
    private double calculateCutOpponentGroupScore(Board board, Point point, Stone color) {
        int opponents = -1; // -1, żeby 0 sąsiadów dało wynik 0 (z Math.max)

        for (Direction d : Direction.values()) {
            int newX = point.x + d.getDx();
            int newY = point.y + d.getDy();

            if (board.isFieldOnBoard(newX, newY) && board.getField(newX, newY) == color.opponent()) opponents++;
        }

        return Math.max(0, opponents);
    }

    /**
     * Ocenia łączenie własnych grup.
     * <p>
     * Rozróżnia dwa przypadki:
     * <ol>
     * <li><b>Wydłużanie (Extending):</b> Dotykanie tylko jednej własnej grupy. Mała nagroda (1.0).
     * Zapobiega to tworzeniu "węży" bez powodu.</li>
     * <li><b>Łączenie (Connecting):</b> Dotykanie dwóch lub więcej ROZŁĄCZNYCH grup. Duża nagroda.
     * Jest to kluczowe zagranie strategiczne.</li>
     * </ol>
     */
    private double calculateConnectOwnGroupScore(Board board, Point point, Stone color) {
        int differentGroups = 0;

        // Zbiór, żeby zapamiętać, które grupy już policzyliśmy (żeby nie liczyć tej samej 2 razy)
        Set<Point> exploredFieldsSandbox = new HashSet<>();

        // Dummy sety potrzebne tylko do wywołania exploreGroup
        Set<Point> groupMembersSandbox = new HashSet<>();
        Set<Point> groupLibertiesSandbox = new HashSet<>();

        for (Direction d : Direction.values()) {
            int newX = point.x + d.getDx();
            int newY = point.y + d.getDy();

            if (board.isFieldOnBoard(newX, newY)) {
                Point newPoint = new Point(newX, newY);

                // Jeśli to nasz kamień i jeszcze nie badaliśmy tej konkretnej grupy...
                if (board.getField(newX, newY) == color && !exploredFieldsSandbox.contains(newPoint)) {
                    differentGroups++;

                    // Pobieramy całą grupę, żeby oznaczyć jej kamienie jako "zaliczone"
                    groupMembersSandbox.clear();
                    groupLibertiesSandbox.clear();
                    mechanics.exploreGroup(board, newPoint, color, groupMembersSandbox, groupLibertiesSandbox);

                    // Dodajemy całą grupę do "zbadanych", żeby pętla for nie policzyła jej znowu
                    // (np. gdy dotykamy tej samej grupy z góry i z lewej)
                    exploredFieldsSandbox.addAll(groupMembersSandbox);
                }
            }
        }

        // --- PUNKTACJA ---

        // 0 grup = Samotny kamień (nie łączy niczego).
        if (differentGroups == 0) return 0;

        // 1 grupa = Wydłużanie (Extending).
        // To NIE JEST łączenie (Connecting). To po prostu budowanie ściany/węża.
        // Dajemy bardzo małą nagrodę, żeby bot nie robił tego bez powodu.
        if (differentGroups == 1) return 1.0;

        // 2+ grupy = Faktyczne łączenie (Connecting).
        // To jest bardzo ważny ruch strategiczny (np. łatanie dziury).
        int index = Math.min(differentGroups, groupConnectionPoints.length - 1);
        return groupConnectionPoints[index];
    }

    /**
     * Metoda pomocnicza sprawdzająca, czy na danym polu stoi kamień własnego koloru.
     * Obsługuje wyjścia poza tablicę (zwraca false).
     */
    private static boolean isMyStone(Board board, int x, int y, Stone color) {
        if (!board.isFieldOnBoard(x, y)) {
            return false;
        }
        return board.getField(x, y) == color;
    }

    // (Nieużywana metoda - Manhattan Distance)
    private int calculateManhattanDistance(Point lastMove, Point candidatePoint) {
        return Math.abs(candidatePoint.x - lastMove.x) + Math.abs(candidatePoint.y - lastMove.y);
    }
}