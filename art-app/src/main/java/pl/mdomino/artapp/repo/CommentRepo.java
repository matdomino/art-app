package pl.mdomino.artapp.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pl.mdomino.artapp.model.Comment;
import pl.mdomino.artapp.model.Image;

import java.util.List;
import java.util.UUID;

@Repository
public interface CommentRepo extends JpaRepository<Comment, UUID> {
    List<Comment> findByImage(Image image);
}

