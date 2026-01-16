package go.logic;

/**
 * Typ wyliczeniowy reprezentujący możliwe stany pola na planszy Go.
 * Określa kolor kamienia (Czarny/Biały) lub brak kamienia (Puste).
 */
public enum Stone {
    BLACK,
    WHITE,
    EMPTY;

    /**
     * Zwraca kolor kamienia przeciwnika dla bieżącego koloru.
     *
     * @return WHITE dla BLACK, BLACK dla WHITE, oraz EMPTY dla EMPTY.
     */
    public Stone opponent() {
        if (this == BLACK) return WHITE;
        if (this == WHITE) return BLACK;
        return EMPTY;
    }
}