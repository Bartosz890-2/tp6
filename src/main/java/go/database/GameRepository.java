package go.database;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
// Interfejs repozytorium do zarzadzania wynikami gier w bazie danych
@Repository
public interface GameRepository extends JpaRepository<GameResult, Long> {
    
}
