package pl.mdomino.artapp.service;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pl.mdomino.artapp.model.Image;
import pl.mdomino.artapp.model.Rating;
import pl.mdomino.artapp.model.User;
import pl.mdomino.artapp.repo.ImageRepo;
import pl.mdomino.artapp.repo.RatingRepo;
import pl.mdomino.artapp.repo.UserRepo;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class RatingService {
    private final RatingRepo ratingRepo;
    private final ImageRepo imageRepo;
    private final UserRepo userRepo;

    @Autowired
    public RatingService(RatingRepo ratingRepo, ImageRepo imageRepo, UserRepo userRepo) {
        this.ratingRepo = ratingRepo;
        this.imageRepo = imageRepo;
        this.userRepo = userRepo;
    }

    @Transactional
    public Rating addOrUpdateRating(UUID imageId, UUID userUuid, Rating rating) {
        User author = userRepo.findById(userUuid)
                .orElseThrow(() -> new IllegalArgumentException("User not found with UUID: " + userUuid));

        Image image = imageRepo.findById(imageId)
                .orElseThrow(() -> new IllegalArgumentException("Image not found"));

        Optional<Rating> existingRating = ratingRepo.findByImageAndAuthor(image, author);

        if (existingRating.isPresent()) {
            Rating existing = existingRating.get();
            existing.setRating(rating.getRating());
            return ratingRepo.save(existing);
        } else {
            rating.setImage(image);
            rating.setAuthor(author);
            return ratingRepo.save(rating);
        }
    }

    @Transactional(readOnly = true)
    public List<Rating> getAllRatingsForImage(UUID imageId) {
        Image image = imageRepo.findById(imageId)
                .orElseThrow(() -> new IllegalArgumentException("Image not found with ID: " + imageId));
        return ratingRepo.findByImage(image);
    }

    @Transactional
    public Rating deleteRating(UUID imageId, UUID userUuid) {
        User author = userRepo.findById(userUuid)
                .orElseThrow(() -> new IllegalArgumentException("User not found with UUID: " + userUuid));

        Image image = imageRepo.findById(imageId)
                .orElseThrow(() -> new IllegalArgumentException("Image not found with ID: " + imageId));

        Rating rating = ratingRepo.findByImageAndAuthor(image, author)
                .orElseThrow(() -> new IllegalArgumentException("Rating not found for the given image and user"));

        ratingRepo.delete(rating);

        return rating;
    }
}
