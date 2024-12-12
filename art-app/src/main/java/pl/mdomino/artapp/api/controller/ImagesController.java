package pl.mdomino.artapp.api.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@RestController
@RequestMapping("api/images")
public class ImagesController {
    // Dp zmienienia - przenisc do service, dodac walidacje rozszerzenia, bardziej bylo jako test.

    private final Path fileUploadDir;

    public ImagesController(@Value("${file.upload-dir}") String fileUploadDir) throws IOException {
        this.fileUploadDir = Paths.get(fileUploadDir).normalize().toAbsolutePath();
        Files.createDirectories(this.fileUploadDir);
    }

    @PostMapping("/upload")
    public ResponseEntity<String> uploadImage(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("Empty file");
        }

        try {
            String orginalFileName = StringUtils.cleanPath(file.getOriginalFilename());
            String extension = orginalFileName.substring(orginalFileName.lastIndexOf("."));
            String fileName = UUID.randomUUID().toString() + extension;

            Path filePath = fileUploadDir.resolve(fileName);
            file.transferTo(filePath.toFile());

            return ResponseEntity.ok("Saved file as " + fileName);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("File save error: " + e.getMessage());
        }
    }
}
