package go.ui.fx;

import java.awt.Point;
import java.util.HashSet;
import java.util.Set;

import go.logic.Board;
import go.logic.Stone;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * Komponent graficzny (rozszerzający JavaFX Canvas) odpowiedzialny za wizualizację planszy do gry Go.
 * Rysuje tło, siatkę, kamienie z cieniami oraz specjalne oznaczenia (np. krzyżyki na martwych grupach).
 * Odpowiada również za przeliczanie współrzędnych ekranowych (kliknięć) na współrzędne logiczne planszy.
 */
public class BoardCanvas extends Canvas {

    /** Odległość w pikselach między liniami siatki. */
    private final double cellSize = 35.0;

    /** Margines (przesunięcie) od krawędzi płótna do pierwszej linii siatki. */
    private final double offset = 40.0;

    /** Zbiór punktów, które mają zostać wyróżnione na planszy (np. jako martwe grupy). */
    private Set<Point> highlightedPoints = new HashSet<>();

    /**
     * Aktualizuje zbiór punktów, które mają być wyróżnione na planszy (np. czerwonym krzyżykiem).
     * Uwaga: Ta metoda nie odświeża widoku automatycznie, należy wywołać {@link #draw(Board)}.
     *
     * @param points zbiór punktów do zaznaczenia.
     */
    public void setHighlightedPoints(Set<Point> points) {
        this.highlightedPoints = new HashSet<>(points);
    }

    /**
     * Główna metoda rysująca. Czyści płótno i rysuje kolejno:
     * 1. Tło planszy (kolor drewna).
     * 2. Siatkę linii (19x19).
     * 3. Kamienie (czarne i białe) na podstawie stanu obiektu Board.
     * 4. Oznaczenia (krzyżyki) dla punktów w {@code highlightedPoints}.
     *
     * @param board obiekt modelu planszy zawierający aktualny układ kamieni.
     */
    public void draw(Board board) {
        GraphicsContext gc = getGraphicsContext2D();
        int size = board.getSize();

        // 1. TŁO
        gc.setFill(Color.web("#DEB887")); // Kolor przypominający drewno (Burlywood)
        gc.fillRect(0, 0, getWidth(), getHeight());

        // 2. SIATKA
        gc.setStroke(Color.BLACK);
        gc.setLineWidth(1.0);
        for (int i = 0; i < size; i++) {
            // linie poziome
            gc.strokeLine(offset, offset + i * cellSize, offset + (size - 1) * cellSize, offset + i * cellSize);
            // linie pionowe
            gc.strokeLine(offset + i * cellSize, offset, offset + i * cellSize, offset + (size - 1) * cellSize);
        }

        // 3. KAMIENIE
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                Stone stone = board.getField(x, y);
                if (stone != Stone.EMPTY) {
                    renderStone(gc, x, y, stone);
                }
            }
        }

        // 4. ZAZNACZENIA (KRZYŻYKI)
        if (!highlightedPoints.isEmpty()) {
            gc.setStroke(Color.RED);
            gc.setLineWidth(3.0);

            for (Point p : highlightedPoints) {
                double centerX = offset + p.x * cellSize;
                double centerY = offset + p.y * cellSize;
                double r = cellSize * 0.2; // Rozmiar ramienia krzyżyka

                // Rysujemy "X"
                gc.strokeLine(centerX - r, centerY - r, centerX + r, centerY + r); // Linia \
                gc.strokeLine(centerX + r, centerY - r, centerX - r, centerY + r); // Linia /
            }
        }
    }

    /**
     * Pomocnicza metoda rysująca pojedynczy kamień.
     * Dodaje cień pod kamieniem oraz delikatną obwódkę dla lepszej widoczności (zwłaszcza białych kamieni).
     *
     * @param gc    kontekst graficzny JavaFX.
     * @param x     współrzędna logiczna X na planszy (indeks kolumny).
     * @param y     współrzędna logiczna Y na planszy (indeks wiersza).
     * @param color kolor kamienia (Stone.BLACK lub Stone.WHITE).
     */
    private void renderStone(GraphicsContext gc, int x, int y, Stone color) {
        double r = cellSize * 0.45; // promień kamienia (trochę mniejszy niż połowa kratki, żeby się nie stykały)
        double centerX = offset + x * cellSize;
        double centerY = offset + y * cellSize;

        // Cień
        gc.setFill(Color.rgb(0, 0, 0, 0.3));
        gc.fillOval(centerX - r + 2, centerY - r + 2, r * 2, r * 2);

        // Kamień właściwy
        gc.setFill(color == Stone.BLACK ? Color.BLACK : Color.WHITE);
        gc.fillOval(centerX - r, centerY - r, r * 2, r * 2);

        // Obwódka
        gc.setStroke(Color.BLACK);
        gc.setLineWidth(0.5);
        gc.strokeOval(centerX - r, centerY - r, r * 2, r * 2);
    }

    /**
     * Konwertuje współrzędną ekranową (piksele, np. z kliknięcia myszką) na indeks pola na planszy.
     * Znajduje najbliższy punkt przecięcia linii siatki.
     *
     * @param pixel wartość współrzędnej w pikselach.
     * @return indeks pola na siatce (0-18) odpowiadający kliknięciu.
     */
    public int toBoardCoord(double pixel) {
        return (int) Math.round((pixel - offset) / cellSize);
    }
}