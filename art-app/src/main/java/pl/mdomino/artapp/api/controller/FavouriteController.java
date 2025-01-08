package pl.mdomino.artapp.api.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import pl.mdomino.artapp.model.Favorite;
import pl.mdomino.artapp.service.FavouriteService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("api/favourites")
public class FavouriteController {
    private final FavouriteService favouriteService;

    @Autowired
    public FavouriteController(FavouriteService favouriteService) {
        this.favouriteService = favouriteService;
    }

    @PostMapping("/{imageUuid}/add")
    public ResponseEntity<ApiResponse> addImageToFavorites(@PathVariable UUID imageUuid) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            return ResponseEntity.status(401).body(new ApiResponse("Unauthorized: Unable to get user information."));
        }

        UUID userUuid = UUID.fromString(jwt.getClaimAsString("sub"));

        Favorite favorite = favouriteService.addImageToFavorites(imageUuid, userUuid);

        return ResponseEntity.ok(new ApiResponse("Added image to favorites:" + favorite.getPreferenceID()));
    }

    @DeleteMapping("/{imageUuid}/remove")
    public ResponseEntity<ApiResponse> removeImageFromFavorites(@PathVariable UUID imageUuid) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            return ResponseEntity.status(401).body(new ApiResponse("Unauthorized: Unable to get user information."));
        }

        UUID userUuid = UUID.fromString(jwt.getClaimAsString("sub"));

        favouriteService.removeImageFromFavorites(imageUuid, userUuid);

        return ResponseEntity.ok(new ApiResponse("Removed image from favorites."));
    }

    @GetMapping("/")
    public ResponseEntity<List<Favorite>> getFavoritesForUser() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            return ResponseEntity.status(401).body(null);
        }

        UUID userUuid = UUID.fromString(jwt.getClaimAsString("sub"));

        List<Favorite> favorites = favouriteService.getFavoritesForUser(userUuid);

        return ResponseEntity.ok(favorites);
    }
}
