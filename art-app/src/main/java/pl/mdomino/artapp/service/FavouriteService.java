package pl.mdomino.artapp.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.mdomino.artapp.model.Favorite;
import pl.mdomino.artapp.model.Image;
import pl.mdomino.artapp.model.User;
import pl.mdomino.artapp.repo.FavouriteRepo;
import pl.mdomino.artapp.repo.ImageRepo;
import pl.mdomino.artapp.repo.UserRepo;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class FavouriteService {
    private final FavouriteRepo favouriteRepo;
    private final ImageRepo imageRepo;
    private final UserRepo userRepo;

    @Autowired
    public FavouriteService(FavouriteRepo favouriteRepo, ImageRepo imageRepo, UserRepo userRepo) {
        this.favouriteRepo = favouriteRepo;
        this.imageRepo = imageRepo;
        this.userRepo = userRepo;
    }

    @Transactional
    public Favorite addImageToFavorites(UUID imageUuid, UUID userUuid) {
        User author = userRepo.findById(userUuid)
                .orElseThrow(() -> new IllegalArgumentException("User not found with UUID: " + userUuid));

        Image image = imageRepo.findById(imageUuid)
                .orElseThrow(() -> new IllegalArgumentException("Image not found with ID: " + imageUuid));

        Optional<Favorite> existingFavorite = favouriteRepo.findByUserAndImage(author, image);

        if (existingFavorite.isPresent()) {
            throw new IllegalArgumentException("Image is already in favorites for user: " + userUuid);
        }

        Favorite favorite = new Favorite();
        favorite.setImage(image);
        favorite.setUser(author);
        favorite.setCreateDate(LocalDateTime.now());

        favouriteRepo.save(favorite);

        return favorite;
    }

    @Transactional
    public void removeImageFromFavorites(UUID imageUuid, UUID userUuid) {
        User author = userRepo.findById(userUuid)
                .orElseThrow(() -> new IllegalArgumentException("User not found with UUID: " + userUuid));

        Image image = imageRepo.findById(imageUuid)
                .orElseThrow(() -> new IllegalArgumentException("Image not found with ID: " + imageUuid));

        Favorite favorite = favouriteRepo.findByUserAndImage(author, image)
                .orElseThrow(() -> new IllegalArgumentException("Image is not in favorites for user: " + userUuid));

        favouriteRepo.delete(favorite);
    }

    @Transactional(readOnly = true)
    public List<Favorite> getFavoritesForUser(UUID userUuid) {
        User author = userRepo.findById(userUuid)
                .orElseThrow(() -> new IllegalArgumentException("User not found with UUID: " + userUuid));

        return favouriteRepo.findAllByUser(author);
    }
}

