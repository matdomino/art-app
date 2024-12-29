package pl.mdomino.artapp.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pl.mdomino.artapp.model.User;

import java.util.UUID;

@Repository
public interface UserRepo extends JpaRepository<User, UUID> {
}
