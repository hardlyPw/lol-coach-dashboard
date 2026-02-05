package com.lolcoaching.backend.repository;

import com.lolcoaching.backend.domain.GameMatch;
import com.lolcoaching.backend.domain.Player;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PlayerRepository extends JpaRepository<Player, Long> {
    // "특정 매치"의 "몇 번 플레이어(inGameId)"인지로 찾기
    Optional<Player> findByGameMatchAndInGameId(GameMatch gameMatch, Integer inGameId);
}