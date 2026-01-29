package go.database;

import java.time.LocalDate;

import jakarta.persistence.*;

import java.time.LocalDateTime;
@Entity
@Table(name = "game_results")
public class GameResult {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
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
    public GameResult(String winner, int BlackScore, int WhiteScore, String gameType, String movesHistory) {
        this.playedAt = LocalDateTime.now();
        this.winner = winner;
        this.BlackScore = BlackScore;
        this.WhiteScore = WhiteScore;
        this.gameType = gameType;
        this.movesHistory = movesHistory;
    }
    public Long getId() {return id;}
    public LocalDateTime getPlayedAt() {return playedAt;}
    public String getWinner() {return winner;}
    public int getBlackScore() {return BlackScore;}
    public int getWhiteScore() {return WhiteScore;}
    public String getGameType() {return gameType;}
    public String getMovesHistory() {return movesHistory;}
}
