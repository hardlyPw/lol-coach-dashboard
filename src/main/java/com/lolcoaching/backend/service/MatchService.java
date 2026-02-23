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
public class MatchService {

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
    public Long importMatch(MultipartFile zipFile, String matchCode, String myTeam) throws Exception {
        // [Step 1] 매치 객체 초기화 및 기본 정보 설정
        GameMatch tempMatch = new GameMatch();

        // matchCode가 비어있을 경우를 대비한 기본값 설정
        String finalMatchCode = (matchCode != null && !matchCode.isEmpty()) ? matchCode : "Unknown Match";
        tempMatch.setMatchCode(finalMatchCode);
        tempMatch.setUserTeam(myTeam.toUpperCase()); // "BLUE" 또는 "RED" 저장

        // 연관 데이터 저장을 위해 매치를 먼저 저장하여 ID를 확보합니다.
        GameMatch savedMatch = matchRepository.save(tempMatch);
        savedMatch.setMatchCode(finalMatchCode);

        List<String[]> infoRows = new ArrayList<>();
        List<String[]> daRows = new ArrayList<>();
        //List<String[]> korRows = new ArrayList<>();
        List<String[]> eventRows = new ArrayList<>();
        List<String[]> timeRows = new ArrayList<>();

        boolean hasMeta = false;

        // [Step 2] ZIP 파일 읽기 및 데이터 수집
        try (ZipInputStream zis = new ZipInputStream(zipFile.getInputStream())) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.isDirectory()) continue;
                String fileName = entry.getName().toLowerCase();

                if (fileName.endsWith(".txt") && fileName.contains("match_")) {
                    parseMetaFileContent(zis, savedMatch);
                    hasMeta = true;
                }
                else if (fileName.contains("info.csv")) infoRows.addAll(readCsvLines(zis));
                else if (fileName.contains("da_result.csv")) daRows.addAll(readCsvLines(zis));
                else if (fileName.contains("event.csv")) eventRows.addAll(readCsvLines(zis));
                else if (fileName.contains("game_time.csv")) timeRows.addAll(readCsvLines(zis));
                zis.closeEntry();
            }
        }

        if (!hasMeta) throw new RuntimeException("ZIP 파일 내 메타데이터(.txt)가 누락되었습니다.");

        // [Step 3] 플레이어 저장 및 우리 팀(isUserTeam) 식별
        Map<Integer, Player> idToPlayerMap = new HashMap<>();
        for (String[] row : infoRows) {
            try {
                // row[0]: Team (BLUE/RED), row[1]: PlayerID, row[2]: SummonerName, row[3]: Position
                String teamInCsv = row[0].toUpperCase().trim();
                boolean isMyTeam = teamInCsv.equals(myTeam.toUpperCase());

                Player player = Player.builder()
                        .gameMatch(savedMatch)
                        .inGameId(Integer.parseInt(row[1]))
                        .team(teamInCsv)
                        .summonerName(row[2])
                        .position(standardizePosition(row[3]))
                        .isUserTeam(isMyTeam) // 프론트에서 선택한 팀과 일치하면 true
                        .build();

                playerRepository.save(player);
                idToPlayerMap.put(player.getInGameId(), player);
            } catch (Exception e) {
                System.err.println("플레이어 파싱 실패: " + e.getMessage());
            }
        }

        // [Step 4] 음성 로그(VoiceLog) 처리
        // [Step 4] 음성 로그 저장 (VoiceLog)
        List<VoiceLog> voiceLogs = new ArrayList<>();
        String[] labels = {"I", "Q", "D", "C"};

        // ★ korRows와의 비교 없이 daRows만 반복합니다.
        for (int i = 0; i < daRows.size(); i++) {
            try {
                String[] daRow = daRows.get(i);
                // String[] korRow = korRows.get(i); // ★ 제거

                // 1. 화자 및 플레이어 매핑 (기존 로직 유지)
                int extractedId = extractIdFromSpeaker(daRow[0]);
                int actualPlayerId = ("RED".equalsIgnoreCase(myTeam)) ? extractedId + 5 : extractedId;

                Player player = idToPlayerMap.get(actualPlayerId);
                if (player == null) continue;

                // 2. DA 정보 파싱 (기존 로직 유지)
                int actCode = Integer.parseInt(daRow[5]);
                String actLabel = (actCode >= 0 && actCode < labels.length) ? labels[actCode] : "UNK";

                // 3. VoiceLog 생성
                VoiceLog log = VoiceLog.builder()
                        .gameMatch(savedMatch)
                        .player(player)
                        // ★ 핵심 수정: daRow의 1번 인덱스(text 컬럼)에서 한글 내용을 직접 가져옵니다.
                        .textKor(daRow[1].replace("\"", ""))
                        .startTime(Double.parseDouble(daRow[2]))
                        .endTime(Double.parseDouble(daRow[3]))
                        .actCode(actCode)
                        .actLabel(actLabel)
                        .build();

                voiceLogs.add(log);
            } catch (Exception e) {
                System.err.println(i + "번째 음성 로그 파싱 실패: " + e.getMessage());
            }
        }
        voiceLogRepository.saveAllAndFlush(voiceLogs);

        // [Step 5] 게임 이벤트(GameEvent) 처리
        List<GameEvent> gameEvents = new ArrayList<>();
        Pattern idPattern = Pattern.compile("^(\\d+)");

        for (String[] row : eventRows) {
            try {
                if (row.length < 2) continue;
                GameEvent event = new GameEvent();
                event.setGameMatch(savedMatch);
                event.setEventName(row[0]);

                // 시간 파싱 (정규화 없이 원본 ms 그대로 사용)
                if (row[1] != null && !row[1].isEmpty()) {
                    double rawTime = Double.parseDouble(row[1]);
                    event.setEventTime((long) Math.max(0, rawTime));
                }

                // Killer/Victim ID 추출
                if (row.length > 2 && row[2] != null && !row[2].isEmpty()) {
                    Matcher kMatcher = idPattern.matcher(row[2].trim());
                    if (kMatcher.find()) event.setKillerId(Long.parseLong(kMatcher.group(1)));
                }
                if (row.length > 3 && row[3] != null && !row[3].isEmpty()) {
                    Matcher vMatcher = idPattern.matcher(row[3].trim());
                    if (vMatcher.find()) event.setVictimId(Long.parseLong(vMatcher.group(1)));
                }
                gameEvents.add(event);
            } catch (Exception e) {
                System.err.println("이벤트 파싱 실패: " + e.getMessage());
            }
        }
        gameEventRepository.saveAllAndFlush(gameEvents);

        // [Step 6] 최종 Duration 계산 및 매치 정보 업데이트
        // 음성 로그와 게임 이벤트 중 가장 늦은 시간을 게임의 전체 길이로 잡습니다.
        double maxVoiceTime = voiceLogs.stream().mapToDouble(VoiceLog::getStartTime).max().orElse(0.0);
        double maxEventTime = gameEvents.stream().mapToDouble(e -> (double) e.getEventTime()).max().orElse(0.0);
        long finalDurationMs = (long) Math.max(maxVoiceTime, maxEventTime);

        savedMatch.setDuration(finalDurationMs);
        matchRepository.save(savedMatch); // 최종 업데이트

        System.out.println(">> [Import 완료] MatchID: " + savedMatch.getId() + ", Duration: " + finalDurationMs + "ms");

        // [Step 7] 분석 서비스 호출
        networkMetricService.analyzeAndSaveMetrics(savedMatch.getId());

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
        //if (line != null) match.setMatchCode(line.trim());
        System.out.println(">> [정보] 메타데이터 파일 확인됨: " + line);
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

    @Transactional
    public void deleteMatch(Long id) {
        // 1. 해당 ID가 있는지 먼저 확인 (안전장치)
        GameMatch match = matchRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 매치입니다. ID: " + id));

        // 2. 삭제 실행 (Cascade 설정 덕분에 연관 데이터도 같이 삭제됨)
        matchRepository.delete(match);
    }


}