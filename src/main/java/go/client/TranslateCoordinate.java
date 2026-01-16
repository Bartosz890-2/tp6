package go.client;

/**
 * Klasa narzędziowa realizująca wzorzec Adapter.
 * Tłumaczy współrzędne w formacie tekstowym (np. "A10") na format tablicowy [x, y]
 * używany w logice gry, oraz odwrotnie.
 */
public class TranslateCoordinate {

    /**
     * Konwertuje ciąg znaków reprezentujący ruch (np. "C15") na współrzędne tablicowe.
     *
     * @param input ciąg znaków (litera kolumny + liczba wiersza), np. "A1", "T19".
     * @return tablica int[] {x, y} lub null, jeśli format jest niepoprawny lub poza zakresem.
     */
    public static int[] translate(String input) {
        if (input.length() < 2 || input.length() > 3) return null;

        input = input.toUpperCase();
        char letter = input.charAt(0);

        int x = letter - 'A';
        if (x < 0 || x >= 19) return null;

        try {
            int y = Integer.parseInt(input.substring(1)) - 1;

            if (y >= 0 && y < 19) return new int[]{x, y};
        } catch (NumberFormatException e) {
            return null;
        }

        return null;
    }

    /**
     * Konwertuje indeks kolumny na odpowiadającą mu literę.
     *
     * @param number indeks kolumny (0-18).
     * @return znak reprezentujący kolumnę (np. 0 -> 'A').
     */
    public static char invertTranslate(int number) {
        return (char) (number + 'A');
    }
}