package pl.mdomino.artapp.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Setter
@Getter
@Entity
@Table(name = "tags")
public class Tag {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "tag_ID")
    private UUID tagID;

    @NotNull(message = "Tag name cannot be null")
    @Size(min = 1, max = 30, message = "Tag name must be between 1 and 30 characters")
    @Column(name = "tag_name", length = 30, nullable = false, unique = true)
    private String tagName;

    @ManyToMany(mappedBy = "tags")
    private Set<Image> images = new HashSet<>();
}