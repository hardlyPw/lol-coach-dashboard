package com.lolcoaching.backend.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter @Setter
@Builder // ★ 이 줄이 꼭 있어야 .builder()를 쓸 수 있습니다!
@NoArgsConstructor // JPA는 기본 생성자가 필수입니다.
@AllArgsConstructor // Builder 패턴은 전체 생성자가 필요합니다.
public class VoiceLog {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ==========================================
    // [삭제] 더 이상 여기에 직접 저장하지 않습니다!
    // ==========================================
    // private String speakerId;
    // private String speakerName;
    // private String position;

    // ==========================================
    // [추가] 대신 Player 엔티티와 연결합니다.
    // ==========================================
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id") // DB에는 'player_id'라는 숫자로 저장됨
    private Player player;

    // ------------------------------------------

    private Double startTime; // ms 단위
    private Double endTime;

    @Column(columnDefinition = "TEXT")
    private String textKor;   // 한글 대화

    private int actCode;      // 0, 1, 2, 3
    private String actLabel;  // "I", "Q", "D", "C"

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_id")
    private GameMatch gameMatch;
}