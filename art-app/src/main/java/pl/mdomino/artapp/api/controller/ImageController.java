package pl.mdomino.artapp.api.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
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
import pl.mdomino.artapp.model.dto.ImageDTO;
import pl.mdomino.artapp.repo.ImageRepo;
import pl.mdomino.artapp.repo.UserRepo;
import pl.mdomino.artapp.service.ImageService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("api/images")
public class ImageController {
    private final ImageService imageService;
    private final UserRepo userRepo;

    @Autowired
    public ImageController(ImageService imageService, UserRepo userRepo) {
        this.imageService = imageService;
        this.userRepo = userRepo;
    }

    @PostMapping("/upload")
    public ResponseEntity<ApiResponse> uploadImage(@Valid @RequestParam("image") String imageJson, @RequestParam("file") MultipartFile file) throws JsonProcessingException {
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

        String fileName = imageService.addImage(image, file, userUuid);

        return ResponseEntity.ok(new ApiResponse("Saved file as " + fileName));
    }

    @PutMapping("/{imageId}/edit")
    public ResponseEntity<ApiResponse> editImage(
            @PathVariable UUID imageId,
            @RequestParam(value = "image", required = false) String imageJson,
            @RequestParam(value = "file", required = false) MultipartFile file) throws JsonProcessingException, IllegalArgumentException {

        var authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            return ResponseEntity.status(401).body(new ApiResponse("Unauthorized: Unable to get user information."));
        }

        if (imageJson == null && file == null) {
            return ResponseEntity.badRequest().body(new ApiResponse("At least one of 'image' or 'file' must be provided."));
        }

        UUID userUuid = UUID.fromString(jwt.getClaimAsString("sub"));

        String newTitle = null;
        String newDescription = null;

        if (imageJson != null) {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode imageNode = objectMapper.readTree(imageJson);

            List<String> allowedFields = List.of("title", "description");
            List<String> unknownFields = new ArrayList<>();
            imageNode.fieldNames().forEachRemaining(field -> {
                if (!allowedFields.contains(field)) {
                    unknownFields.add(field);
                }
            });
            if (!unknownFields.isEmpty()) {
                return ResponseEntity.badRequest().body(new ApiResponse("Unknown fields: " + String.join(", ", unknownFields)));
            }

            newTitle = imageNode.has("title") ? imageNode.get("title").asText() : null;
            newDescription = imageNode.has("description") ? imageNode.get("description").asText() : null;

            Image tempImage = new Image();
            tempImage.setTitle(newTitle);
            tempImage.setDescription(newDescription);
            var violations = validateImage(tempImage);
            if (!violations.isEmpty()) {
                return ResponseEntity.badRequest().body(new ApiResponse(String.join(", ", violations)));
            }
        }

        String fileName = imageService.editImage(imageId, file, userUuid, newTitle, newDescription);

        return ResponseEntity.ok(new ApiResponse("Saved modified file as " + fileName));
    }

    @GetMapping("/download/{imageName}")
    public ResponseEntity<?> getImage(@PathVariable String imageName) throws IOException {
        Path imagePath = imageService.getImageByFilePath(imageName);

        byte[] imageContent = Files.readAllBytes(imagePath);

        String contentType = Files.probeContentType(imagePath);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(contentType != null ? contentType : "application/octet-stream"));
        headers.setContentDispositionFormData("attachment", imagePath.getFileName().toString());

        return new ResponseEntity<>(imageContent, headers, HttpStatus.OK);
    }

    @GetMapping("/preview/{imageName}")
    public ResponseEntity<?> previewImage(@PathVariable String imageName) throws IOException {
        Path imagePath = imageService.getImageByFilePath(imageName);

        byte[] imageContent = Files.readAllBytes(imagePath);

        String contentType = Files.probeContentType(imagePath);
        if (contentType == null) {
            contentType = "application/octet-stream";
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(contentType));

        return new ResponseEntity<>(imageContent, headers, HttpStatus.OK);
    }

    @GetMapping("/random")
    public List<ImageDTO> getRandomImages() {
        return imageService.getRandomImages();
    }

    @GetMapping("/search")
    public List<Image> searchImages(
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "uploadDate") String sortBy,
            @RequestParam(defaultValue = "false") boolean ascending,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        return imageService.searchImages(query, sortBy, ascending, page, size);
    }

    @GetMapping("/{imageUuid}/getdetails")
    public ResponseEntity<Map<String, Object>> getImageDetails(@PathVariable UUID imageUuid) {
        Map<String, Object> imageDetails = imageService.getImageDetails(imageUuid);

        return ResponseEntity.ok(imageDetails);
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

    @GetMapping("/{imageUuid}/suggestions")
    public ResponseEntity<List<ImageDTO>> getImageSuggestions(@PathVariable UUID imageUuid) {
        List<ImageDTO> suggestions = imageService.getSuggestions(imageUuid);
        return ResponseEntity.ok(suggestions);
    }

    @DeleteMapping("/{imageUuid}/delete")
    public ResponseEntity<ApiResponse> deleteImage(@PathVariable UUID imageUuid) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            return ResponseEntity.status(401).body(new ApiResponse("Unauthorized: Unable to get user information."));
        }

        UUID userUuid = UUID.fromString(jwt.getClaimAsString("sub"));

        Image deletedImage = imageService.deleteImage(imageUuid, userUuid);

        return ResponseEntity.ok(new ApiResponse("Deleted image " + deletedImage.getTitle()));
    }

    @GetMapping("/top")
    public ResponseEntity<List<Map<String, Object>>> getTopImages(
            @RequestParam("sortBy") String sortBy,
            @RequestParam(defaultValue = "10") int limit) {

        List<Map<String, Object>> topImages;

        if (limit <= 0) {
            return ResponseEntity.badRequest().body(null);
        }

        if ("favourites".equalsIgnoreCase(sortBy)) {
            topImages = imageService.getTopImagesByFavorites(limit);
        } else if ("ratings".equalsIgnoreCase(sortBy)) {
            topImages = imageService.getTopImagesByRatings(limit);
        } else {
            return ResponseEntity.badRequest().body(null);
        }

        return ResponseEntity.ok(topImages);
    }
}
