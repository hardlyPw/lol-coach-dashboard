package com.lolcoaching.backend.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "network_metric", indexes = {
        @Index(name = "idx_match_time", columnList = "match_id, time_index")
})
public class NetworkMetric {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long matchId;

    @Column(nullable = false)
    private int timeIndex;

    @Column(nullable = false)
    private int sourceDa;

    @Column(nullable = false)
    private int targetDa;

    // ==========================================
    // ★ [추가] 단순 대화 횟수 (Count)
    // ==========================================
    @Column(nullable = false)
    private int count;
    // 기존 지표들
    private double density;
    private double cod;
    private double cid;

    @Column(columnDefinition = "TEXT")
    private String positionDaCounts;

    // ★ [추가] 포지션별 In Count (수신/응답 횟수)
    // 저장 예시: "TOP:0,JUG:1,MID:5,ADC:2,SUP:0"
    @Column(columnDefinition = "TEXT")
    private String positionReceiveCounts;

    @Builder
    public NetworkMetric(Long matchId, int timeIndex, int sourceDa, int targetDa,
                         int count, double density, double cod, double cid,
                         String positionDaCounts, String positionReceiveCounts) { // 생성자 추가
        this.matchId = matchId;
        this.timeIndex = timeIndex;
        this.sourceDa = sourceDa;
        this.targetDa = targetDa;
        this.count = count;
        this.density = density;
        this.cod = cod;
        this.cid = cid;
        this.positionDaCounts = positionDaCounts;       // 저장
        this.positionReceiveCounts = positionReceiveCounts; // 저장
    }
}