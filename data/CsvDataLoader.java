package com.lolcoaching.backend.loader;

import com.lolcoaching.backend.domain.GameEvent;
import com.lolcoaching.backend.domain.GameMatch;
import com.lolcoaching.backend.domain.Player;
import com.lolcoaching.backend.domain.VoiceLog;
import com.lolcoaching.backend.repository.GameEventRepository;
import com.lolcoaching.backend.repository.GameMatchRepository;
import com.lolcoaching.backend.repository.PlayerRepository;
import com.lolcoaching.backend.repository.VoiceLogRepository;
import com.lolcoaching.backend.service.NetworkMetricService; // ★ import 확인
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional; // 트랜잭션 추가

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class CsvDataLoader implements CommandLineRunner {

    private final GameMatchRepository matchRepository;
    private final VoiceLogRepository voiceLogRepository;
    private final GameEventRepository eventRepository;
    private final PlayerRepository playerRepository;

    // ★ 1. 서비스 주입 (이거 확인하세요!)
    private final NetworkMetricService networkMetricService;

    private final String[] DA_LABELS = {"I", "Q", "D", "C"};

    @Override
    @Transactional // ★ 데이터 저장과 분석을 한 트랜잭션으로 묶기 위해 추가
    public void run(String... args) throws Exception {

// 1. 중복 실행 방지
        if (playerRepository.count() > 0) { // (Player 데이터가 있으면 건너뜀으로 변경 추천)
            System.out.println(">> 데이터가 이미 존재합니다. 로딩을 건너뜁니다.");
            return;
        }

        System.out.println("========== 데이터 로딩 시작 ==========");

        File dir = new File("data");
        if (!dir.exists()) {
            System.out.println("!! data 폴더가 없습니다. 생략합니다.");
            return;
        }

        // ==========================================
        // [로직 1] 매치 생성
        // ==========================================
        String matchTitle = "Unknown Match";
        File[] files = dir.listFiles((d, name) -> name.startsWith("match_") && name.endsWith(".txt"));
        if (files != null && files.length > 0) {
            matchTitle = files[0].getName();
        }

        GameMatch match = new GameMatch();
        match.setMatchCode(matchTitle);
        matchRepository.save(match);
        System.out.println(">> 매치 생성 완료: " + matchTitle);


        // ==========================================
        // [로직 2] 플레이어 정보 저장 (여기를 고쳐야 함!)
        // ==========================================
        File infoFile = new File(dir, "info.csv");
        if (infoFile.exists()) {
            List<Player> players = new ArrayList<>();
            try (BufferedReader br = new BufferedReader(new FileReader(infoFile, StandardCharsets.UTF_8))) {
                br.readLine(); // 헤더 스킵 (첫 줄 날림)

                String line;
                while ((line = br.readLine()) != null) {
                    String[] cols = line.split(",");
                    // cols[0]: Team, cols[1]: ID, cols[2]: Name, cols[3]: Position

                    if(cols.length >= 4) {
                        Player player = new Player();
                        player.setGameMatch(match);
                        try {
                            player.setTeam(cols[0].trim());
                            player.setInGameId(Integer.parseInt(cols[1].trim()));
                            player.setSummonerName(cols[2].trim());

                            // ★★★ [핵심 수정] 여기서 변환 함수를 통과시킵니다! ★★★
                            String rawPosition = cols[3].trim(); // CSV에 있는 "MIDDLE"
                            String fixedPosition = standardizePosition(rawPosition); // "MID"로 변환
                            player.setPosition(fixedPosition); // 변환된 값 저장
                            // -----------------------------------------------------

                            players.add(player);
                        } catch (Exception e) {
                            System.out.println("!! 파싱 실패: " + line);
                        }
                    }
                }
            }
            playerRepository.saveAll(players);
            System.out.println(">> 플레이어 " + players.size() + "명 저장 완료 (포지션 변환 적용됨)");
        }
        // ==========================================
        // [로직 3] 보이스 로그 저장
        // ==========================================
        File fKor = new File(dir, "asr_result_kor.csv");
        File fDa = new File(dir, "da_result.csv");

        if (fKor.exists() && fDa.exists()) {
            List<VoiceLog> logs = new ArrayList<>();
            BufferedReader brKor = new BufferedReader(new FileReader(fKor, StandardCharsets.UTF_8));
            BufferedReader brDa = new BufferedReader(new FileReader(fDa));
            brKor.readLine(); brDa.readLine();

            String lineKor;
            while ((lineKor = brKor.readLine()) != null) {
                String lineDa = brDa.readLine();
                if (lineDa == null) break;

                // ... (기존 파싱 로직 그대로 사용) ...
                String[] korCols = lineKor.split(",");
                String[] daCols = lineDa.split(",");
                if (korCols.length < 5) continue;

                VoiceLog log = new VoiceLog();
                log.setGameMatch(match);

                // Player 연결
                try {
                    String[] speakerParts = korCols[0].split("-");
                    int inGameId = Integer.parseInt(speakerParts[0]);
                    playerRepository.findByGameMatchAndInGameId(match, inGameId)
                            .ifPresent(log::setPlayer);
                } catch (Exception e) { /* 무시 */ }

                // 시간/텍스트/Act 파싱 (기존 로직 유지)
                try {
                    log.setStartTime(Double.parseDouble(korCols[korCols.length - 5].trim()));
                    log.setEndTime(Double.parseDouble(korCols[korCols.length - 4].trim()));

                    StringBuilder sb = new StringBuilder();
                    for (int i = 1; i < korCols.length - 5; i++) sb.append(korCols[i]);
                    String text = sb.toString().replace("\"", "");
                    log.setTextKor(text);

                    if (daCols.length >= 2) {
                        int actCode = Integer.parseInt(daCols[daCols.length - 2].trim());
                        log.setActCode(actCode);
                        log.setActLabel(actCode >= 0 && actCode < DA_LABELS.length ? DA_LABELS[actCode] : "UNK");
                    }
                } catch (Exception e) { continue; }

                logs.add(log);
            }
            voiceLogRepository.saveAll(logs);
            System.out.println(">> 음성 로그 " + logs.size() + "개 저장 완료");
            brKor.close(); brDa.close();
        }

        // 4. 이벤트 파일 로딩 (유지)
        File fEvent = new File(dir, "event.csv");
        if (fEvent.exists()) {
            List<GameEvent> events = new ArrayList<>();
            try (BufferedReader br = new BufferedReader(new FileReader(fEvent))) {
                br.readLine();
                String line;
                while ((line = br.readLine()) != null) {
                    String[] cols = line.split(",");
                    if (cols.length < 2) continue;

                    if (cols[0].contains("Kill") || cols[0].contains("Destroy") || cols[0].contains("FirstBlood")) {
                        GameEvent event = new GameEvent();
                        event.setGameMatch(match);
                        event.setEventName(cols[0]);
                        try {
                            event.setEventTime(Long.parseLong(cols[1]));
                        } catch (NumberFormatException e) { continue; }

                        if (cols.length > 3) {
                            event.setKillerId(cols[2]);
                            event.setVictimId(cols[3]);
                        }
                        events.add(event);
                    }
                }
            }
            eventRepository.saveAll(events);
            System.out.println(">> 게임 이벤트 " + events.size() + "개 저장 완료");
        }

        System.out.println("========== 데이터 로딩 종료 ==========");


        System.out.println("========== 데이터 로딩 종료 ==========");

        // ★★★★★ [핵심 추가] 여기입니다! ★★★★★
        // 모든 데이터 저장이 끝난 후, 분석 서비스를 실행합니다.
        System.out.println(">> [분석 시작] 네트워크 지표(Density, Centralization) 계산 중...");

        networkMetricService.analyzeAndSaveMetrics(match.getId());

        System.out.println(">> [분석 완료] 네트워크 지표 DB 저장 끝!");
    }


    // CSV에 적힌 긴 이름(MIDDLE)을 짧은 이름(MID)으로 바꿔주는 함수
    private String standardizePosition(String rawPosition) {
        if (rawPosition == null) return "UNK";
        String upper = rawPosition.toUpperCase().trim();

        switch (upper) {
            case "JUNGLE":  return "JUG";
            case "MIDDLE":  return "MID";
            case "BOTTOM":  return "ADC"; // 보통 원딜이 BOTTOM
            case "UTILITY": return "SUP";
            case "TOP":     return "TOP";
            default:        return upper;
        }
    }
}