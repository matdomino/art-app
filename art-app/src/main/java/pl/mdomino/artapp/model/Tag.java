package pl.mdomino.artapp.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Setter
@Getter
@Entity
public class Tag {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "tag_ID")
    private UUID tagID;

    @Column(name = "tag_name", length = 30, nullable = false, unique = true)
    private String tagName;

    @ManyToMany(mappedBy = "tags")
    private Set<Image> images = new HashSet<>();
}