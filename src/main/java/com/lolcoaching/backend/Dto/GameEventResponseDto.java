package com.lolcoaching.backend.Dto;

import com.lolcoaching.backend.domain.GameEvent;
import lombok.Getter;

@Getter
public class GameEventResponseDto {
    private String eventName;
    private Long eventTime;
    private Long killerId; // ★ Integer -> Long (엔티티와 타입 통일)
    private Long victimId; // ★ Integer -> Long

    public GameEventResponseDto(GameEvent event) {
        this.eventName = event.getEventName();
        this.eventTime = event.getEventTime();

        // ★ [수정] 이제 복잡한 객체 탐색 없이 바로 ID를 가져옵니다.
        // GameEvent에 getKillerId(), getVictimId() 메서드가 생겼기 때문입니다.
        this.killerId = event.getKillerId();
        this.victimId = event.getVictimId();
    }
}