package pl.mdomino.artapp.web.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pl.mdomino.artapp.service.AdminService;
import pl.mdomino.artapp.service.CommentService;
import pl.mdomino.artapp.service.ImageService;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Controller
@PreAuthorize("hasAuthority('Admin')")
public class WebAdminController {
    private final AdminService adminService;
    private final ImageService imageService;
    private final CommentService commentService;

    public WebAdminController(AdminService adminService, ImageService imageService, CommentService commentService) {
        this.adminService = adminService;
        this.imageService = imageService;
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

    @GetMapping("/admin")
    public String adminPage(Model model, Authentication auth) {
        addAuthAttributes(model, auth);
        return "admin.html";
    }

    @GetMapping("/admin/export/csv")
    public ResponseEntity<byte[]> exportDataToCSV() {
        byte[] csvData = adminService.exportDataToCSV();
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"exported_data.csv\"")
                .header("Content-Type", "text/csv")
                .body(csvData);
    }

    @GetMapping("/admin/image/{imageId}")
    public String adminImagePage(@PathVariable UUID imageId, Model model, Authentication auth) {
        addAuthAttributes(model, auth);

        Map<String, Object> imageDetails = imageService.getImageDetails(imageId);
        List<?> comments = commentService.getComments(imageId);
        List<?> suggestions = imageService.getSuggestions(imageId);

        model.addAttribute("image", imageDetails);
        model.addAttribute("comments", comments);
        model.addAttribute("suggestions", suggestions);

        return "adminimagepage.html";
    }

    @PostMapping("/admin/image/{imageId}/delete")
    public String deleteImage(@PathVariable UUID imageId, RedirectAttributes redirectAttributes) {
        adminService.deleteImage(imageId);
        redirectAttributes.addFlashAttribute("successMessage", "Image deleted successfully!");
        return "redirect:/admin";
    }

    @PostMapping("/admin/comment/{commentId}/delete")
    public String deleteComment(@PathVariable UUID commentId, @RequestParam UUID imageId, RedirectAttributes redirectAttributes) {
        adminService.deleteComment(commentId);
        redirectAttributes.addFlashAttribute("successMessage", "Comment deleted successfully!");
        return "redirect:/admin/image/" + imageId;
    }

    @PostMapping("/admin/image/{imageId}/tag/delete")
    public String deleteTag(@PathVariable UUID imageId, @RequestParam("tagName") String tagName, RedirectAttributes redirectAttributes) {
        adminService.removeTagFromImage(tagName, imageId);
        redirectAttributes.addFlashAttribute("successMessage", "Tag removed successfully!");
        return "redirect:/admin/image/" + imageId;
    }
}
