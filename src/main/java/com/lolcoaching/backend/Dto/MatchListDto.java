package com.lolcoaching.backend.Dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class MatchListDto {
    private Long id;
    private String matchCode; // 게임 이름 (예: T1 vs GEN 1set)
}