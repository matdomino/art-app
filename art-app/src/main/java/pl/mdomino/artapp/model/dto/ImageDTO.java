package pl.mdomino.artapp.model.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Setter
@Getter
public class ImageDTO {
    private UUID image_ID;
    private String title;
    private String description;

    public ImageDTO(UUID image_ID, String title, String description) {
        this.image_ID = image_ID;
        this.title = title;
        this.description = description;
    }
}