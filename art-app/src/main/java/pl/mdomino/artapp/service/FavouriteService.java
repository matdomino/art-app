package pl.mdomino.artapp.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pl.mdomino.artapp.model.Favorite;
import pl.mdomino.artapp.model.Image;
import pl.mdomino.artapp.model.User;
import pl.mdomino.artapp.repo.FavoriteRepo;
import pl.mdomino.artapp.repo.ImageRepo;
import pl.mdomino.artapp.repo.UserRepo;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class FavouriteService {
    private final FavoriteRepo favoriteRepo;
    private final ImageRepo imageRepo;
    private final UserRepo userRepo;

    @Autowired
    public FavouriteService(FavoriteRepo favoriteRepo, ImageRepo imageRepo, UserRepo userRepo) {
        this.favoriteRepo = favoriteRepo;
        this.imageRepo = imageRepo;
        this.userRepo = userRepo;
    }

    public Favorite addImageToFavorites(UUID imageUuid, UUID userUuid) {
        User author = userRepo.findById(userUuid)
                .orElseThrow(() -> new IllegalArgumentException("User not found with UUID: " + userUuid));

        Image image = imageRepo.findById(imageUuid)
                .orElseThrow(() -> new IllegalArgumentException("Image not found with ID: " + imageUuid));

        Optional<Favorite> existingFavorite = favoriteRepo.findByUserAndImage(author, image);

        if (existingFavorite.isPresent()) {
            throw new IllegalArgumentException("Image is already in favorites for user: " + userUuid);
        }

        Favorite favorite = new Favorite();
        favorite.setImage(image);
        favorite.setUser(author);
        favorite.setCreateDate(LocalDateTime.now());

        favoriteRepo.save(favorite);

        return favorite;
    }

    public void removeImageFromFavorites(UUID imageUuid, UUID userUuid) {
        User author = userRepo.findById(userUuid)
                .orElseThrow(() -> new IllegalArgumentException("User not found with UUID: " + userUuid));

        Image image = imageRepo.findById(imageUuid)
                .orElseThrow(() -> new IllegalArgumentException("Image not found with ID: " + imageUuid));

        Favorite favorite = favoriteRepo.findByUserAndImage(author, image)
                .orElseThrow(() -> new IllegalArgumentException("Image is not in favorites for user: " + userUuid));

        favoriteRepo.delete(favorite);
    }

    public List<Favorite> getFavoritesForUser(UUID userUuid) {
        User author = userRepo.findById(userUuid)
                .orElseThrow(() -> new IllegalArgumentException("User not found with UUID: " + userUuid));

        return favoriteRepo.findAllByUser(author);
    }
}

