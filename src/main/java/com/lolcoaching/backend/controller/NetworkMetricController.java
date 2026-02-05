package com.lolcoaching.backend.controller;

import com.lolcoaching.backend.domain.NetworkMetric;
import com.lolcoaching.backend.repository.NetworkMetricRepository;
import com.lolcoaching.backend.repository.VoiceLogRepository;
import com.lolcoaching.backend.service.NetworkMetricService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/matches") // 공통 주소
@RequiredArgsConstructor
public class NetworkMetricController {

    private final NetworkMetricRepository networkMetricRepository;

    private final NetworkMetricService networkMetricService;
    // ★ 이 메서드가 없어서 프론트엔드가 데이터를 못 받고 있는 겁니다.
    @GetMapping("/{matchId}/metrics")
    public List<NetworkMetric> getMetrics(
            @PathVariable Long matchId,
            @RequestParam(defaultValue = "1") int sourceDa,
            @RequestParam(defaultValue = "0") int targetDa
    ) {
        // ★ 요청이 -1(전체)이면 DB를 거치지 않고 실시간 계산 결과를 줍니다.
        if (sourceDa == -1 && targetDa == -1) {
            // Service에 새로 만든 메서드 호출
            return networkMetricService.getCalculatedAllMetrics(matchId);
        }

        // 그 외(일반 패턴)는 DB에서 가져옵니다.
        return networkMetricRepository.findByMatchIdAndSourceDaAndTargetDaOrderByTimeIndexAsc(matchId, sourceDa, targetDa);
    }



    // ★ [신규 추가] 특정 시간 범위의 정확한 누적 밀도 계산 API
    @GetMapping("/{matchId}/analysis")
    public Map<String, Object> getRangeAnalysis(
            @PathVariable Long matchId,
            @RequestParam(defaultValue = "0") Integer start, // int -> Integer 변경
            @RequestParam(defaultValue = "0") Integer end,   // int -> Integer 변경
            @RequestParam(defaultValue = "-1") Integer sourceDa,
            @RequestParam(defaultValue = "-1") Integer targetDa
    ) {
        // 1. 방어 로직: 이상한 값이 오면 0.0 리턴
        if (start == null || end == null || start >= end) {
            System.out.println("유효하지 않은 시간 범위 요청: " + start + " ~ " + end);
            return Map.of("density", 0.0);
        }

        // 2. 서비스 호출
        double density = networkMetricService.calculateRangeDensity(matchId, start, end, sourceDa, targetDa);

        return Map.of("density", density);
    }
}