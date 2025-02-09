package pl.mdomino.artapp.web.controller;


import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import pl.mdomino.artapp.model.Comment;
import pl.mdomino.artapp.model.Image;
import pl.mdomino.artapp.model.dto.ImageDTO;
import pl.mdomino.artapp.service.CommentService;
import pl.mdomino.artapp.service.ImageService;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Controller
public class WebImageController {

    private final ImageService imageService;
    private final CommentService commentService;

    @Autowired
    public WebImageController(ImageService imageService, CommentService commentService) {
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

    @GetMapping("/")
    public String getIndex(Model model, Authentication auth) {
        addAuthAttributes(model, auth);

        List<ImageDTO> randomImages = imageService.getRandomImages();
        model.addAttribute("randomImages", randomImages);

        return "home.html";
    }

    @GetMapping("/admin")
    public String adminPage(Model model, Authentication auth) {
        addAuthAttributes(model, auth);

        return "admin.html";
    }

    @GetMapping("/search")
    public String search(@RequestParam("q") String query,
                         @RequestParam(defaultValue = "uploadDate") String sortBy,
                         @RequestParam(defaultValue = "false") boolean ascending,
                         @RequestParam(defaultValue = "0") int page,
                         @RequestParam(defaultValue = "10") int size,
                         Model model, Authentication auth) {
        addAuthAttributes(model, auth);

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
        addAuthAttributes(model, auth);

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
        addAuthAttributes(model, auth);

        Map<String, Object> imageDetails = imageService.getImageDetails(id);
        List<ImageDTO> suggestions = imageService.getSuggestions(id);
        List<Comment> comments = commentService.getComments(id);

        model.addAttribute("image", imageDetails);
        model.addAttribute("suggestions", suggestions);
        model.addAttribute("comments", comments);

        return "image.html";
    }

    @GetMapping("/upload")
    public String uploadImagePage(Model model, Authentication auth) {
        addAuthAttributes(model, auth);

        model.addAttribute("image", new Image());
        return "upload.html";
    }

    @PostMapping("/upload")
    public String uploadImage(@Valid @ModelAttribute("image") Image image,
                              BindingResult bindingResult,
                              @RequestParam("file") MultipartFile file,
                              Authentication auth,
                              Model model) {
        addAuthAttributes(model, auth);

        if (bindingResult.hasErrors()) {
            model.addAttribute("errorMessage", "Validation failed: " + bindingResult.getAllErrors());
            return "upload.html";
        }

        if (auth instanceof OAuth2AuthenticationToken oauth && oauth.getPrincipal() instanceof OidcUser oidc) {
            try {
                UUID userId = UUID.fromString(oidc.getSubject());
                imageService.addImage(image, file, userId);
                model.addAttribute("successMessage", "Image uploaded successfully!");
            } catch (Exception e) {
                model.addAttribute("errorMessage", "Upload failed: " + e.getMessage());
            }
        } else {
            model.addAttribute("errorMessage", "Unauthorized: Unable to get user information.");
        }

        return "upload.html";
    }
}
