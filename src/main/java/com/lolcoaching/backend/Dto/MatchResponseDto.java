package com.lolcoaching.backend.Dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatchResponseDto {
    private Long matchId;
    private String matchCode;
    private Long duration; // 전체 게임 시간 (밀리초 단위)

    private List<VoiceLogResponseDto> voiceLogs;
    private List<PlayerResponseDto> players;      // 엔티티 대신 DTO 사용
    private List<GameEventResponseDto> gameEvents; // 엔티티 대신 DTO 사용
}