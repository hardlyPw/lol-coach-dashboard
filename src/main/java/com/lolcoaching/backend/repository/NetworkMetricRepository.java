package com.lolcoaching.backend.repository;

import com.lolcoaching.backend.domain.NetworkMetric;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NetworkMetricRepository extends JpaRepository<NetworkMetric, Long> {

    /**
     * 특정 게임의 '특정 대화 패턴'에 대한 지표만 시간순으로 가져옵니다.
     * 용도: 프론트엔드 그래프 그리기
     * 예: findByMatchIdAndSourceDaAndTargetDaOrderByTimeIndexAsc(1L, 1, 0); -> "질문->답변" 추이 조회
     */
    List<NetworkMetric> findByMatchIdAndSourceDaAndTargetDaOrderByTimeIndexAsc(
            Long matchId, int sourceDa, int targetDa
    );
    void deleteByMatchId(Long matchId);
    /**
     * 특정 게임의 모든 지표 데이터를 가져옵니다.
     * 용도: 한 번에 데이터를 다 로딩해놓고 필터링할 때 사용
     */
    List<NetworkMetric> findByMatchIdOrderByTimeIndexAsc(Long matchId);
    List<NetworkMetric> findByMatchIdAndSourceDaAndTargetDa(Long matchId, int sourceDa, int targetDa);
}