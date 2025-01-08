package pl.mdomino.artapp.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.mdomino.artapp.model.Image;
import pl.mdomino.artapp.model.Tag;
import pl.mdomino.artapp.repo.ImageRepo;
import pl.mdomino.artapp.repo.TagRepo;

import java.util.*;

@Service
public class TagService {
    private final TagRepo tagRepo;
    private final ImageRepo imageRepo;

    @Autowired
    public TagService(TagRepo tagRepo, ImageRepo imageRepo) {
        this.tagRepo = tagRepo;
        this.imageRepo = imageRepo;
    }

    @Transactional
    public String addTagToImage(Tag givenTag, UUID imageUuid, UUID userUuid) {
        Image image = imageRepo.findById(imageUuid)
                .orElseThrow(() -> new IllegalArgumentException("Image not found with UUID: " + imageUuid));

        if (!userUuid.equals(image.getAuthor().getKeycloakID())) {
            throw new IllegalArgumentException("User is not authorized to edit this comment.");
        }

        Optional<Tag> optionalTag = tagRepo.findByTagName(givenTag.getTagName());

        Tag tag;
        if (optionalTag.isPresent()) {
            tag = optionalTag.get();
        } else {
            tag = new Tag();
            tag.setTagName(givenTag.getTagName());
            tag = tagRepo.save(tag);
        }

        if (!image.getTags().contains(tag)) {
            image.getTags().add(tag);
            imageRepo.save(image);
        }

        return tag.getTagName();
    }

    @Transactional
    public String removeTagFromImage(String tagName, UUID imageUuid, UUID userUuid) {
        Image image = imageRepo.findById(imageUuid)
                .orElseThrow(() -> new IllegalArgumentException("Image not found with UUID: " + imageUuid));

        if (!userUuid.equals(image.getAuthor().getKeycloakID())) {
            throw new IllegalArgumentException("User is not authorized to edit this comment.");
        }

        Tag tag = tagRepo.findByTagName(tagName)
                .orElseThrow(() -> new IllegalArgumentException("Tag not found with name: " + tagName));

        if (image.getTags().contains(tag)) {
            image.getTags().remove(tag);
            imageRepo.save(image);
        }

        return tagName;
    }

    @Transactional(readOnly = true)
    public List<Tag> getTagsByImageUuid(UUID imageUuid) {
        Image image = imageRepo.findById(imageUuid)
                .orElseThrow(() -> new IllegalArgumentException("Image not found with UUID: " + imageUuid));

        return new ArrayList<>(image.getTags());
    }
}
