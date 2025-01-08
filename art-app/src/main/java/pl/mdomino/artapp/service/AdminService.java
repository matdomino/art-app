package pl.mdomino.artapp.service;

import com.opencsv.CSVWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import pl.mdomino.artapp.model.Comment;
import pl.mdomino.artapp.model.Image;
import pl.mdomino.artapp.model.Tag;
import pl.mdomino.artapp.model.User;
import pl.mdomino.artapp.repo.CommentRepo;
import pl.mdomino.artapp.repo.ImageRepo;
import pl.mdomino.artapp.repo.TagRepo;
import pl.mdomino.artapp.repo.UserRepo;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
public class AdminService {
    private final Path fileUploadDir;
    private final ImageRepo imageRepo;
    private final CommentRepo commentRepo;
    private final TagRepo tagRepo;
    private final UserRepo userRepo;


    public AdminService(ImageRepo imageRepo,CommentRepo commentRepo, TagRepo tagRepo, UserRepo userRepo, @Value("${file.upload-dir}") String fileUploadDir) {
        this.imageRepo = imageRepo;
        this.fileUploadDir = Paths.get(fileUploadDir).normalize().toAbsolutePath();
        this.commentRepo = commentRepo;
        this.tagRepo = tagRepo;
        this.userRepo = userRepo;
    }

    @Transactional
    public String editImage(UUID imageId, MultipartFile file, String newTitle, String newDescription) {
        try {
            Image image = imageRepo.findById(imageId)
                    .orElseThrow(() -> new IllegalArgumentException("Image not found with the given ID"));

            if (newTitle != null && !newTitle.isBlank()) {
                image.setTitle(newTitle);
            }

            if (newDescription != null && !newDescription.isBlank()) {
                image.setDescription(newDescription);
            }

            String fileName = image.getFileName();

            if (file != null && !file.isEmpty()) {
                String originalFileName = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));
                String extension = originalFileName.substring(originalFileName.lastIndexOf("."));

                if (!extension.equalsIgnoreCase(".jpg") && !extension.equalsIgnoreCase(".jpeg")
                        && !extension.equalsIgnoreCase(".png") && !extension.equalsIgnoreCase(".gif")) {
                    throw new IllegalArgumentException("Invalid file type. Only JPEG, PNG, and GIF are allowed.");
                }

                if (fileName != null) {
                    Path oldFilePath = fileUploadDir.resolve(fileName);
                    if (Files.exists(oldFilePath)) {
                        try {
                            Files.delete(oldFilePath);
                        } catch (IOException e) {
                            throw new RuntimeException("Error deleting old file: " + e.getMessage(), e);
                        }
                    }
                }

                fileName = image.getImage_ID() + extension;
                image.setFileName(fileName);

                Path filePath = fileUploadDir.resolve(fileName);
                file.transferTo(filePath.toFile());
            }

            image.setUpdateDate(LocalDateTime.now());
            imageRepo.save(image);

            return fileName;

        } catch (Exception e) {
            throw new RuntimeException("Unexpected error occurred", e);
        }
    }

    @Transactional
    public Image deleteImage(UUID imageId) {
        try {
            Image image = imageRepo.findById(imageId)
                    .orElseThrow(() -> new IllegalArgumentException("Image not found with the given ID"));

            String fileName = image.getFileName();
            if (fileName != null) {
                Path filePath = fileUploadDir.resolve(fileName);
                try {
                    Files.delete(filePath);
                } catch (IOException e) {
                    throw new RuntimeException("Error deleting the file: " + e.getMessage(), e);
                }
            }

            imageRepo.delete(image);

            return image;

        } catch (Exception e) {
            throw new RuntimeException("Unexpected error occurred during image deletion", e);
        }
    }

    @Transactional
    public UUID editComment(Comment comment, UUID commentUuid) {
        Comment oldComment = commentRepo.findById(commentUuid)
                .orElseThrow(() -> new IllegalArgumentException("Comment not found with UUID: " + commentUuid));

        oldComment.setCommentText(comment.getCommentText());

        commentRepo.save(oldComment);

        return oldComment.getCommentID();
    }

    @Transactional
    public UUID deleteComment(UUID commentUuid) {
        Comment comment = commentRepo.findById(commentUuid)
                .orElseThrow(() -> new IllegalArgumentException("Comment not found with UUID: " + commentUuid));

        commentRepo.deleteById(commentUuid);

        return comment.getCommentID();
    }

    @Transactional
    public String addTagToImage(Tag givenTag, UUID imageUuid) {
        Image image = imageRepo.findById(imageUuid)
                .orElseThrow(() -> new IllegalArgumentException("Image not found with UUID: " + imageUuid));

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
    public String removeTagFromImage(String tagName, UUID imageUuid) {
        Image image = imageRepo.findById(imageUuid)
                .orElseThrow(() -> new IllegalArgumentException("Image not found with UUID: " + imageUuid));

        Tag tag = tagRepo.findByTagName(tagName)
                .orElseThrow(() -> new IllegalArgumentException("Tag not found with name: " + tagName));

        if (image.getTags().contains(tag)) {
            image.getTags().remove(tag);
            imageRepo.save(image);
        } else {
            throw new IllegalArgumentException("There is no tag with name: " + tagName);
        }

        return tagName;
    }

    @Transactional
    public User changeProfile(User user, UUID userUuid) {
        User existingUser = userRepo.findById(userUuid)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (user.getUsername() != null) {
            existingUser.setUsername(user.getUsername());
        }
        if (user.getProfileSummary() != null) {
            existingUser.setProfileSummary(user.getProfileSummary());
        }

        return userRepo.save(existingUser);
    }

    @Transactional
    public User deleteUser(UUID userUuid) {
        User user = userRepo.findById(userUuid)
                .orElseThrow(() -> new IllegalArgumentException("User not found with UUID: " + userUuid));

        userRepo.delete(user);

        return user;
    }

    @Transactional(readOnly = true)
    public byte[] exportDataToCSV() {
        List<Image> images = imageRepo.findAll();
        List<Comment> comments = commentRepo.findAll();
        List<User> users = userRepo.findAll();
        List<Tag> tags = tagRepo.findAll();

        StringWriter stringWriter = new StringWriter();
        try (CSVWriter csvWriter = new CSVWriter(stringWriter)) {
            csvWriter.writeNext(new String[]{"Type", "ID", "Details"});

            for (Image image : images) {
                csvWriter.writeNext(new String[]{"Image", image.getImage_ID().toString(), image.getTitle() + " - " + image.getDescription()});
            }

            for (Comment comment : comments) {
                csvWriter.writeNext(new String[]{"Comment", comment.getCommentID().toString(), comment.getCommentText()});
            }

            for (User user : users) {
                csvWriter.writeNext(new String[]{"User", user.getKeycloakID().toString(), user.getUsername()});
            }

            for (Tag tag : tags) {
                csvWriter.writeNext(new String[]{"Tag", tag.getTagID().toString(), tag.getTagName()});
            }
        } catch (Exception e) {
            throw new RuntimeException("Error generating CSV file", e);
        }

        return stringWriter.toString().getBytes(StandardCharsets.UTF_8);
    }
}
