package pl.mdomino.artapp.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pl.mdomino.artapp.model.Image;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ImageRepo extends JpaRepository<Image, UUID> {
    Optional<Image> findById(UUID id);
}
