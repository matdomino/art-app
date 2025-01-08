package pl.mdomino.artapp.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Setter
@Getter
@Entity
@Table(name = "users")
public class User {
    @Id
    @Column(name = "keycloak_ID", unique = true, nullable = false)
    private UUID keycloakID;

    @NotNull
    @Size(min = 4, max = 20, message = "Username must be between 4 and 20 characters")
    @Column(name = "username", length = 20, nullable = false)
    private String username;

    @Size(min = 0, max = 500, message = "Profile summary must be between 0 and 500 characters")
    @Column(name = "profile_summary", length = 500)
    private String profileSummary;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
