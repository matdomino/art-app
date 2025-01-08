package pl.mdomino.artapp.api.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import pl.mdomino.artapp.model.Comment;
import pl.mdomino.artapp.model.Image;
import pl.mdomino.artapp.model.Tag;
import pl.mdomino.artapp.model.User;
import pl.mdomino.artapp.service.AdminService;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("api/admin")
@PreAuthorize("hasRole('client_admin')")
public class AdminController {
    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @PutMapping("/{imageId}/editimage")
    public ResponseEntity<ApiResponse> editImage(
            @PathVariable UUID imageId,
            @RequestParam(value = "image", required = false) String imageJson,
            @RequestParam(value = "file", required = false) MultipartFile file) throws JsonProcessingException, IllegalArgumentException {

        if (imageJson == null && file == null) {
            return ResponseEntity.badRequest().body(new ApiResponse("At least one of 'image' or 'file' must be provided."));
        }

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

        String fileName = adminService.editImage(imageId, file, newTitle, newDescription);

        return ResponseEntity.ok(new ApiResponse("Saved modified file as " + fileName));
    }

    @DeleteMapping("/{imageUuid}/deleteimage")
    public ResponseEntity<ApiResponse> deleteImage(@PathVariable UUID imageUuid) {
        Image deletedImage = adminService.deleteImage(imageUuid);

        return ResponseEntity.ok(new ApiResponse("Deleted image " + deletedImage.getTitle()));
    }

    @PutMapping("/{commentUuid}/editcomment")
    public ResponseEntity<ApiResponse> editcomment(@Valid @RequestBody Comment comment, @PathVariable UUID commentUuid) {
        UUID commentId = adminService.editComment(comment, commentUuid);

        return ResponseEntity.ok(new ApiResponse("Comment " + commentId + " modified successfully"));
    }

    @DeleteMapping("/{commentUuid}/deletecomment")
    public ResponseEntity<ApiResponse> deleteComment(@PathVariable UUID commentUuid) {
        UUID removedComment = adminService.deleteComment(commentUuid);

        return ResponseEntity.ok(new ApiResponse("Comment " + removedComment + " deleted successfully"));
    }

    @PostMapping("/{imageUuid}/addtag")
    public ResponseEntity<ApiResponse> addTag(@PathVariable UUID imageUuid, @Valid @RequestBody Tag tag) {
        String tagName = adminService.addTagToImage(tag, imageUuid);

        return ResponseEntity.ok(new ApiResponse("Tag " + tagName + " added to image " + imageUuid));
    }

    @DeleteMapping("/{imageUuid}/deletetag/{tag}")
    public ResponseEntity<ApiResponse> deleteTag(@PathVariable UUID imageUuid, @PathVariable String tag) {
        String tagName = adminService.removeTagFromImage(tag, imageUuid);

        return ResponseEntity.ok(new ApiResponse("Removed " + tagName + " form image " + imageUuid));
    }

    @PutMapping("/{userUuid}/edituser")
    public ResponseEntity<ApiResponse> editUserProfile(@PathVariable UUID userUuid, @RequestBody @Valid User user) {
        User updatedUser = adminService.changeProfile(user, userUuid);

        return ResponseEntity.ok(new ApiResponse("Updated profile: " + updatedUser.getUsername()));
    }

    @DeleteMapping("/{userUuid}/deleteuser")
    public ResponseEntity<ApiResponse> deleteUser(@PathVariable UUID userUuid) {
        User deletedUser = adminService.deleteUser(userUuid);

        return ResponseEntity.ok(new ApiResponse("Deleted user: " + deletedUser.getKeycloakID()));
    }

    @GetMapping("/export/csv")
    public ResponseEntity<byte[]> exportDataToCSV() {
        byte[] csvData = adminService.exportDataToCSV();
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"exported_data.csv\"")
                .header("Content-Type", "text/csv")
                .body(csvData);
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
