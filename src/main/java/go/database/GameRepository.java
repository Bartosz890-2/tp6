package go.database;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
@Repository
public interface GameRepository extends JpaRepository<GameResult, Long> {
    
}
