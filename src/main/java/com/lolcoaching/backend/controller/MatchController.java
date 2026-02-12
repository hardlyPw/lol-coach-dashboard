package com.lolcoaching.backend.controller;

import com.lolcoaching.backend.Dto.MatchResponseDto; // DTO 패키지명 주의 (Dto vs dto)
import com.lolcoaching.backend.service.MatchImportService; // ★ 서비스 임포트 필수!
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
    private final MatchImportService matchImportService;

    @GetMapping("/{id}") // 결과: GET http://3.34.82.181/api/matches/3
    public ResponseEntity<MatchResponseDto> getMatchDetail(@PathVariable Long id) {
        try {
            // 서비스에 위임해서 DTO 받아오기
            MatchResponseDto responseDto = matchImportService.getMatchDetail(id);
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
            @RequestParam("matchCode") String matchCode // ★ [추가] 이름 받기
    ) {
        try {
            // 서비스에 matchCode도 같이 넘김
            Long matchId = matchImportService.importMatch(file, matchCode);
            return ResponseEntity.ok(matchId);
        } catch (Exception e) {
            e.printStackTrace(); // 에러 로그 출력
            return ResponseEntity.badRequest().build();
        }
    }


}