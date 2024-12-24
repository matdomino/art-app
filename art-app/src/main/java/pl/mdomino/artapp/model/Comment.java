package pl.mdomino.artapp.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Setter
@Getter
@Entity
public class Comment {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "comment_ID")
    private UUID commentID;

    @ManyToOne
    @JoinColumn(name = "image_ID", referencedColumnName = "image_ID", nullable = false)
    private Image image;

    @ManyToOne
    @JoinColumn(name = "author_ID", referencedColumnName = "keycloak_ID", nullable = false)
    private User author;

    @Column(name = "comment_text", length = 500, nullable = false)
    private String commentText;

    @Column(name = "create_date")
    private LocalDateTime createDate;
}