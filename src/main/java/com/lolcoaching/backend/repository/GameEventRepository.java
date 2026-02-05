package com.lolcoaching.backend.repository;
import com.lolcoaching.backend.domain.GameEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface GameEventRepository extends JpaRepository<GameEvent, Long> {
    List<GameEvent> findByGameMatchId(Long matchId);
}