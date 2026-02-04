package go.database;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
@Entity
@Table(name = "game_results")
// Klasa reprezentujaca wynik gry w bazie danych
public class GameResult {
    // Identyfikator wyniku gry, generowany automatycznie przez bazę danych
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    // kolumny tabeli w bazie danych
    private Long id;
    private LocalDateTime playedAt;
    private String winner;
    private int BlackScore;
    private int WhiteScore;
    private String gameType;
    @Lob
    private String movesHistory;
    public GameResult() {
    }
    // Konstruktor pozwalajacy na stworzenie obiektu GameResult z podanymi parametrami
    public GameResult(String winner, int BlackScore, int WhiteScore, String gameType, String movesHistory) {
        this.playedAt = LocalDateTime.now();
        this.winner = winner;
        this.BlackScore = BlackScore;
        this.WhiteScore = WhiteScore;
        this.gameType = gameType;
        this.movesHistory = movesHistory;
    }
    // Gettery pozwalaja na dostep do pol klasy
    public Long getId() {return id;}
    public LocalDateTime getPlayedAt() {return playedAt;}
    public String getWinner() {return winner;}
    public int getBlackScore() {return BlackScore;}
    public int getWhiteScore() {return WhiteScore;}
    public String getGameType() {return gameType;}
    public String getMovesHistory() {return movesHistory;}
}
