package com.lolcoaching.backend.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@Builder            // 1. 빌더 패턴 사용
@NoArgsConstructor  // 2. [필수] JPA가 빈 객체를 생성할 때 필요함 (이게 없어서 에러 남)
@AllArgsConstructor
public class Player {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ★ CsvDataLoader에서 setInGameId(...)를 쓰려면 이 변수가 있어야 함
    private Integer inGameId;

    // ★ setSummonerName(...)용
    private String summonerName;

    // ★ setPosition(...)용
    private String position;

    // ★ setTeam(...)용 (이게 없어서 오류 났을 가능성이 큽니다)
    private String team;

    // ★ setGameMatch(...)용 (Repository 오류 해결을 위해 이름을 match -> gameMatch로 통일)
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_id")
    private GameMatch gameMatch;
}