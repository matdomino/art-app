package pl.mdomino.artapp.repo;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pl.mdomino.artapp.model.Image;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ImageRepo extends JpaRepository<Image, UUID> {
    Optional<Image> findById(UUID id);

    @Query(value = "SELECT * FROM images ORDER BY RANDOM() LIMIT 10", nativeQuery = true)
    List<Image> findRandomImages();

    @Query("SELECT i FROM Image i " +
            "LEFT JOIN i.tags t " +
            "WHERE LOWER(i.title) LIKE LOWER(CONCAT('%', :query, '%')) " +
            "OR LOWER(t.tagName) LIKE LOWER(CONCAT('%', :query, '%')) " +
            "OR LOWER(i.author.username) LIKE LOWER(CONCAT('%', :query, '%'))")
    Page<Image> findByQuery(String query, Pageable pageable);

    @Query("SELECT i FROM Image i " +
            "JOIN i.tags t " +
            "WHERE t.tagName IN :tagNames " +
            "AND i.image_ID != :imageId")
    List<Image> findSimilarImagesByTags(@Param("tagNames") List<String> tagNames, @Param("imageId") UUID imageId);
}
