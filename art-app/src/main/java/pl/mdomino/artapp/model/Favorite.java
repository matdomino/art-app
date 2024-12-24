package pl.mdomino.artapp.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Setter
@Getter
@Entity
public class Favorite {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "preference_ID")
    private UUID preferenceID;

    @ManyToOne
    @JoinColumn(name = "user_ID", referencedColumnName = "keycloak_ID", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "image_ID", referencedColumnName = "image_ID", nullable = false)
    private Image image;

    @Column(name = "create_date")
    private LocalDateTime createDate;
}

