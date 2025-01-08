package pl.mdomino.artapp.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.*;

@Setter
@Getter
@Entity
@Table(name = "images")
public class Image {
    @Id
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

    @Column(name = "file_name")
    private String fileName;

    @ManyToMany
    @JoinTable(
            name = "ImageTags",
            joinColumns = @JoinColumn(name = "image_ID"),
            inverseJoinColumns = @JoinColumn(name = "tag_ID")
    )
    private Set<Tag> tags = new HashSet<>();

    @JsonIgnore
    @OneToMany(mappedBy = "image", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Rating> ratings = new ArrayList<>();

    @JsonIgnore
    @OneToMany(mappedBy = "image", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Favorite> favorites = new ArrayList<>();

    @PreUpdate
    public void preUpdate() {
        this.updateDate = LocalDateTime.now();
    }
}

