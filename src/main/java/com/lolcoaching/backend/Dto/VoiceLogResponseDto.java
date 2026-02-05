package com.lolcoaching.backend.Dto;

import com.lolcoaching.backend.domain.VoiceLog;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class VoiceLogResponseDto {
    private Long logId;
    private String textKor;
    private Long startTime;  // 밀리초
    private Long endTime;    // 밀리초
    private int actCode;     // 0, 1, 2, 3
    private String actLabel; // I, Q, D, C

    // ★ [핵심] 프론트엔드 필터링을 위해 꼭 필요한 필드들
    private String position;     // "MID", "JUG" 등
    private String speakerName;  // 소환사명

    public VoiceLogResponseDto(VoiceLog log) {
        this.logId = log.getId();
        this.textKor = log.getTextKor();

        // 시간은 Long으로 내보냄
        this.startTime = log.getStartTime().longValue();
        this.endTime = log.getEndTime().longValue();

        this.actCode = log.getActCode();
        this.actLabel = log.getActLabel();

        // ★ Player 엔티티에서 정보 꺼내서 채우기 (Null 체크 필수)
        if (log.getPlayer() != null) {
            this.position = log.getPlayer().getPosition();
            this.speakerName = log.getPlayer().getSummonerName();
        } else {
            this.position = "UNKNOWN";
            this.speakerName = "Unknown";
        }
    }
}