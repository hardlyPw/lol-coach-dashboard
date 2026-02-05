package com.lolcoaching.backend.domain;

import jakarta.persistence.*;
import lombok.Data; // @Getter, @Setter 포함

@Entity
@Data
@Table(name = "game_event")
public class GameEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_id")
    private GameMatch gameMatch;

    private Long eventTime;
    private String eventName;

    // ★★★ [수정] Player 객체 대신 단순 ID(숫자)로 저장합니다 ★★★

    // DB 컬럼명: killer_id (자동 매핑됨)
    // Lombok 생성 메서드: setKillerId(Long id)
    private Long killerId;

    // DB 컬럼명: victim_id (자동 매핑됨)
    // Lombok 생성 메서드: setVictimId(Long id)
    private Long victimId;
}