package go.logic;

import java.awt.Point;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

/**
 * Klasa odpowiedzialna za logikę i mechanikę gry Go.
 * Przechowuje zasady dotyczące legalności ruchów (w tym zasady Ko i samobójstwa),
 * mechanizmy bicia kamieni, obliczania terytorium oraz punktacji.
 */
public class GameMechanics {

    /** Liczba kamieni zbitych przez gracza czarnego (czyli białych kamieni zdjętych z planszy). */
    public int blackCaptures = 0;

    /** Liczba kamieni zbitych przez gracza białego (czyli czarnych kamieni zdjętych z planszy). */
    public int whiteCaptures = 0;

    private int whiteTerritory = 0;
    private int blackTerritory = 0;

    /**
     * Kopia planszy z poprzedniego ruchu. Służy do weryfikacji zasady Ko
     * (zakaz doprowadzania do identycznego stanu planszy, jaki był turę wcześniej).
     */
    private Board recentMoveBoardCopy = new Board(19);

    /**
     * Kopia robocza planszy używana do symulacji przyszłego ruchu
     * w celu sprawdzenia jego legalności bez modyfikowania głównej planszy przed weryfikacją.
     */
    private Board boardAfterFutureMove = new Board(19);

    /**
     * Sprawdza, czy ruch w danym miejscu jest legalny zgodnie z zasadami gry Go.
     * Weryfikuje: granice planszy, czy pole jest puste, czy ruch powoduje bicie,
     * czy nie jest samobójstwem (chyba że bije) oraz czy nie narusza zasady Ko.
     * <p>
     * Jeśli ruch jest poprawny, metoda aktualizuje stan przekazanej planszy.
     *
     * @param board aktualna plansza gry.
     * @param x współrzędna X ruchu.
     * @param y współrzędna Y ruchu.
     * @param color kolor stawianego kamienia.
     * @return true, jeśli ruch jest możliwy i został wykonany; false w przeciwnym razie.
     */
    public boolean IsMovePossible(Board board, int x, int y, Stone color) {
        if (!board.isFieldOnBoard(x, y)) return false; // jezeli wskazane pole nie nalezy do planszy - zwracamy false
        if (board.getField(x, y) != Stone.EMPTY ) return false; //jezeli aktualnie lezy jakis kamien na wskazanym polu - zwracamy false

        // Kopiujemy planszę, aby zasymulować ruch
        board.copyBoard(boardAfterFutureMove);
        boardAfterFutureMove.setField(x, y, color);

        // Sprawdzamy, czy ten ruch zbija jakieś kamienie przeciwnika
        CheckCaptures(boardAfterFutureMove, x, y, color);

        // Sprawdzamy zasadę KO oraz zasadę samobójstwa
        if (!boardAfterFutureMove.equals(recentMoveBoardCopy) && !checkSuicide(boardAfterFutureMove, x, y, color)) {
            // Ruch jest legalny:
            // 1. Zapisujemy obecny stan jako "poprzedni" dla przyszłego sprawdzania KO
            board.copyBoard(recentMoveBoardCopy);
            // 2. Aktualizujemy główną planszę stanem po ruchu
            boardAfterFutureMove.copyBoard(board);
            return true;
        }
        return false;
    }

    /**
     * Sprawdza sąsiednie pola po postawieniu kamienia i usuwa grupy przeciwnika,
     * które utraciły wszystkie oddechy (zostały otoczone).
     *
     * @param board plansza, na której wykonano symulowany ruch.
     * @param x współrzędna X postawionego kamienia.
     * @param y współrzędna Y postawionego kamienia.
     * @param color kolor postawionego kamienia.
     */
    public void CheckCaptures(Board board, int x, int y, Stone color) {
        Set<Point> groupMembers = new HashSet<>();
        Set<Point> emptyGroupFields = new HashSet<>();
        Set<Point> processedOpponents = new HashSet<>();

        for (Direction d : Direction.values()) {
            int newX = x + d.getDx();
            int newY = y + d.getDy();

            if (board.isFieldOnBoard(newX, newY) && board.getField(newX, newY) == color.opponent()) {
                Point opponentPoint = new Point(newX, newY);
                if (!processedOpponents.contains(opponentPoint)) {
                    groupMembers.clear();
                    emptyGroupFields.clear();

                    // Badamy grupę przeciwnika
                    exploreGroup(board, opponentPoint, color.opponent(), groupMembers, emptyGroupFields);

                    // Jeśli grupa nie ma oddechów (emptyGroupFields jest puste), usuwamy ją
                    if (emptyGroupFields.isEmpty()) {
                        if (color == Stone.BLACK) blackCaptures += groupMembers.size();
                        else if (color == Stone.WHITE) whiteCaptures += groupMembers.size();
                        for (Point point : groupMembers) {
                            board.setField(point.x, point.y, Stone.EMPTY);
                        }
                        groupMembers.clear();
                    }
                    processedOpponents.addAll(groupMembers);
                }
            }
        }
    }

    /**
     * Algorytm BFS (przeszukiwanie wszerz) do znajdowania wszystkich kamieni należących do jednej grupy
     * oraz znajdowania ich "oddechów" (sąsiednich pustych pól).
     *
     * @param board plansza gry.
     * @param startPoint punkt startowy analizy (kamień).
     * @param color kolor kamieni w grupie.
     * @param groupMembers zbiór wyjściowy, do którego zostaną dodane wszystkie punkty należące do grupy.
     * @param emptyGroupFields zbiór wyjściowy, do którego zostaną dodane wszystkie unikalne oddechy grupy.
     */
    public void exploreGroup(Board board, Point startPoint, Stone color, Set<Point> groupMembers, Set<Point> emptyGroupFields) {
        Queue<Point> queue = new LinkedList<>();

        queue.add(startPoint);
        groupMembers.add(startPoint);

        while (!queue.isEmpty()) {
            Point current = queue.poll();
            groupMembers.add(current);

            for (Direction d : Direction.values()) {
                int newX = current.x + d.getDx();
                int newY = current.y + d.getDy();

                if (board.isFieldOnBoard(newX, newY)) {
                    Point newPoint = new Point(newX, newY);

                    if (board.getField(newX, newY) == color && !(groupMembers.contains(newPoint))) {
                        // Ten sam kolor -> część grupy
                        groupMembers.add(newPoint);
                        queue.add(newPoint);
                    }
                    else if (board.getField(newX, newY) == Stone.EMPTY) {
                        // Puste pole -> oddech
                        emptyGroupFields.add(newPoint);
                    }
                }
            }
        }
    }

    /**
     * Sprawdza, czy postawienie kamienia w danym miejscu jest ruchem samobójczym.
     * Samobójstwo to ruch, który zabiera ostatni oddech własnej grupie i nie powoduje bicia przeciwnika.
     *
     * @param board plansza po symulowanym ruchu (i ewentualnym usunięciu zbitych kamieni wroga).
     * @param x współrzędna X.
     * @param y współrzędna Y.
     * @param color kolor gracza.
     * @return true, jeśli ruch jest samobójczy (brak oddechów); false w przeciwnym razie.
     */
    private boolean checkSuicide(Board board, int x, int y, Stone color) {
        Set<Point> myGroup = new HashSet<>();
        Set<Point> myLiberties = new HashSet<>();

        // Badamy grupę kamienia, którego właśnie postawiliśmy
        exploreGroup(board, new Point(x, y), color, myGroup, myLiberties);

        // Jeśli zbiór oddechów jest pusty, to jest samobójstwo
        return myLiberties.isEmpty();
    }

    /**
     * Przelicza punkty terytorium dla obu graczy na podstawie obecnego stanu planszy.
     * Skanuje wszystkie puste pola i przydziela je odpowiedniemu graczowi.
     * Wypisuje wyniki na standardowe wyjście.
     *
     * @param board plansza gry.
     */
    public void calculateGameScore(Board board) {
        boolean[][] visited = new boolean[board.getSize()][board.getSize()];

        for (int x = 0; x < board.getSize(); x++) {
            for (int y = 0; y < board.getSize(); y++) {
                if (board.getField(x, y) == Stone.EMPTY && !visited[x][y]) {
                    calculateEmptyFieldPoints(board, new Point(x, y), visited);
                }
            }
        }

        System.out.println(whiteCaptures + " punkty bialego za zbicia");
        System.out.println(blackCaptures + " punkty czarnego za zbicia");
        System.out.println(whiteTerritory + " punkty bialego za terytorium");
        System.out.println(blackTerritory + " punkty czarnego za terytorium");
    }

    /**
     * Algorytm BFS analizujący spójny obszar pustych pól (terytorium).
     * Określa, czy terytorium należy do białego, czarnego, czy jest niczyje (dame),
     * sprawdzając z jakimi kamieniami graniczy dany obszar.
     *
     * @param board plansza gry.
     * @param point punkt startowy (puste pole).
     * @param explored tablica odwiedzonych pól.
     */
    private void calculateEmptyFieldPoints(Board board, Point point, boolean[][] explored) {
        whiteTerritory = 0;
        blackTerritory = 0;
        Queue<Point> queue = new LinkedList<>();
        queue.add(point);
        int currentTerritorySize = 0;

        boolean touchesWhite = false;
        boolean touchesBlack  = false;
        explored[point.x][point.y] = true;

        while (!queue.isEmpty()) {
            Point p = queue.poll();
            currentTerritorySize++;

            for (Direction d : Direction.values()) {
                int newX = p.x + d.getDx();
                int newY = p.y + d.getDy();

                Point neighbourPoint = new Point(newX, newY);

                if (board.isFieldOnBoard(newX, newY) && !explored[newX][newY]) {
                    Stone neighbourStone = board.getField(newX, newY);
                    if (neighbourStone == Stone.EMPTY) {
                        queue.add(neighbourPoint);
                        explored[newX][newY] = true;
                    }
                    else if (neighbourStone == Stone.BLACK) {
                        touchesBlack = true;
                    }
                    else if (neighbourStone == Stone.WHITE) {
                        touchesWhite = true;
                    }
                }
            }
        }

        // Terytorium liczy się tylko, jeśli dotyka kamieni wyłącznie jednego koloru
        if (touchesBlack && !touchesWhite) {
            blackTerritory += currentTerritorySize;
        }
        else if (touchesWhite && !touchesBlack) {
            whiteTerritory += currentTerritorySize;
        }
    }

    /**
     * Usuwa z planszy grupy kamieni oznaczone jako martwe w fazie negocjacji
     * i dolicza je do puli jeńców odpowiedniego gracza.
     *
     * @param board plansza gry.
     * @param deadGroups lista punktów (kamieni) do usunięcia.
     */
    public void takeOffDeadGroups(Board board, ArrayList<Point> deadGroups) {
        for (Point point : deadGroups) {
            if (board.getField(point.x, point.y) == Stone.BLACK) {
                blackCaptures ++;
            }
            else if (board.getField(point.x, point.y) == Stone.WHITE) {
                whiteCaptures ++;
            }

            board.setField(point.x, point.y, Stone.EMPTY);
        }
    }

    /**
     * Zwraca obliczone terytorium czarnego gracza.
     * @return liczba punktów za terytorium.
     */
    public int getBlackTerritory() {
        return blackTerritory;
    }

    /**
     * Zwraca obliczone terytorium białego gracza.
     * @return liczba punktów za terytorium.
     */
    public int getWhiteTerritory() {
        return whiteTerritory;
    }

    public void subtractFromWhiteCaptures(int score) {
        whiteCaptures -= score;
    }

    public void subtractFromBlackCaptures(int score) {
        blackCaptures -= score;
    }
}