package pl.mdomino.artapp.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Setter
@Getter
@Entity
public class User {
    @Id
    @Column(name = "keycloak_ID", unique = true, nullable = false)
    private UUID keycloakID;

    @Column(name = "username", length = 20, nullable = false)
    private String username;

    @Column(name = "profile_summary", length = 500)
    private String profileSummary;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
