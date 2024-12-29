package pl.mdomino.artapp.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Setter
@Getter
@Entity
@Table(name = "ratings")
public class Rating {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "rating_ID")
    private UUID ratingID;

    @ManyToOne
    @JoinColumn(name = "image_ID", referencedColumnName = "image_ID", nullable = false)
    private Image image;

    @ManyToOne
    @JoinColumn(name = "author_ID", referencedColumnName = "keycloak_ID", nullable = false)
    private User author;

    @Column(name = "rating", nullable = false)
    private int rating;

    @PrePersist
    @PreUpdate
    public void validateRating() {
        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException("Rating must be between 1 and 5");
        }
    }
}
