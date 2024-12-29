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
@Table(name = "comments")
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

    @NotNull(message = "Comment cannot be null")
    @Size(min = 1, max = 500, message = "Comment must be between 1 and 500 characters")
    @Column(name = "comment_text", length = 500, nullable = false)
    private String commentText;

    @Column(name = "create_date")
    private LocalDateTime createDate;
}