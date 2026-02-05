package com.lolcoaching.backend.repository;
import com.lolcoaching.backend.domain.GameMatch;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GameMatchRepository extends JpaRepository<GameMatch, Long> {
}