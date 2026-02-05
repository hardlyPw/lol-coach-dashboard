package com.lolcoaching.backend.repository;
import com.lolcoaching.backend.domain.VoiceLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface VoiceLogRepository extends JpaRepository<VoiceLog, Long> {
    // 특정 게임의 로그만 가져오는 기능
    List<VoiceLog> findByGameMatchId(Long matchId);
    List<VoiceLog> findByGameMatchIdOrderByStartTimeAsc(Long gameMatchId);
    // ★ [추가] 전체 조회용 메서드
    List<VoiceLog> findAllByGameMatchId(Long matchId);
}