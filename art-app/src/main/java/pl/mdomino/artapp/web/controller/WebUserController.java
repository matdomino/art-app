package pl.mdomino.artapp.web.controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pl.mdomino.artapp.model.User;
import pl.mdomino.artapp.service.UserService;

import java.util.UUID;

@Controller
public class WebUserController {
    private final UserService userService;

    @Autowired
    public WebUserController(UserService userService) {
        this.userService = userService;
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

    @GetMapping("/user/{id}")
    public String user(@PathVariable("id") UUID id, Model model, Authentication auth) {
        model.addAttribute("name",
                auth instanceof OAuth2AuthenticationToken oauth && oauth.getPrincipal() instanceof OidcUser oidc
                        ? oidc.getPreferredUsername()
                        : "");
        addAuthAttributes(model, auth);

        User userProfile = userService.getUserProfile(id);

        model.addAttribute("user", userProfile);

        return "user.html";
    }

    @PostMapping("/user/{id}/edit")
    public String editUserProfile(@PathVariable("id") UUID id,
                                  @ModelAttribute @Valid User user,
                                  Authentication auth,
                                  RedirectAttributes redirectAttributes,
                                  Model model) {
        addAuthAttributes(model, auth);

        if (!(auth instanceof OAuth2AuthenticationToken oauth) || !(oauth.getPrincipal() instanceof OidcUser oidc)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Unauthorized: Unable to get user information.");
            return "redirect:/user/" + id;
        }

        UUID userUuid = UUID.fromString(oidc.getSubject());
        try {
            User updatedUser = userService.changeProfile(user, userUuid);
            redirectAttributes.addFlashAttribute("successMessage", "Profile updated successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to update profile: " + e.getMessage());
        }

        return "redirect:/user/" + id;
    }
}
