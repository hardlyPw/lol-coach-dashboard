package com.lolcoaching.backend.controller;

import com.lolcoaching.backend.Dto.MatchListDto;
import com.lolcoaching.backend.repository.GameMatchRepository;
import com.lolcoaching.backend.service.MatchImportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/matches")
@RequiredArgsConstructor
public class MatchUploadController {

    private final MatchImportService matchImportService;
    private final GameMatchRepository gameMatchRepository;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Long> uploadMatchData(
            @RequestPart("file") MultipartFile file,
            @RequestParam("matchCode") String matchCode // ★ 여기 추가!
    ) {
        try {
            // 인자 2개를 넘기도록 수정
            Long matchId = matchImportService.importMatch(file, matchCode);
            return ResponseEntity.ok(matchId);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/list")
    public ResponseEntity<List<MatchListDto>> getMatchList() {
        // DB에서 모든 매치를 가져와서 DTO 리스트로 변환
        List<MatchListDto> matches = gameMatchRepository.findAll().stream()
                .map(m -> new MatchListDto(m.getId(), m.getMatchCode()))
                .collect(Collectors.toList());

        return ResponseEntity.ok(matches);
    }


}