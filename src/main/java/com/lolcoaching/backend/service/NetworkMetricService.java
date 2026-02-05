package com.lolcoaching.backend.service;

import com.lolcoaching.backend.domain.NetworkMetric;
import com.lolcoaching.backend.domain.VoiceLog;
import com.lolcoaching.backend.repository.NetworkMetricRepository;
import com.lolcoaching.backend.repository.VoiceLogRepository;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NetworkMetricService {

    private final VoiceLogRepository voiceLogRepository;
    private final NetworkMetricRepository networkMetricRepository;

    // 1. PATTERNS 리스트에 전체(-1, -1) 추가
    private static final List<int[]> PATTERNS = Arrays.asList(
            // new int[]{-1, -1},  <-- 이거 삭제! 절대 DB에 넣지 않음.
            new int[]{1, 0},
            new int[]{2, 3},
            new int[]{0, 0},
            new int[]{0, 1},
            new int[]{0, 2},
            new int[]{3, 0}
    );


    public List<NetworkMetric> getCalculatedAllMetrics(Long matchId) {
        // 1. 해당 매치의 '모든' 로그를 가져옴
        List<VoiceLog> allLogs = voiceLogRepository.findByGameMatchIdOrderByStartTimeAsc(matchId);

        if (allLogs.isEmpty()) return Collections.emptyList();

        // 2. 시간대별 그룹핑
        Map<Integer, List<VoiceLog>> logsByTimeWindow = groupLogsByTimeWindow(allLogs);
        int lastIndex = logsByTimeWindow.keySet().stream().max(Integer::compareTo).orElse(0);

        List<NetworkMetric> result = new ArrayList<>();

        // 3. 계산
        for (int i = 0; i <= lastIndex; i++) {
            List<VoiceLog> windowLogs = logsByTimeWindow.getOrDefault(i, Collections.emptyList());

            // [중요] Density 계산을 위해 Edge 추출은 그대로 유지 (절대 지우면 안 됨)
            List<Edge> edges = extractAllEdges(windowLogs);

            // ★★★ [수정] 여기가 핵심입니다 ★★★
            // 기존 코드: int count = edges.size();  (연결된 것만 카운트)
            // 변경 코드: int count = windowLogs.size(); (말한 건 다 카운트)
            int count = windowLogs.size();

            // 지표 계산 (Density는 여전히 edges 기준)
            double density = computeDensity(edges);
            double[] centralizations = computeCentralization(edges);

            Map<String, Integer> outMap = initRoleMap();
            Map<String, Integer> inMap = initRoleMap();

            // 레이더 차트용 데이터 (여기도 Edge 기준 유지)
            for (Edge edge : edges) {
                outMap.put(edge.from, outMap.getOrDefault(edge.from, 0) + 1);
                inMap.put(edge.to, inMap.getOrDefault(edge.to, 0) + 1);
            }

            NetworkMetric metric = NetworkMetric.builder()
                    .matchId(matchId)
                    .timeIndex(i)
                    .sourceDa(-1)
                    .targetDa(-1)
                    .count(count) // 여기에 '3' (총 발화량)이 들어갑니다.
                    .density(density)
                    .cod(centralizations[0])
                    .cid(centralizations[1])
                    .positionDaCounts(mapToString(outMap))
                    .positionReceiveCounts(mapToString(inMap))
                    .build();

            result.add(metric);
        }
        return result;
    }


    @Transactional
    public void analyzeAndSaveMetrics(Long matchId) {
        // 1. 기존 데이터 삭제 및 로그 로딩
        networkMetricRepository.deleteByMatchId(matchId);
        List<VoiceLog> logs = voiceLogRepository.findByGameMatchIdOrderByStartTimeAsc(matchId);

        if (logs.isEmpty()) return;

        Map<Integer, List<VoiceLog>> logsByTimeWindow = groupLogsByTimeWindow(logs);
        int lastIndex = logsByTimeWindow.keySet().stream().max(Integer::compareTo).orElse(0);

        List<NetworkMetric> metricsToSave = new ArrayList<>();

        for (int i = 0; i <= lastIndex; i++) {
            List<VoiceLog> windowLogs = logsByTimeWindow.getOrDefault(i, Collections.emptyList());

            for (int[] pattern : PATTERNS) {

                // ★★★ [이 2줄이 빠지면 에러 납니다!] ★★★
                int sourceDa = pattern[0]; // 패턴의 앞부분 (예: Q)
                int targetDa = pattern[1]; // 패턴의 뒷부분 (예: I)
                // ------------------------------------------

                // 엣지 추출
                List<Edge> edges = extractEdges(windowLogs, sourceDa, targetDa);

                // ★ [수정] 전체(-1)일 경우 조건 없이 모든 연결 추출
                if (sourceDa == -1 && targetDa == -1) {
                    edges = extractAllEdges(windowLogs); // 아래에 새로 만들 메서드 호출
                } else {
                    edges = extractEdges(windowLogs, sourceDa, targetDa);
                }

                // 1. 기본 지표 계산
                int count = edges.size();
                double density = computeDensity(edges);
                double[] centralizations = computeCentralization(edges);

                // 2. 포지션별 카운트 집계 (JSON용)
                Map<String, Integer> outMap = initRoleMap();
                Map<String, Integer> inMap = initRoleMap();

                for (Edge edge : edges) {
                    outMap.put(edge.from, outMap.getOrDefault(edge.from, 0) + 1);
                    inMap.put(edge.to, inMap.getOrDefault(edge.to, 0) + 1);
                }

                String outDetail = mapToString(outMap);
                String inDetail = mapToString(inMap);

                // 3. 엔티티 생성
                NetworkMetric metric = NetworkMetric.builder()
                        .matchId(matchId)
                        .timeIndex(i)
                        .sourceDa(sourceDa) // 여기서 변수 사용
                        .targetDa(targetDa) // 여기서 변수 사용
                        .count(count)
                        .density(density)
                        .cod(centralizations[0])
                        .cid(centralizations[1])
                        .positionDaCounts(outDetail)
                        .positionReceiveCounts(inDetail)
                        .build();

                metricsToSave.add(metric);
            }
        }
        networkMetricRepository.saveAll(metricsToSave);


    }

    // ★ [추가] 모든 연결을 다 뽑는 메서드 (extractEdges 복사해서 조건만 뺌)
    private List<Edge> extractAllEdges(List<VoiceLog> logs) {
        List<Edge> edges = new ArrayList<>();
        if (logs.size() < 2) return edges;

        for (int i = 0; i < logs.size() - 1; i++) {
            VoiceLog current = logs.get(i);
            VoiceLog next = logs.get(i + 1);

            // DA 코드 확인(if문)을 없애고 무조건 연결로 간주
            if (isValidInteraction(current, next)) {
                edges.add(new Edge(
                        current.getPlayer().getPosition(),
                        next.getPlayer().getPosition()
                ));
            }
        }
        return edges;
    }

    // =================================================================
    // 🧮 내부 계산 로직
    // =================================================================

    private Map<Integer, List<VoiceLog>> groupLogsByTimeWindow(List<VoiceLog> logs) {
        return logs.stream()
                .collect(Collectors.groupingBy(log -> {
                    // StartTime(ms)을 10초(10000ms) 단위 인덱스로 변환
                    long timeMs = log.getStartTime().longValue();
                    return (int) (timeMs / 10000);
                }));
    }

    private List<Edge> extractEdges(List<VoiceLog> logs, int sourceDa, int targetDa) {
        List<Edge> edges = new ArrayList<>();
        if (logs.size() < 2) return edges;

        // 연속된 로그(Chain)를 확인하여 패턴 매칭
        for (int i = 0; i < logs.size() - 1; i++) {
            VoiceLog current = logs.get(i);
            VoiceLog next = logs.get(i + 1);

            if (current.getActCode() == sourceDa && next.getActCode() == targetDa) {
                // 자기 자신과의 대화 제외 (null 체크 포함)
                if (isValidInteraction(current, next)) {
                    // ★ 여기서 중복 체크 없이 무조건 추가함 (Count 계산용)
                    edges.add(new Edge(
                            current.getPlayer().getPosition(),
                            next.getPlayer().getPosition()
                    ));
                }
            }
        }
        return edges;
    }

    private boolean isValidInteraction(VoiceLog current, VoiceLog next) {
        return current.getPlayer() != null && next.getPlayer() != null &&
                !current.getPlayer().getId().equals(next.getPlayer().getId());
    }

    private double computeDensity(List<Edge> edges) {
        if (edges.isEmpty()) return 0.0;

        // ★ Density 계산 시에만 중복 제거 (distinct)
        long uniqueEdges = edges.stream().distinct().count();

        // 5명 기준 최대 연결 수 = 10 (Directed라면 20일 수도 있으나, 현재 로직은 10으로 가정)
        return (double) uniqueEdges / 10.0;
    }

    private double[] computeCentralization(List<Edge> edges) {
        if (edges.isEmpty()) return new double[]{0.0, 0.0};

        Map<String, Integer> outDegree = new HashMap<>();
        Map<String, Integer> inDegree = new HashMap<>();

        // 여기서 totalWeight는 count와 같습니다.
        int totalWeight = edges.size();

        for (Edge edge : edges) {
            outDegree.put(edge.from, outDegree.getOrDefault(edge.from, 0) + 1);
            inDegree.put(edge.to, inDegree.getOrDefault(edge.to, 0) + 1);
        }

        int maxOut = outDegree.values().stream().mapToInt(Integer::intValue).max().orElse(0);
        int maxIn = inDegree.values().stream().mapToInt(Integer::intValue).max().orElse(0);

        // 실제 DB에 저장된 포지션 문자열과 정확히 일치해야 합니다. (오타 주의)
        List<String> allNodes = Arrays.asList("TOP", "JUG", "MID", "ADC", "SUP");

        double sumDiffOut = 0;
        double sumDiffIn = 0;

        for (String node : allNodes) {
            sumDiffOut += (maxOut - outDegree.getOrDefault(node, 0));
            sumDiffIn += (maxIn - inDegree.getOrDefault(node, 0));
        }

        // Freeman Centralization 변형 공식 사용 중인 것으로 추정
        double denominator = 4.0 * totalWeight;

        if (denominator == 0) return new double[]{0.0, 0.0};

        return new double[]{ sumDiffOut / denominator, sumDiffIn / denominator };
    }

    // 엣지 클래스: Density의 distinct()가 제대로 동작하려면 equals/hashCode 필수
    @AllArgsConstructor
    @EqualsAndHashCode
    static class Edge {
        String from;
        String to;
    }

    // 1. 맵 초기화 (모든 포지션 0으로 세팅)
    private Map<String, Integer> initRoleMap() {
        Map<String, Integer> map = new LinkedHashMap<>(); // 순서 보장 (TOP -> JUG -> ...)
        map.put("TOP", 0);
        map.put("JUG", 0);
        map.put("MID", 0);
        map.put("ADC", 0);
        map.put("SUP", 0);
        return map;
    }

    // 2. 맵을 문자열로 변환 (DB 저장용: "TOP:1,JUG:0...")
    private String mapToString(Map<String, Integer> map) {
        return map.entrySet().stream()
                .map(e -> e.getKey() + ":" + e.getValue())
                .collect(Collectors.joining(","));
    }

    // ★ [신규 추가] 시간 범위 내 로그를 다시 조회해서 "누적 밀도" 계산
    public double calculateRangeDensity(Long matchId, int startSec, int endSec, int sourceDa, int targetDa) {

        // 1. 해당 매치의 모든 로그 가져오기 (DB 최적화를 위해선 시간 조건도 쿼리에 넣는 게 좋지만, 일단은 필터링으로 구현)
        List<VoiceLog> allLogs = voiceLogRepository.findByGameMatchIdOrderByStartTimeAsc(matchId);

        // 2. 시간 범위 & DA 패턴 필터링
        List<VoiceLog> filteredLogs = allLogs.stream()
                .filter(log -> {
                    long sec = (long) (log.getStartTime() / 1000); // ms -> sec 변환
                    boolean timeCondition = (sec >= startSec && sec <= endSec);

                    // 전체(-1)이면 시간만 보고, 아니면 DA 코드도 확인
                    boolean daCondition = (sourceDa == -1 && targetDa == -1)
                            ? true
                            : (log.getActCode() == sourceDa || log.getActCode() == targetDa); // 주의: 이 부분은 단순 필터링이고, 실제 연결 확인은 아래에서 함

                    return timeCondition;
                })
                .collect(Collectors.toList());

        // 3. 엣지 추출 (패턴에 맞는 연결만 뽑아내기)
        List<Edge> edges;
        if (sourceDa == -1 && targetDa == -1) {
            edges = extractAllEdges(filteredLogs); // 아까 만든 전체 추출 메서드 재활용
        } else {
            edges = extractEdges(filteredLogs, sourceDa, targetDa); // 특정 패턴 추출 메서드 재활용
        }

        // 4. ★★★ [핵심] 중복 제거 (Distinct) ★★★
        // 10분 동안 TOP-JUG가 100번 말했어도, Unique Edge는 1개로 침!
        long uniqueEdges = edges.stream().distinct().count();

        // 5. 밀도 계산 (최대 연결 가능 수 10개 기준)
        // 소수점 첫째 자리까지만 깔끔하게 나오도록 처리
        double density = (double) uniqueEdges / 10.0;

        return Math.min(density, 1.0); // 1.0을 넘을 순 없으므로 안전장치
    }
}