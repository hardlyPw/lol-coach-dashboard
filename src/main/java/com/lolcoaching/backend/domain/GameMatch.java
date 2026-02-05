package com.lolcoaching.backend.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Entity
@Getter @Setter
public class GameMatch {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String matchCode;

    // ★ [추가] 아까 빨간 줄 떴던 부분 해결을 위해 꼭 필요합니다!
    private Long duration;

    // ==========================================
    // 양방향 관계에서 무한 루프를 방지하기 위한 설정
    // ==========================================

    @JsonIgnore
    @OneToMany(mappedBy = "gameMatch", cascade = CascadeType.ALL)
    private List<VoiceLog> logs;

    @JsonIgnore
    @OneToMany(mappedBy = "gameMatch", cascade = CascadeType.ALL)
    private List<GameEvent> events;

    @JsonIgnore
    @OneToMany(mappedBy = "gameMatch", cascade = CascadeType.ALL)
    private List<Player> players;
}