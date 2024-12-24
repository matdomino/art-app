package pl.mdomino.artapp.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Setter
@Getter
@Entity
public class DataLog {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "data_log_ID")
    private UUID dataLogID;

    @ManyToOne
    @JoinColumn(name = "admin_ID", referencedColumnName = "keycloak_ID", nullable = false)
    private User admin;

    @Column(name = "action", length = 10, nullable = false)
    @Enumerated(EnumType.STRING)
    private Action action;

    @Column(name = "file_name", length = 255)
    private String fileName;

    @Column(name = "log_date")
    private LocalDateTime logDate;

    public enum Action {
        IMPORT, EXPORT
    }
}
