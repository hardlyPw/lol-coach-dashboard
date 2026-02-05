package com.lolcoaching.backend.service;

import com.lolcoaching.backend.Dto.GameEventResponseDto;
import com.lolcoaching.backend.Dto.MatchResponseDto;
import com.lolcoaching.backend.Dto.PlayerResponseDto;
import com.lolcoaching.backend.Dto.VoiceLogResponseDto;
import com.lolcoaching.backend.domain.*;
import com.lolcoaching.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class MatchImportService {

    private final NetworkMetricService networkMetricService;
    private final GameMatchRepository matchRepository;
    private final VoiceLogRepository voiceLogRepository;
    private final GameEventRepository gameEventRepository;
    private final PlayerRepository playerRepository;


    @Transactional(readOnly = true)
    public MatchResponseDto getMatchDetail(Long matchId) {
        GameMatch match = matchRepository.findById(matchId)
                .orElseThrow(() -> new IllegalArgumentException("매치 없음"));

        // 1. 보이스 로그 변환
        List<VoiceLogResponseDto> voiceLogDtos = voiceLogRepository.findAll().stream()
                .filter(v -> v.getGameMatch().getId().equals(matchId))
                .map(VoiceLogResponseDto::new)
                .toList();

        // 2. 플레이어 정보 변환
        List<PlayerResponseDto> playerDtos = playerRepository.findAll().stream()
                .filter(p -> p.getGameMatch().getId().equals(matchId))
                .map(PlayerResponseDto::new)
                .toList();

        // 3. 게임 이벤트 변환
        List<GameEventResponseDto> eventDtos = gameEventRepository.findAll().stream()
                .filter(e -> e.getGameMatch().getId().equals(matchId))
                .map(GameEventResponseDto::new)
                .toList();

        return MatchResponseDto.builder()
                .matchId(match.getId())
                .matchCode(match.getMatchCode())
                .duration(match.getDuration()) // ★ 타임라인 0:00 해결의 핵심!
                .voiceLogs(voiceLogDtos)
                .players(playerDtos)
                .gameEvents(eventDtos)
                .build();
    }

    @Transactional
    // ★ String matchCode를 콤마(,) 뒤에 추가해주세요!
    public Long importMatch(MultipartFile zipFile, String matchCode) throws Exception {
        GameMatch tempMatch = new GameMatch();
        List<String[]> infoRows = new ArrayList<>();
        List<String[]> daRows = new ArrayList<>();
        List<String[]> korRows = new ArrayList<>();
        List<String[]> eventRows = new ArrayList<>();
        List<String[]> timeRows = new ArrayList<>(); // ★ game_time.csv 추가

        boolean hasMeta = false;

        // 1. ZIP 파일 읽기 및 바구니에 담기
        try (ZipInputStream zis = new ZipInputStream(zipFile.getInputStream())) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.isDirectory()) continue;
                String fileName = entry.getName().toLowerCase();

                if (fileName.endsWith(".txt") && fileName.contains("match_")) {
                    parseMetaFileContent(zis, tempMatch);
                    hasMeta = true;
                }
                else if (fileName.contains("info.csv")) infoRows.addAll(readCsvLines(zis));
                else if (fileName.contains("da_result.csv")) daRows.addAll(readCsvLines(zis));
                else if (fileName.contains("asr_result_kor.csv")) korRows.addAll(readCsvLines(zis));
                else if (fileName.contains("event.csv")) eventRows.addAll(readCsvLines(zis));
                else if (fileName.contains("game_time.csv")) timeRows.addAll(readCsvLines(zis)); // ★ 추가
                zis.closeEntry();
            }
        }

        if (!hasMeta) throw new RuntimeException("ZIP 파일 메타데이터 누락");

        // 2. 게임 시간(Duration) 처리
        if (!timeRows.isEmpty()) {
            try {
                // [5]번 인덱스가 duration (ms 단위)
                tempMatch.setDuration(Long.parseLong(timeRows.get(0)[5].trim()));
            } catch (Exception e) { System.err.println("Duration 파싱 실패"); }
        }

        // 3. 데이터베이스 저장 (순서 중요! Match -> Player -> Logs/Events)

        // [Step 1] 매치 저장
        GameMatch savedMatch = matchRepository.save(tempMatch);


        // ★ [수정] 사용자가 입력한 이름을 저장 (없으면 파일명 등 기본값)
        if (matchCode != null && !matchCode.isEmpty()) {
            savedMatch.setMatchCode(matchCode);
        } else {
            savedMatch.setMatchCode("Unknown Match");
        }


        // [Step 2] 플레이어 저장 및 매핑 준비 (ID 1~10 -> Player 객체)
        // info.csv: [0]Team, [1]PlayerID, [2]SummonerName, [3]Position
// [Step 2] 플레이어 저장 및 매핑 준비
        // 3. 플레이어 저장 (포지션 표준화 적용)
        Map<Integer, Player> idToPlayerMap = new HashMap<>();
        for (String[] row : infoRows) {
            try {
                Player player = Player.builder()
                        .gameMatch(savedMatch)
                        .inGameId(Integer.parseInt(row[1]))
                        .team(row[0])
                        .summonerName(row[2])
                        .position(standardizePosition(row[3])) // ★ 표준화 함수 적용
                        .build();
                playerRepository.save(player);
                idToPlayerMap.put(player.getInGameId(), player);
            } catch (Exception e) { /* 에러 처리 */ }
        }
        // [Step 3] 음성 로그 저장 (VoiceLog)
        // da_result.csv: [0]speaker("3-go_ni"), [1]text, [2]start, [3]end, ... [5]act
        // [Step 3] 음성 로그 저장 (VoiceLog) - 숫자와 라벨 함께 저장
        List<VoiceLog> voiceLogs = new ArrayList<>();
        String[] labels = {"I", "Q", "D", "C"}; // 0:I, 1:Q, 2:D, 3:C 매핑

        int minSize = Math.min(daRows.size(), korRows.size());

        for (int i = 0; i < minSize; i++) {
            try {
                String[] daRow = daRows.get(i);
                String[] korRow = korRows.get(i);

                int playerId = extractIdFromSpeaker(daRow[0]);
                Player player = idToPlayerMap.get(playerId);

                // actCode 파싱 및 대응하는 라벨 결정
                int actCode = Integer.parseInt(daRow[5]);
                String actLabel = (actCode >= 0 && actCode < labels.length) ? labels[actCode] : "UNK";

                VoiceLog log = VoiceLog.builder()
                        .gameMatch(savedMatch)
                        .player(player)
                        .textKor(korRow[1].replace("\"", ""))
                        .startTime(Double.parseDouble(daRow[2]))
                        .endTime(Double.parseDouble(daRow[3]))
                        .actCode(actCode)    // 숫자 저장 (0, 1, 2, 3)
                        .actLabel(actLabel)  // 문자열 라벨 저장 ("I", "Q", "D", "C")
                        .build();

                voiceLogs.add(log);
            } catch (Exception e) {
                System.err.println(i + "번째 행 파싱 실패: " + e.getMessage());
            }
        }

        // MatchImportService.java

// ... (CSV 파싱 부분은 그대로 유지) ...

// MatchImportService.java 내부

// ... (CSV 파싱 부분 생략) ...

// ==========================================================
// ★★★ [수정] 이미 밀리초이므로 * 1000 제거! ★★★
// ==========================================================

// 1. 가장 빠른 시간(minTime) 찾기
        double minTime = voiceLogs.stream()
                .mapToDouble(VoiceLog::getStartTime)
                .filter(t -> t > 0.1)
                .min()
                .orElse(0.0);

        double maxTime = voiceLogs.stream()
                .mapToDouble(VoiceLog::getStartTime)
                .max()
                .orElse(0.0);

        if (minTime == 0.0 && !voiceLogs.isEmpty()) {
            minTime = voiceLogs.get(0).getStartTime();
        }

// [수정 1] Duration 계산: * 1000 제거
        long durationMs = (long) (maxTime - minTime);

        System.out.println(">> [TimeCheck] Fixed Duration(ms): " + durationMs);
// 이제 940,000 (약 15분) 정도로 정상적으로 나올 겁니다.

        savedMatch.setDuration(durationMs);
        matchRepository.save(savedMatch);

// [수정 2] 음성 로그 시간 정규화: * 1000 제거
        for (VoiceLog log : voiceLogs) {
            // 그냥 뺍니다 (이미 밀리초 단위임)
            double startMs = Math.max(0, log.getStartTime() - minTime);
            double endMs = Math.max(0, log.getEndTime() - minTime);

            log.setStartTime(startMs);
            log.setEndTime(endMs);
        }

        voiceLogRepository.saveAllAndFlush(voiceLogs);


// [Step 4] 게임 이벤트 저장 (GameEvent)
        List<GameEvent> gameEvents = new ArrayList<>();
        Pattern idPattern = Pattern.compile("^(\\d+)");

        for (String[] row : eventRows) {
            try {
                if (row.length < 2) continue;

                GameEvent event = new GameEvent();
                event.setGameMatch(savedMatch);
                event.setEventName(row[0]); // 예: ChampionKill

                // 1. 시간 파싱 및 정규화
                if (row[1] != null && !row[1].isEmpty()) {
                    try {
                        double rawTime = Double.parseDouble(row[1]);
                        // (이미 밀리초라면 *1000 제거, 초 단위라면 *1000 유지 - 현재 코드 기준 *1 유지)
                        double normalizedTimeMs = Math.max(0, rawTime - minTime);
                        event.setEventTime((long) normalizedTimeMs);
                    } catch (NumberFormatException e) {
                        event.setEventTime(0L);
                    }
                }

                // ★★★ [추가된 부분] Killer ID & Victim ID 파싱 ★★★
                // event.csv 구조 가정: [0]Event, [1]Time, [2]Killer, [3]Victim, ...

                // 2. Killer ID 추출 (3번째 컬럼)
                if (row.length > 2 && row[2] != null && !row[2].isEmpty()) {
                    Matcher kMatcher = idPattern.matcher(row[2].trim());
                    if (kMatcher.find()) {
                        try {
                            event.setKillerId(Long.parseLong(kMatcher.group(1)));
                        } catch (NumberFormatException e) { /* 무시 */ }
                    }
                }

                // 3. Victim ID 추출 (4번째 컬럼)
                if (row.length > 3 && row[3] != null && !row[3].isEmpty()) {
                    Matcher vMatcher = idPattern.matcher(row[3].trim());
                    if (vMatcher.find()) {
                        try {
                            event.setVictimId(Long.parseLong(vMatcher.group(1)));
                        } catch (NumberFormatException e) { /* 무시 */ }
                    }
                }

                gameEvents.add(event);

            } catch (Exception e) {
                System.err.println("이벤트 파싱 중 오류 발생: " + e.getMessage());
            }
        }

        gameEventRepository.saveAllAndFlush(gameEvents);

// ... (이후 분석 서비스 호출) ...

// [Step 5] 분석 서비스 호출
        System.out.println(">> [MatchImportService] 네트워크 지표 분석 시작...");
        networkMetricService.analyzeAndSaveMetrics(savedMatch.getId());
        System.out.println(">> [MatchImportService] 네트워크 지표 분석 완료!");

        return savedMatch.getId();


    }

    // --- Helper Methods ---

    // CSV 파일 읽어서 List<String[]>으로 반환 (공통 함수)
    private List<String[]> readCsvLines(ZipInputStream zis) throws Exception {
        List<String[]> list = new ArrayList<>();
        // ZIP 스트림을 닫지 않도록 감싸기만 함
        BufferedReader br = new BufferedReader(new InputStreamReader(zis, StandardCharsets.UTF_8));
        String line;
        boolean isHeader = true;
        while ((line = br.readLine()) != null) {
            if (isHeader) { isHeader = false; continue; }
            // 정규식으로 쉼표 분리 (따옴표 안의 쉼표 무시)
            list.add(line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1));
        }
        return list;
    }

    // 메타데이터 파싱
    private void parseMetaFileContent(ZipInputStream zis, GameMatch match) throws Exception {
        BufferedReader br = new BufferedReader(new InputStreamReader(zis, StandardCharsets.UTF_8));
        String line = br.readLine();
        if (line != null) match.setMatchCode(line.trim());
    }

    // "3-go_ni" -> 3 추출
    private int extractIdFromSpeaker(String speaker) {
        try {
            if (speaker.contains("-")) {
                return Integer.parseInt(speaker.split("-")[0]);
            }
            return Integer.parseInt(speaker.replaceAll("[^0-9]", "")); // 숫자만 추출
        } catch (NumberFormatException e) {
            return -1; // 찾기 실패
        }
    }
    // CsvDataLoader에서 가져온 표준화 함수
    private String standardizePosition(String rawPosition) {
        if (rawPosition == null) return "UNK";
        String upper = rawPosition.toUpperCase().trim();
        switch (upper) {
            case "JUNGLE":  return "JUG";
            case "MIDDLE":  return "MID";
            case "BOTTOM":  return "ADC";
            case "UTILITY": return "SUP";
            default:        return upper;
        }
    }
}