package pl.mdomino.artapp.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pl.mdomino.artapp.model.Image;
import pl.mdomino.artapp.model.Rating;
import pl.mdomino.artapp.model.User;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RatingRepo extends JpaRepository<Rating, UUID> {
    Optional<Rating> findByImageAndAuthor(Image image, User author);

    List<Rating> findByImage(Image image);

    @Query("SELECT COALESCE(AVG(r.rating), 0) FROM Rating r WHERE r.image.image_ID = :imageId")
    Double findAverageRatingByImageId(@Param("imageId") UUID imageId);
}
