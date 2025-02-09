package pl.mdomino.artapp.web.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pl.mdomino.artapp.model.Tag;
import pl.mdomino.artapp.service.TagService;
import java.util.UUID;

@Controller
@RequestMapping("/image/{imageId}/tags")
public class WebTagController {
    private final TagService tagService;

    public WebTagController(TagService tagService) {
        this.tagService = tagService;
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

    @PostMapping("/add")
    public String addTag(@PathVariable UUID imageId,
                         @RequestParam("tagName") String tagName,
                         Authentication auth,
                         RedirectAttributes redirectAttributes,
                         Model model) {
        addAuthAttributes(model, auth);


        if (!(auth instanceof OAuth2AuthenticationToken oauth) || !(oauth.getPrincipal() instanceof OidcUser oidc)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Unauthorized: Unable to get user information.");
            return "redirect:/image/" + imageId;
        }

        UUID userId = UUID.fromString(oidc.getSubject());
        try {
            Tag tag = new Tag();
            tag.setTagName(tagName);

            tagService.addTagToImage(tag, imageId, userId);
            redirectAttributes.addFlashAttribute("successMessage", "Tag added successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to add tag: " + e.getMessage());
        }
        return "redirect:/image/" + imageId;
    }

    @PostMapping("/remove")
    public String removeTag(@PathVariable UUID imageId,
                            @RequestParam("tagName") String tagName,
                            Authentication auth,
                            RedirectAttributes redirectAttributes,
                            Model model) {

        addAuthAttributes(model, auth);

        if (!(auth instanceof OAuth2AuthenticationToken oauth) || !(oauth.getPrincipal() instanceof OidcUser oidc)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Unauthorized: Unable to get user information.");
            return "redirect:/image/" + imageId;
        }

        UUID userId = UUID.fromString(oidc.getSubject());
        try {
            tagService.removeTagFromImage(tagName, imageId, userId);
            redirectAttributes.addFlashAttribute("successMessage", "Tag removed successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to remove tag: " + e.getMessage());
        }
        return "redirect:/image/" + imageId;
    }
}
