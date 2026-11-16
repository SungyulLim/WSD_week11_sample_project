package edu.handong.web11.controller;

import edu.handong.web11.domain.Attachment;
import edu.handong.web11.repository.AttachmentRepository;
import edu.handong.web11.service.FileStorageService;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/attachments")
public class AttachmentController {
    private final AttachmentRepository attachments;
    private final FileStorageService storage;
    public AttachmentController(AttachmentRepository attachments, FileStorageService storage) {
        this.attachments = attachments; this.storage = storage;
    }
    @GetMapping("/{id}/download") // 첨부파일 ID가 포함된 다운로드 요청을 처리.
    public ResponseEntity<Resource> download(@PathVariable Long id) {
        Attachment file = attachments.findById(id).orElseThrow(() ->
            new ResponseStatusException(HttpStatus.NOT_FOUND, "첨부파일이 없습니다."));
        // URL에서 받은 ID로 DB의 첨부파일 정보를 조회한다.
        // 해당 ID가 없으면 HTTP 404 오류를 반환한다.

        Resource resource = storage.load(file);
        // DB에 기록된 저장 파일명을 이용해 실제 파일을 불러온다.
        // 실제 파일이 없는 경우의 검사는 storage.load()에서 처리한다.

        String disposition = ContentDisposition.attachment() // 브라우저가 파일을 첨부파일로 다운로드하도록 헤더 값을 만든다.
            .filename(file.getOriginalFilename(), StandardCharsets.UTF_8) // UUID 저장명이 아닌 원본 이름으로 다운로드하도록 설정하고 한글이 포함된 파일명을 전달 가능하도록 UTF-8을 사용한다
            .build().toString(); // 설정한 내용을 HTTP 헤더에 넣을 문자열로 완성한다.

        return ResponseEntity.ok() // 정상 처리 상태인 HTTP 200 응답을 만든다.
            .contentType(MediaType.APPLICATION_OCTET_STREAM) // 응답 데이터를 일반적인 바이너리 파일 형식으로 지정한다.
            .header(HttpHeaders.CONTENT_DISPOSITION, disposition) // 다운로드 방식과 원본 파일명이 담긴 헤더를 응답에 추가한다.
            .body(resource); // 실제 파일을 읽어 전달할 Resource를 응답 본문으로 지정한다.
    }
    // STUDENT TODO: GET /attachments/{id}/view (image preview).

    @GetMapping("/{id}/view")
    public ResponseEntity<Resource> view(@PathVariable Long id) {
        Attachment file = attachments.findById(id).orElseThrow(() ->
                new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "첨부파일이 없습니다."));

        String contentType = file.getContentType();

        boolean isImage = "image/jpeg".equals(contentType) || "image/png".equals(contentType) || "image/gif".equals(contentType);

        if (!isImage) {
            throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "이미지 파일만 미리보기할 수 있습니다.");
        }

        Resource resource = storage.load(file);

        String disposition = ContentDisposition.inline()
                .filename(file.getOriginalFilename(), StandardCharsets.UTF_8)
                .build()
                .toString();

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition)
                .header("X-Content-Type-Options", "nosniff")
                .body(resource);
    }
}
