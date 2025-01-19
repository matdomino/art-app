package pl.mdomino.artapp.web.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import pl.mdomino.artapp.model.dto.ImageDTO;
import pl.mdomino.artapp.service.ImageService;

import java.util.List;
import java.util.Objects;

@Controller
public class WebImageController {
    private final ImageService imageService;

    @Autowired
    public WebImageController(ImageService imageService) {
        this.imageService = imageService;
    }

    @GetMapping("/")
    public String getIndex(Model model, Authentication auth) {
        if (auth != null) {
            System.out.println("=== Authorities: ===");
            auth.getAuthorities().forEach(a -> System.out.println(a.getAuthority()));
        }

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

    @GetMapping("/test")
    public String test(Model model, Authentication auth) {
        return "test.html";
    }

    @GetMapping("/admin")
    public String adminPage(Model model, Authentication auth) {
        return "admin.html";
    }
}
