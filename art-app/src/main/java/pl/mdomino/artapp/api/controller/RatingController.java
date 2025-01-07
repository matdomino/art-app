package pl.mdomino.artapp.api.controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import pl.mdomino.artapp.model.Rating;
import pl.mdomino.artapp.service.RatingService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("api/images")
public class RatingController {
    private final RatingService ratingService;

    @Autowired
    public RatingController(RatingService ratingService) {
        this.ratingService = ratingService;
    }

    @PostMapping("/{imageUuid}/rate")
    public ResponseEntity<ApiResponse> rateImage(@PathVariable UUID imageUuid, @Valid @RequestBody Rating rating) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            return ResponseEntity.status(401).body(new ApiResponse("Unauthorized: Unable to get user information."));
        }

        UUID userUuid = UUID.fromString(jwt.getClaimAsString("sub"));

        Rating savedRating = ratingService.addOrUpdateRating(imageUuid, userUuid, rating);

        return ResponseEntity.ok(new ApiResponse("Rated image: " + imageUuid + " rating: " + savedRating.getRatingID()));
    }

    @GetMapping("/{imageUuid}/ratings")
    public ResponseEntity<List<Rating>> getRatings(@PathVariable UUID imageUuid) {
        List<Rating> ratings = ratingService.getAllRatingsForImage(imageUuid);

        return ResponseEntity.ok(ratings);
    }

    @DeleteMapping("/{imageUuid}/deleterating")
    public ResponseEntity<ApiResponse> deleteRating(@PathVariable UUID imageUuid) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            return ResponseEntity.status(401).body(new ApiResponse("Unauthorized: Unable to get user information."));
        }

        UUID userUuid = UUID.fromString(jwt.getClaimAsString("sub"));

        Rating deletedRating = ratingService.deleteRating(imageUuid, userUuid);

        return ResponseEntity.ok(new ApiResponse("Deleted rating: " + deletedRating.getRatingID()));
    }
}
