package pl.mdomino.artapp.web.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
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

    private void addAuthAttributes(Model model, Authentication auth) {
        String username = "";
        String userId = "";
        boolean isAdmin = false;

        if (auth instanceof OAuth2AuthenticationToken oauth && oauth.getPrincipal() instanceof OidcUser oidc) {
            username = oidc.getPreferredUsername();
            userId = oidc.getSubject();
            isAdmin = auth.getAuthorities().stream()
                    .anyMatch(authority -> "Admin".equals(authority.getAuthority()));
        }

        model.addAttribute("name", username);
        model.addAttribute("userId", userId);
        model.addAttribute("isAuthenticated", auth != null && auth.isAuthenticated());
        model.addAttribute("isAdmin", isAdmin);
    }

    @PostMapping("/image/{imageId}/comment")
    public String addComment(@PathVariable("imageId") UUID imageId,
                             @RequestParam("commentText") String commentText,
                             Authentication auth,
                             RedirectAttributes redirectAttributes,
                             Model model) {
        addAuthAttributes(model, auth);

        if (auth instanceof OAuth2AuthenticationToken oauth && oauth.getPrincipal() instanceof OidcUser oidc) {
            try {
                UUID userId = UUID.fromString(oidc.getSubject());
                Comment comment = new Comment();
                comment.setCommentText(commentText);
                commentService.addComment(comment, userId, imageId);
                redirectAttributes.addFlashAttribute("successMessage", "Comment added successfully!");
            } catch (Exception e) {
                redirectAttributes.addFlashAttribute("errorMessage", "Failed to add comment: " + e.getMessage());
            }
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Unauthorized: Unable to get user information.");
        }
        return "redirect:/image/" + imageId;
    }

    @PostMapping("/comment/{commentId}/delete")
    public String deleteComment(@PathVariable("commentId") UUID commentId,
                                @RequestParam("imageId") UUID imageId,
                                Authentication auth,
                                RedirectAttributes redirectAttributes,
                                Model model) {
        addAuthAttributes(model, auth);

        if (auth instanceof OAuth2AuthenticationToken oauth && oauth.getPrincipal() instanceof OidcUser oidc) {
            try {
                UUID userId = UUID.fromString(oidc.getSubject());
                commentService.deleteComment(commentId, userId);
                redirectAttributes.addFlashAttribute("successMessage", "Comment deleted successfully!");
            } catch (Exception e) {
                redirectAttributes.addFlashAttribute("errorMessage", "Failed to delete comment: " + e.getMessage());
            }
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Unauthorized: Unable to get user information.");
        }
        return "redirect:/image/" + imageId;
    }

    @GetMapping("/comment/{commentId}/edit")
    public String showEditCommentPage(@PathVariable("commentId") UUID commentId, Model model, Authentication auth) {
        addAuthAttributes(model, auth);

        if (auth instanceof OAuth2AuthenticationToken oauth && oauth.getPrincipal() instanceof OidcUser oidc) {
            UUID userId = UUID.fromString(oidc.getSubject());
            Comment comment = commentService.getCommentById(commentId).orElse(null);
            if (comment != null && comment.getAuthor().getKeycloakID().equals(userId)) {
                model.addAttribute("comment", comment);
                return "editcomment";
            }
        }
        return "redirect:/";
    }

    @PostMapping("/comment/{commentId}/edit")
    public String editComment(@PathVariable("commentId") UUID commentId,
                              @RequestParam("commentText") String commentText,
                              @RequestParam("imageId") UUID imageId,
                              Authentication auth,
                              RedirectAttributes redirectAttributes,
                              Model model) {
        addAuthAttributes(model, auth);

        if (auth instanceof OAuth2AuthenticationToken oauth && oauth.getPrincipal() instanceof OidcUser oidc) {
            try {
                UUID userId = UUID.fromString(oidc.getSubject());

                Comment comment = new Comment();
                comment.setCommentText(commentText);

                commentService.editComment(comment, userId, commentId);
                redirectAttributes.addFlashAttribute("successMessage", "Comment edited successfully!");
            } catch (Exception e) {
                redirectAttributes.addFlashAttribute("errorMessage", "Failed to edit comment: " + e.getMessage());
            }
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Unauthorized: Unable to get user information.");
        }
        return "redirect:/image/" + imageId;
    }
}
