package pl.mdomino.artapp.web.controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pl.mdomino.artapp.model.Comment;
import pl.mdomino.artapp.model.Image;
import pl.mdomino.artapp.model.dto.ImageDTO;
import pl.mdomino.artapp.service.CommentService;
import pl.mdomino.artapp.service.ImageService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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

    @GetMapping("/image/download/{imageName}")
    public ResponseEntity<?> downloadImage(@PathVariable String imageName, Authentication auth) throws IOException {
        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized: You must be logged in to download images.");
        }

        Path imagePath = imageService.getImageByFilePath(imageName);

        if (!Files.exists(imagePath)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Image not found.");
        }

        byte[] imageContent = Files.readAllBytes(imagePath);
        String contentType = Files.probeContentType(imagePath);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(contentType != null ? contentType : "application/octet-stream"));
        headers.setContentDispositionFormData("attachment", imagePath.getFileName().toString());

        return new ResponseEntity<>(imageContent, headers, HttpStatus.OK);
    }

    @GetMapping("/image/{imageId}/edit")
    public String showEditImagePage(@PathVariable UUID imageId, Model model, Authentication auth) {
        addAuthAttributes(model, auth);

        if (!(auth instanceof OAuth2AuthenticationToken oauth) || !(oauth.getPrincipal() instanceof OidcUser oidc)) {
            return "redirect:/";
        }

        UUID userId = UUID.fromString(oidc.getSubject());
        Image image = imageService.getImageById(imageId).orElse(null);

        if (image != null && image.getAuthor().getKeycloakID().equals(userId)) {
            model.addAttribute("image", image);
            return "editimage";
        }
        return "redirect:/";
    }

    @PostMapping("/image/{imageId}/edit")
    public String editImage(@PathVariable UUID imageId,
                            @RequestParam(value = "title", required = false) String newTitle,
                            @RequestParam(value = "description", required = false) String newDescription,
                            @RequestParam(value = "file", required = false) MultipartFile file,
                            Authentication auth,
                            RedirectAttributes redirectAttributes) {

        if (!(auth instanceof OAuth2AuthenticationToken oauth) || !(oauth.getPrincipal() instanceof OidcUser oidc)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Unauthorized: Unable to get user information.");
            return "redirect:/image/" + imageId + "/edit";
        }

        UUID userId = UUID.fromString(oidc.getSubject());

        try {
            String fileName = imageService.editImage(imageId, file, userId, newTitle, newDescription);
            redirectAttributes.addFlashAttribute("successMessage", "Saved modified file as " + fileName);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to update image: " + e.getMessage());
        }

        return "redirect:/image/" + imageId;
    }
}
