package pl.mdomino.artapp.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Setter
@Getter
@Entity
@Table(name = "images")
public class Image {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "image_ID")
    private UUID image_ID;

    @ManyToOne
    @JoinColumn(name = "author_ID", referencedColumnName = "keycloak_ID", nullable = false)
    private User author;

    @NotNull(message = "Title cannot be null")
    @Size(min = 1, max = 255, message = "Title must be between 1 and 255 characters")
    @Column(name = "title", length = 255, nullable = false)
    private String title;

    @NotNull(message = "Description cannot be null")
    @Size(min = 1, max = 500, message = "Description must be between 1 and 255 characters")
    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "upload_date")
    private LocalDateTime uploadDate;

    @Column(name = "update_date")
    private LocalDateTime updateDate;

    @ManyToMany
    @JoinTable(
            name = "ImageTags",
            joinColumns = @JoinColumn(name = "image_ID"),
            inverseJoinColumns = @JoinColumn(name = "tag_ID")
    )
    private Set<Tag> tags = new HashSet<>();

    @PreUpdate
    public void preUpdate() {
        this.updateDate = LocalDateTime.now();
    }
}

