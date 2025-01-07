package pl.mdomino.artapp.model.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class TagDTO {
    private UUID tagID;
    private String tagName;
}