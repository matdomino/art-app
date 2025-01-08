package pl.mdomino.artapp.api.controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import pl.mdomino.artapp.model.Tag;
import pl.mdomino.artapp.service.TagService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("api/tags")
public class TagController {
    private final TagService tagService;

    @Autowired
    public TagController(TagService tagService) {
        this.tagService = tagService;
    }

    @PostMapping("/{imageUuid}/add")
    public ResponseEntity<ApiResponse> previewImage(@PathVariable UUID imageUuid, @Valid @RequestBody Tag tag) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            return ResponseEntity.status(401).body(new ApiResponse("Unauthorized: Unable to get user information."));
        }

        UUID userUuid = UUID.fromString(jwt.getClaimAsString("sub"));

        String tagName = tagService.addTagToImage(tag, imageUuid, userUuid);

        return ResponseEntity.ok(new ApiResponse("Tag " + tagName + " added to image " + imageUuid));
    }

    @DeleteMapping("/{imageUuid}/deletetag/{tag}")
    public ResponseEntity<ApiResponse> deleteTag(@PathVariable UUID imageUuid, @PathVariable String tag) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            return ResponseEntity.status(401).body(new ApiResponse("Unauthorized: Unable to get user information."));
        }

        UUID userUuid = UUID.fromString(jwt.getClaimAsString("sub"));

        String tagName = tagService.removeTagFromImage(tag, imageUuid, userUuid);

        return ResponseEntity.ok(new ApiResponse("Removed " + tagName + " form image " + imageUuid));
    }

    @GetMapping("/{imageUuid}")
    public ResponseEntity<List<Tag>> getTags(@PathVariable UUID imageUuid) {
        List<Tag> tags = tagService.getTagsByImageUuid(imageUuid);

        return ResponseEntity.ok(tags);
    }
}
