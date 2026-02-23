package com.lolcoaching.backend.controller;

import com.lolcoaching.backend.Dto.MatchResponseDto; // DTO 패키지명 주의 (Dto vs dto)
import com.lolcoaching.backend.service.MatchService; // ★ 서비스 임포트 필수!
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/matches") // ★ 1. 프론트엔드와 맞추기 위해 복수형(matches)으로 변경
@RequiredArgsConstructor
public class MatchController {

    // ★ 2. Repository 대신 'Service'를 불러와야 합니다.
    // (Controller는 Service에게 시키고, Service가 Repository를 쓰는 구조입니다)
    private final MatchService matchService;

    @PostMapping("/import")
    public ResponseEntity<?> importMatch(
            @RequestParam("zipFile") MultipartFile file,
            @RequestParam("matchCode") String matchCode,
            @RequestParam("myTeam") String myTeam // "BLUE" 또는 "RED"가 들어옴
    ) {
        try {
            Long matchId = matchService.importMatch(file, matchCode, myTeam);
            return ResponseEntity.ok(matchId);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/{id}") // 결과: GET http://3.34.82.181/api/matches/3
    public ResponseEntity<MatchResponseDto> getMatchDetail(@PathVariable Long id) {
        try {
            // 서비스에 위임해서 DTO 받아오기
            MatchResponseDto responseDto = matchService.getMatchDetail(id);
            return ResponseEntity.ok(responseDto);
        } catch (IllegalArgumentException e) {
            // ID에 해당하는 매치가 없으면 404 리턴
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            // 그 외 에러는 500 리턴 (로그 확인용)
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping
    public ResponseEntity<Long> uploadMatch(
            @RequestParam("file") MultipartFile file,
            @RequestParam("matchCode") String matchCode,
            @RequestParam("myTeam") String myTeam // ★ [추가] 프론트에서 보낸 BLUE/RED 정보를 받습니다.
    ) {
        try {
            // 이제 3개의 인자(file, matchCode, myTeam)를 모두 넘겨줍니다.
            Long matchId = matchService.importMatch(file, matchCode, myTeam);
            return ResponseEntity.ok(matchId);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMatch(@PathVariable Long id) {
        matchService.deleteMatch(id);
        return ResponseEntity.ok().build();
    }


}