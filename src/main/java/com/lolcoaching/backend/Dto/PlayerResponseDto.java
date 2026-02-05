package com.lolcoaching.backend.Dto;

import com.lolcoaching.backend.domain.Player;
import lombok.Getter;

@Getter
public class PlayerResponseDto {
    private int inGameId;
    private String summonerName;
    private String team;
    private String position;

    public PlayerResponseDto(Player player) {
        this.inGameId = player.getInGameId();
        this.summonerName = player.getSummonerName();
        this.team = player.getTeam();
        this.position = player.getPosition();
    }
}