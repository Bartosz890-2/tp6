package go.si;

import java.awt.*;

/**
 * Rekord (DTO) reprezentujący kandydata na ruch w algorytmie bota.
 * <p>
 * Przechowuje parę: współrzędne na planszy oraz przypisaną im wartość punktową (ocenę heurystyczną).
 * Obiekty tego typu są tworzone przez {@link SmartBotHeuristics}, a następnie sortowane,
 * aby wyłonić najlepszych kandydatów, którzy zostaną poddani głębszej symulacji (Monte Carlo/Minimax).
 *
 * @param point współrzędne rozważanego ruchu na planszy.
 * @param score wstępna ocena punktowa ruchu (wynik heurystyki). Im wyższa wartość, tym bardziej obiecujący ruch.
 */
public record CandidateRecord(
        Point point,
        double score
) {
}