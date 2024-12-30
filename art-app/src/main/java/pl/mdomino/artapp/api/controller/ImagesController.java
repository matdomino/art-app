package pl.mdomino.artapp.api.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import pl.mdomino.artapp.model.Image;
import pl.mdomino.artapp.model.User;
import pl.mdomino.artapp.repo.UserRepo;
import pl.mdomino.artapp.service.ImageService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("api/images")
public class ImagesController {
    private final ImageService imageService;
    private final UserRepo userRepo;

    @Autowired
    public ImagesController(ImageService imageService, UserRepo userRepo) {
        this.imageService = imageService;
        this.userRepo = userRepo;
    }

    @PostMapping("/uploadimage")
    public ResponseEntity<ApiResponse> uploadImage(@Valid @RequestParam("image") String imageJson, @RequestParam("file") MultipartFile file) {
        try {
            var authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
                return ResponseEntity.status(401).body(new ApiResponse("Unauthorized: Unable to get user information."));
            }

            ObjectMapper objectMapper = new ObjectMapper();
            Image image = objectMapper.readValue(imageJson, Image.class);

            var violations = validateImage(image);
            if (!violations.isEmpty()) {
                return ResponseEntity.badRequest().body(new ApiResponse(String.join(", ", violations)));
            }

            UUID userUuid = UUID.fromString(jwt.getClaimAsString("sub"));

            User author = userRepo.findById(userUuid)
                    .orElseThrow(() -> new IllegalArgumentException("User not found with UUID: " + userUuid));

            image.setAuthor(author);

            String fileName = imageService.addImage(image, file);

            return ResponseEntity.ok(new ApiResponse("Saved file as " + fileName));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ApiResponse(e.getMessage()));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @GetMapping("/download/{imageId}")
    public ResponseEntity<?> getImage(@PathVariable UUID imageId) {
        try {
            Path imagePath = imageService.getImageFileById(imageId);

            byte[] imageContent = Files.readAllBytes(imagePath);

            String contentType = Files.probeContentType(imagePath);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(contentType != null ? contentType : "application/octet-stream"));
            headers.setContentDispositionFormData("attachment", imagePath.getFileName().toString());

            return new ResponseEntity<>(imageContent, headers, HttpStatus.OK);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ApiResponse(e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ApiResponse(e.getMessage()));
        }
    }

    private List<String> validateImage(Image image) {
        List<String> errors = new ArrayList<>();
        if (image.getTitle() == null || image.getTitle().isEmpty()) {
            errors.add("Title cannot be null or empty.");
        }
        if (image.getTitle() != null && (image.getTitle().length() < 1 || image.getTitle().length() > 255)) {
            errors.add("Title must be between 1 and 255 characters.");
        }
        return errors;
    }
}
