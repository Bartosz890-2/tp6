package go.si;

import go.logic.Board;
import go.logic.GameMechanics;
import go.logic.Stone;

import java.awt.*;
import java.util.ArrayList;

/**
 * Główna klasa implementująca logikę Sztucznej Inteligencji (AI) dla gry Go.
 * <p>
 * Bot działa w oparciu o dwuetapowy proces decyzyjny:
 * <ol>
 * <li><b>Selecja kandydatów:</b> Używa {@link SmartBotHeuristics} do wyłonienia kilku najbardziej obiecujących ruchów na podstawie statycznej oceny planszy.</li>
 * <li><b>Symulacja (Look-ahead):</b> Dla każdego kandydata wykonuje symulację ruchu w przód, sprawdzając najlepszą możliwą odpowiedź przeciwnika.</li>
 * </ol>
 * Implementuje interfejs {@link BotStrategy}, dzięki czemu może być łatwo podmieniany w serwerze gry.
 */
public class SmartBot implements BotStrategy {

    /** Obiekt odpowiedzialny za obliczanie heurystycznej wartości punktów na planszy. */
    private SmartBotHeuristics smartBotHeuristics;

    /** Silnik zasad gry, używany do sprawdzania legalności ruchów. */
    private final GameMechanics mechanics;

    /** Kopia planszy używana do przeprowadzania symulacji bez ingerencji w rzeczywistą rozgrywkę. */
    private final Board sandboxBoard = new Board(19);

    /** Licznik ruchów bota, używany do określania fazy gry (np. unikanie pasowania na samym początku). */
    private int moveCounter = 0;

    /**
     * Tworzy nową instancję bota.
     *
     * @param mechanics instancja mechaniki gry, niezbędna do walidacji ruchów i analizy planszy.
     */
    public SmartBot(GameMechanics mechanics) {
        this.mechanics = mechanics;
        smartBotHeuristics = new SmartBotHeuristics(mechanics);
    }

    /**
     * Główna metoda interfejsu strategii. Oblicza najlepszy ruch dla danego koloru.
     *
     * @param board aktualny stan planszy.
     * @param color kolor kamieni bota.
     * @return współrzędne najlepszego ruchu lub {@code null} w przypadku pasowania.
     */
    @Override
    public Point calculateBestMove(Board board, Stone color) {
        moveCounter++;
        return runSymulationAndChooseBestPoint(board, color);
    }

    /**
     * Przeprowadza symulację dla listy najlepszych kandydatów i wybiera ruch ostateczny.
     * <p>
     * Metoda ta realizuje kilka kluczowych funkcji:
     * <ul>
     * <li>Pobiera listę kandydatów z {@link SmartBotHeuristics}.</li>
     * <li>Dla każdego kandydata oblicza bilans (Mój Zysk - Zysk Przeciwnika w następnym ruchu).</li>
     * <li>Sprawdza warunek poddania się (jeśli bilans jest krytycznie niski).</li>
     * <li>Stosuje "Fuzzy Logic": zamiast zawsze wybierać najlepszy ruch, losuje jeden z ruchów,
     * które mieszczą się w granicy tolerancji (np. 2 pkt różnicy od najlepszego). Zapobiega to pętlom i przewidywalności.</li>
     * </ul>
     *
     * @param board aktualna plansza.
     * @param color kolor bota.
     * @return wybrany punkt lub null (pas).
     */
    private Point runSymulationAndChooseBestPoint(Board board, Stone color) {
        // 1. Pobranie wstępnych kandydatów na podstawie statycznej heurystyki
        ArrayList<CandidateRecord> candidates = smartBotHeuristics.findBestCandidates(board, color);

        if (candidates.isEmpty()) return null;

        // Tablica przechowująca wyniki symulacji dla każdego kandydata
        double[] simulatedScores = new double[candidates.size()];

        int bestCandidateIndex = 0;
        double bestBalance = Double.NEGATIVE_INFINITY;

        // 2. Symulacja: Sprawdzamy co zrobi przeciwnik w odpowiedzi na każdy nasz ruch
        for (int i = 0; i < candidates.size(); i++) {
            CandidateRecord candidate = candidates.get(i);

            double balance = getBestOpponentRespondScore(board, candidate, color);

            simulatedScores[i] = balance; // Zapamiętujemy wynik symulacji

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
            // Losujemy jeden z "dobrych" ruchów
            int randomIndex = (int)(Math.random() * candidatesAlternativesIndices.size());
            int finalCandidateIndex = candidatesAlternativesIndices.get(randomIndex);
            return candidates.get(finalCandidateIndex).point();
        }
    }

    /**
     * Symuluje jeden ruch w przód (głębokość 1) i oblicza bilans punktowy.
     * <p>
     * Algorytm:
     * <ol>
     * <li>Kopiuje planszę do sandboxa.</li>
     * <li>Wykonuje ruch kandydata.</li>
     * <li>Szuka najlepszego ruchu dla przeciwnika na nowej planszy.</li>
     * <li>Zwraca różnicę: {@code (Wynik Kandydata - Wynik Najlepszej Odpowiedzi Przeciwnika)}.</li>
     * </ol>
     * Dzięki temu bot unika ruchów, które wyglądają dobrze (dużo punktów), ale wystawiają go na natychmiastowy atak (Atari).
     *
     * @param board           oryginalna plansza.
     * @param candidateRecord kandydat do sprawdzenia.
     * @param color           kolor bota.
     * @return bilans punktowy ruchu.
     */
    private double getBestOpponentRespondScore(Board board, CandidateRecord candidateRecord, Stone color) {
        board.copyBoard(sandboxBoard);
        ArrayList<CandidateRecord> opponentCandidates;

        sandboxBoard.setField(candidateRecord.point().x, candidateRecord.point().y, color);
        sandboxBoard.setLastMove(candidateRecord.point().x, candidateRecord.point().y);

        opponentCandidates = smartBotHeuristics.findBestCandidates(sandboxBoard, color.opponent());

        if (opponentCandidates.isEmpty()) {
            return 0;
        }

        return candidateRecord.score() - opponentCandidates.get(0).score();
    }
}