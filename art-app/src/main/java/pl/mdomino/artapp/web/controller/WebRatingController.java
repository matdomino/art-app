package pl.mdomino.artapp.web.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pl.mdomino.artapp.model.Rating;
import pl.mdomino.artapp.service.RatingService;
import java.util.UUID;

@Controller
public class WebRatingController {
    private final RatingService ratingService;

    public WebRatingController(RatingService ratingService) {
        this.ratingService = ratingService;
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

    @PostMapping("/image/{imageId}/rate")
    public String rateImage(@PathVariable("imageId") UUID imageId,
                            @RequestParam("rating") int ratingValue,
                            Authentication auth,
                            RedirectAttributes redirectAttributes,
                            Model model) {
        addAuthAttributes(model, auth);

        if (auth instanceof OAuth2AuthenticationToken oauth && oauth.getPrincipal() instanceof OidcUser oidc) {
            try {
                UUID userId = UUID.fromString(oidc.getSubject());
                Rating rating = new Rating();
                rating.setRating(ratingValue);

                ratingService.addOrUpdateRating(imageId, userId, rating);
                redirectAttributes.addFlashAttribute("successMessage", "Image rated successfully!");
            } catch (Exception e) {
                redirectAttributes.addFlashAttribute("errorMessage", "Failed to rate image: " + e.getMessage());
            }
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Unauthorized: Unable to get user information.");
        }
        return "redirect:/image/" + imageId;
    }

    @PostMapping("/image/{imageId}/unrate")
    public String removeRating(@PathVariable("imageId") UUID imageId,
                               Authentication auth,
                               RedirectAttributes redirectAttributes,
                               Model model) {
        addAuthAttributes(model, auth);

        if (auth instanceof OAuth2AuthenticationToken oauth && oauth.getPrincipal() instanceof OidcUser oidc) {
            try {
                UUID userId = UUID.fromString(oidc.getSubject());
                ratingService.deleteRating(imageId, userId);
                redirectAttributes.addFlashAttribute("successMessage", "Rating removed successfully!");
            } catch (Exception e) {
                redirectAttributes.addFlashAttribute("errorMessage", "Failed to remove rating: " + e.getMessage());
            }
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Unauthorized: Unable to get user information.");
        }
        return "redirect:/image/" + imageId;
    }
}
