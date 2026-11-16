package edu.handong.web11.service;

import edu.handong.web11.domain.Attachment;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class FileStorageService {
    private static final Set<String> ALLOWED = Set.of("jpg", "jpeg", "png", "gif", "pdf", "txt");
    private final Path uploadDir;

    public FileStorageService(@Value("${app.upload-dir}") String path) throws IOException {
        uploadDir = Path.of(path).toAbsolutePath().normalize();
        Files.createDirectories(uploadDir);
    }

    public Attachment store(MultipartFile file) {

        if (file.isEmpty()) throw new IllegalArgumentException("빈 파일은 업로드할 수 없습니다."); // 내용이 없는 파일은 저장할 수 없으므로 처리를 중단.
        String original = file.getOriginalFilename(); // 사용자가 업로드할 때 사용한 원래 파일 이름을 가져옴.
        if (original == null || original.isBlank() || original.contains("/") || original.contains("\\") || original.contains(".."))
            throw new IllegalArgumentException("잘못된 파일 이름입니다.");
        // 파일 이름이 없거나 경로로 사용될 수 있는 문자가 포함되면 거부
        if (original.length() > 240) throw new IllegalArgumentException("파일 이름이 너무 깁니다."); // 파일 이름이 너무 길지 않도록 제한함.

        int dot = original.lastIndexOf('.'); // 마지막 점의 위치를 찾아 파일 이름과 확장자를 구분.

        String ext = dot < 0 ? "" : original.substring(dot + 1).toLowerCase(Locale.ROOT); // 확장자가 없으면 빈 문자열로 처리하고 확장자는 소문자로 바꾸어 JPG와 jpg 등을 동일하게 검사.

        if (!ALLOWED.contains(ext))
            throw new IllegalArgumentException("허용 형식: jpg, jpeg, png, gif, pdf, txt"); // 미리 정한 허용 확장자에 포함된 파일만 저장.

        if (file.getSize() > 10L * 1024 * 1024)
            throw new IllegalArgumentException("파일은 10MB 이하만 가능합니다."); // 파일 크기를 바이트 단위로 비교하여 10MB를 초과하면 거부.


        String stored = UUID.randomUUID() + "." + ext; // 같은 원본 이름의 파일도 구분할 수 있도록 UUID로 저장 이름을 만든다.
        Path target = uploadDir.resolve(stored).normalize(); // 업로드 폴더와 저장 이름을 합쳐 실제 저장 경로를 만들고, normalize()로 경로의 불필요한 부분을 정리한다.

        if (!target.startsWith(uploadDir)) throw new IllegalArgumentException("잘못된 파일 경로입니다."); // 저장 경로가 업로드 폴더 밖을 가리키면 파일을 저장하지 않는다.

        try (InputStream input = file.getInputStream()) { // 업로드 파일의 데이터를 읽을 스트림을 연다.

            Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING); // 읽어 온 파일 데이터를 실제 디스크의 저장 경로에 복사하고 같은 경로에 파일이 있으면 교체한다.

            String contentType = switch (ext) {
                case "jpg", "jpeg" -> "image/jpeg";
                case "png" -> "image/png";
                case "gif" -> "image/gif";
                case "pdf" -> "application/pdf";
                default -> "text/plain";
            }; // 요청에서 전달한 파일 타입 대신, 검사한 확장자를 기준으로 DB에 기록할 Content-Type을 정한다.

            // 파일에 대한 정보를 Attachment 객체로 만들어 반환한다.
            return new Attachment(original, stored, contentType, file.getSize());

        } catch (IOException e) {

            // 저장 도중 오류가 발생하면 일부만 만들어진 파일이 남지 않도록 해당 경로의 파일 삭제를 시도한다.
            try {
                Files.deleteIfExists(target);
            } catch (IOException ignored) {
                // 정리 중 발생한 오류보다 원래 파일 저장 오류를 전달한다.
            }

            // 파일 저장 실패를 HTTP 500 오류로 전달한다.
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR, "파일 저장 실패", e);
        }
    }

    public Resource load(Attachment attachment) {
        Path target = uploadDir.resolve(attachment.getStoredFilename()).normalize();
        if (!target.startsWith(uploadDir) || !Files.isRegularFile(target))
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "파일이 없습니다.");
        return new PathResource(target);
    }

    public void deleteIfExists(Attachment attachment) {
        try {
            Files.deleteIfExists(uploadDir.resolve(attachment.getStoredFilename()).normalize());
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "파일 삭제 실패", e);
        }
    }
}
