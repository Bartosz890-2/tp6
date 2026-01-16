package go.logic;

/**
 * Typ wyliczeniowy reprezentujący cztery główne kierunki świata (sąsiedztwo w siatce).
 * Używany do iteracji po sąsiadach danego pola (góra, dół, lewo, prawo)
 * w algorytmach takich jak wykrywanie grup czy liczenie oddechów.
 */
public enum Direction {
    UP(0, -1),
    LEFT(-1, 0),
    RIGHT(1, 0),
    DOWN(0, 1);

    /**
     * Przesunięcie w osi X (poziomo).
     */
    private final int dx;

    /**
     * Przesunięcie w osi Y (pionowo).
     */
    private final int dy;

    /**
     * Konstruktor kierunku.
     *
     * @param dx zmiana współrzędnej X.
     * @param dy zmiana współrzędnej Y.
     */
    Direction(int dx, int dy) {
        this.dx = dx;
        this.dy = dy;
    }

    /**
     * Pobiera wartość przesunięcia w osi X dla danego kierunku.
     *
     * @return -1, 0 lub 1.
     */
    public int getDx() {
        return dx;
    }

    /**
     * Pobiera wartość przesunięcia w osi Y dla danego kierunku.
     *
     * @return -1, 0 lub 1.
     */
    public int getDy() {
        return dy;
    }
}