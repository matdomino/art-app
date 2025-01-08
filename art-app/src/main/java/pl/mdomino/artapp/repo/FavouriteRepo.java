package pl.mdomino.artapp.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pl.mdomino.artapp.model.Favorite;
import pl.mdomino.artapp.model.Image;
import pl.mdomino.artapp.model.User;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FavouriteRepo extends JpaRepository<Favorite, UUID> {
    Optional<Favorite> findByUserAndImage(User user, Image image);

    List<Favorite> findAllByUser(User user);
}

