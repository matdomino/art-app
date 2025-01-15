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
    private String fileName;

    public ImageDTO(UUID image_ID, String title, String description, String fileName) {
        this.image_ID = image_ID;
        this.title = title;
        this.description = description;
        this.fileName = fileName;
    }
}