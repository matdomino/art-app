package pl.mdomino.artapp.api.controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import pl.mdomino.artapp.model.Comment;
import pl.mdomino.artapp.service.CommentService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("api/comments")
public class CommentController {
    private final CommentService commentService;

    @Autowired
    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @GetMapping("/{imageUuid}")
    public ResponseEntity<List<Comment>> getComments(@PathVariable UUID imageUuid) {
        List<Comment> comments = commentService.getComments(imageUuid);

        return ResponseEntity.ok(comments);
    }

    @PostMapping("/{imageUuid}/add")
    public ResponseEntity<ApiResponse> addComment(@Valid @RequestBody Comment comment, @PathVariable UUID imageUuid) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
                return ResponseEntity.status(401).body(new ApiResponse("Unauthorized: Unable to get user information."));
        }

        UUID userUuid = UUID.fromString(jwt.getClaimAsString("sub"));

        UUID commentId = commentService.addComment(comment, userUuid, imageUuid);

        return ResponseEntity.ok(new ApiResponse("Comment added successfully with ID: " + commentId));
    }

    @PutMapping("/{commentUuid}/edit")
    public ResponseEntity<ApiResponse> editcomment(@Valid @RequestBody Comment comment, @PathVariable UUID commentUuid) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            return ResponseEntity.status(401).body(new ApiResponse("Unauthorized: Unable to get user information."));
        }

        UUID userUuid = UUID.fromString(jwt.getClaimAsString("sub"));

        UUID commentId = commentService.editComment(comment, userUuid, commentUuid);

        return ResponseEntity.ok(new ApiResponse("Comment " + commentId + " modified successfully"));
    }

    @DeleteMapping("/{commentUuid}/delete")
    public ResponseEntity<ApiResponse> deleteComment(@PathVariable UUID commentUuid) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            return ResponseEntity.status(401).body(new ApiResponse("Unauthorized: Unable to get user information."));
        }

        UUID userUuid = UUID.fromString(jwt.getClaimAsString("sub"));

        UUID removedComment = commentService.deleteComment(commentUuid, userUuid);

        return ResponseEntity.ok(new ApiResponse("Comment " + removedComment + " deleted successfully"));
    }
}
