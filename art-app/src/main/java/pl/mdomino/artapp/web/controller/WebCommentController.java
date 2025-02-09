package pl.mdomino.artapp.web.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import pl.mdomino.artapp.model.Comment;
import pl.mdomino.artapp.service.CommentService;

import java.util.UUID;

@Controller
public class WebCommentController {
    private final CommentService commentService;

    @Autowired
    public WebCommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @PostMapping("/image/{imageId}/comment")
    public String addComment(@PathVariable("imageId") UUID imageId,
                             @RequestParam("commentText") String commentText,
                             Authentication auth) {
        if (auth instanceof OAuth2AuthenticationToken oauth && oauth.getPrincipal() instanceof OidcUser oidc) {
            UUID userId = UUID.fromString(oidc.getSubject());
            Comment comment = new Comment();
            comment.setCommentText(commentText);
            commentService.addComment(comment, userId, imageId);
        }
        return "redirect:/image/" + imageId;
    }
}
