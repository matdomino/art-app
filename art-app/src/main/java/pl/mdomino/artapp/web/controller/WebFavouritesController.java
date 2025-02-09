package pl.mdomino.artapp.web.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pl.mdomino.artapp.service.FavouriteService;
import java.util.List;
import java.util.UUID;

@Controller
public class WebFavouritesController {
    private final FavouriteService favouritesService;

    @Autowired
    public WebFavouritesController(FavouriteService favouritesService) {
        this.favouritesService = favouritesService;
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

    @PostMapping("/image/{imageId}/favorite")
    public String addImageToFavorites(@PathVariable("imageId") UUID imageId,
                                      Authentication auth,
                                      RedirectAttributes redirectAttributes,
                                      Model model) {
        addAuthAttributes(model, auth);

        if (auth instanceof OAuth2AuthenticationToken oauth && oauth.getPrincipal() instanceof OidcUser oidc) {
            try {
                UUID userId = UUID.fromString(oidc.getSubject());
                favouritesService.addImageToFavorites(imageId, userId);
                redirectAttributes.addFlashAttribute("successMessage", "Image added to favorites successfully!");
            } catch (Exception e) {
                redirectAttributes.addFlashAttribute("errorMessage", "Failed to add image to favorites: " + e.getMessage());
            }
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Unauthorized: Unable to get user information.");
        }
        return "redirect:/image/" + imageId;
    }

    @PostMapping("/image/{imageId}/unfavorite")
    public String removeImageFromFavorites(@PathVariable("imageId") UUID imageId,
                                           Authentication auth,
                                           RedirectAttributes redirectAttributes,
                                           Model model) {
        addAuthAttributes(model, auth);

        if (auth instanceof OAuth2AuthenticationToken oauth && oauth.getPrincipal() instanceof OidcUser oidc) {
            try {
                UUID userId = UUID.fromString(oidc.getSubject());
                favouritesService.removeImageFromFavorites(imageId, userId);
                redirectAttributes.addFlashAttribute("successMessage", "Image removed from favorites successfully!");
            } catch (Exception e) {
                redirectAttributes.addFlashAttribute("errorMessage", "Failed to remove image from favorites: " + e.getMessage());
            }
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Unauthorized: Unable to get user information.");
        }
        return "redirect:/favourites";
    }

    @GetMapping("/favourites")
    public String getUserFavorites(Model model, Authentication auth) {
        addAuthAttributes(model, auth);

        if (auth instanceof OAuth2AuthenticationToken oauth && oauth.getPrincipal() instanceof OidcUser oidc) {
            UUID userId = UUID.fromString(oidc.getSubject());
            List<?> favorites = favouritesService.getFavoritesForUser(userId);
            model.addAttribute("favorites", favorites);
        } else {
            model.addAttribute("errorMessage", "Unauthorized: Unable to get user information.");
        }
        return "favourites.html";
    }
}
