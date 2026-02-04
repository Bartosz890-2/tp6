package go.client;
// Przekazuje informacje o zapisanej grze
public class GameRecordDTO {
    private final Long id;
    private final String date;
    private final String winner;
    private final String score;
    private final String type;
    private final String movesHistory;
    // Konstruktor oraz getter, ktore pozwalaja na dostep do pol
    public GameRecordDTO(Long id, String date, String winner, String score, String type, String movesHistory) {
        this.id = id;
        this.date = date;
        this.winner = winner;
        this.score = score;
        this.type = type;
        this.movesHistory = movesHistory;
    }
    public Long getId() { return id;}
    public String getDate() { return date;}
    public String getWinner() { return winner;}
    public String getScore() { return score;}
    public String getType() { return type;}
    public String getMovesHistory() { return movesHistory;}
    // Metoda toString zwraca tekstowa reprezentacje gry, ktora zawiera id, date i zwyciezce
    @Override
    public String toString() {
        return "Gra #" + id + " (" + date + ") - Wygrał: " + winner;
    }
    
}
