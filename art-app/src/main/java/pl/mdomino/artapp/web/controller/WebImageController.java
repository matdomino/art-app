package pl.mdomino.artapp.web.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import pl.mdomino.artapp.model.Image;
import pl.mdomino.artapp.model.dto.ImageDTO;
import pl.mdomino.artapp.service.ImageService;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Controller
public class WebImageController {
    private final ImageService imageService;

    @Autowired
    public WebImageController(ImageService imageService) {
        this.imageService = imageService;
    }

    @GetMapping("/")
    public String getIndex(Model model, Authentication auth) {
        model.addAttribute("name",
                auth instanceof OAuth2AuthenticationToken oauth && oauth.getPrincipal() instanceof OidcUser oidc
                        ? oidc.getPreferredUsername()
                        : "");
        model.addAttribute("isAuthenticated",
                auth != null && auth.isAuthenticated());
        model.addAttribute("isAdmin",
                auth != null && auth.getAuthorities().stream().anyMatch(authority -> {
                    return Objects.equals("Admin", authority.getAuthority());
                }));

        List<ImageDTO> randomImages = imageService.getRandomImages();
        model.addAttribute("randomImages", randomImages);

        return "home.html";
    }

    @GetMapping("/admin")
    public String adminPage(Model model, Authentication auth) {
        model.addAttribute("name",
                auth instanceof OAuth2AuthenticationToken oauth && oauth.getPrincipal() instanceof OidcUser oidc
                        ? oidc.getPreferredUsername()
                        : "");
        model.addAttribute("isAuthenticated",
                auth != null && auth.isAuthenticated());
        return "admin.html";
    }

    @GetMapping("/search")
    public String search(@RequestParam("q") String query,
                         @RequestParam(defaultValue = "uploadDate") String sortBy,
                         @RequestParam(defaultValue = "false") boolean ascending,
                         @RequestParam(defaultValue = "0") int page,
                         @RequestParam(defaultValue = "10") int size,
                         Model model, Authentication auth) {
        model.addAttribute("name",
                auth instanceof OAuth2AuthenticationToken oauth && oauth.getPrincipal() instanceof OidcUser oidc
                        ? oidc.getPreferredUsername()
                        : "");
        model.addAttribute("isAuthenticated",
                auth != null && auth.isAuthenticated());
        model.addAttribute("query", query);

        model.addAttribute("ascending", ascending);
        model.addAttribute("sortBy", sortBy);
        model.addAttribute("page", page);
        model.addAttribute("size", size);
        model.addAttribute("q", query);

        List<Image> images = imageService.searchImages(query, sortBy, ascending, page, size);
        model.addAttribute("images", images);

        return "search.html";
    }

    @GetMapping("/topimages")
    public String topImages(@RequestParam(defaultValue = "favourites") String sortBy,
                            @RequestParam(defaultValue = "10") int limit,
                            Model model, Authentication auth) {
        model.addAttribute("name",
                auth instanceof OAuth2AuthenticationToken oauth && oauth.getPrincipal() instanceof OidcUser oidc
                        ? oidc.getPreferredUsername()
                        : "");
        model.addAttribute("isAuthenticated", auth != null && auth.isAuthenticated());

        List<Map<String, Object>> topImages;
        if (limit > 0) {
            if ("favourites".equalsIgnoreCase(sortBy)) {
                topImages = imageService.getTopImagesByFavorites(limit);
            } else if ("ratings".equalsIgnoreCase(sortBy)) {
                topImages = imageService.getTopImagesByRatings(limit);
            } else {
                model.addAttribute("error", "Nieprawidłowy parametr sortowania");
                return "topimages.html";
            }

            System.out.println("Zawartość topImages:");
            topImages.forEach(image -> System.out.println(image));

            model.addAttribute("topImages", topImages);
        } else {
            model.addAttribute("error", "Limit musi być większy niż 0");
        }


        model.addAttribute("sortBy", sortBy);
        model.addAttribute("limit", limit);

        return "topimages.html";
    }

    @GetMapping("/image/{id}")
    public String image(@PathVariable("id") UUID id, Model model, Authentication auth) {
        model.addAttribute("name",
                auth instanceof OAuth2AuthenticationToken oauth && oauth.getPrincipal() instanceof OidcUser oidc
                        ? oidc.getPreferredUsername()
                        : "");
        model.addAttribute("isAuthenticated", auth != null && auth.isAuthenticated());

        Map<String, Object> imageDetails = imageService.getImageDetails(id);

        model.addAttribute("image", imageDetails);

        return "image.html";
    }
}
