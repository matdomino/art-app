package pl.mdomino.artapp.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pl.mdomino.artapp.model.Comment;
import pl.mdomino.artapp.model.Image;
import pl.mdomino.artapp.model.User;
import pl.mdomino.artapp.repo.CommentRepo;
import pl.mdomino.artapp.repo.ImageRepo;
import pl.mdomino.artapp.repo.UserRepo;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class CommentService {
    @Autowired
    private final CommentRepo commentRepo;

    @Autowired
    private final UserRepo userRepo;

    @Autowired
    private final ImageRepo imageRepo;

    public CommentService(CommentRepo commentRepo, UserRepo userRepo, ImageRepo imageRepo) {
        this.commentRepo = commentRepo;
        this.userRepo = userRepo;
        this.imageRepo = imageRepo;
    }

    public List<Comment> getComments(UUID imageUuid) {
        Image image = imageRepo.findById(imageUuid)
                .orElseThrow(() -> new IllegalArgumentException("Image not found with UUID: " + imageUuid));

        return commentRepo.findByImage(image);
    }

    public UUID addComment(Comment comment, UUID userUuid, UUID imageUuid) {
        User author = userRepo.findById(userUuid)
                .orElseThrow(() -> new IllegalArgumentException("User not found with UUID: " + userUuid));

        Image image = imageRepo.findById(imageUuid)
                .orElseThrow(() -> new IllegalArgumentException("Image not found with UUID: " + imageUuid));

        comment.setImage(image);
        comment.setAuthor(author);
        comment.setCreateDate(LocalDateTime.now());

        Comment savedComment = commentRepo.save(comment);

        return savedComment.getCommentID();
    }

    public UUID editComment(Comment comment, UUID userUuid, UUID commentUuid) {
        Comment oldComment = commentRepo.findById(commentUuid)
                .orElseThrow(() -> new IllegalArgumentException("Comment not found with UUID: " + commentUuid));

        if (!userUuid.equals(oldComment.getAuthor().getKeycloakID())) {
            throw new IllegalArgumentException("User is not authorized to edit this comment.");
        }

        oldComment.setCommentText(comment.getCommentText());

        commentRepo.save(oldComment);

        return oldComment.getCommentID();
    }

    public UUID deleteComment(UUID commentUuid, UUID userUuid) {
        Comment comment = commentRepo.findById(commentUuid)
                .orElseThrow(() -> new IllegalArgumentException("Comment not found with UUID: " + commentUuid));

        if (!userUuid.equals(comment.getAuthor().getKeycloakID())) {
            throw new IllegalArgumentException("User is not authorized to delete this comment.");
        }

        commentRepo.deleteById(commentUuid);

        return comment.getCommentID();
    }
}
