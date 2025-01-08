package pl.mdomino.artapp.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import pl.mdomino.artapp.model.Image;
import pl.mdomino.artapp.model.Tag;
import pl.mdomino.artapp.model.User;
import pl.mdomino.artapp.model.dto.ImageDTO;
import pl.mdomino.artapp.repo.ImageRepo;
import pl.mdomino.artapp.repo.RatingRepo;
import pl.mdomino.artapp.repo.UserRepo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ImageService {
    private final Path fileUploadDir;
    private final ImageRepo imageRepo;
    private final RatingRepo ratingRepository;
    private final UserRepo userRepo;

    @Autowired
    public ImageService(ImageRepo imageRepo, UserRepo userRepo, RatingRepo ratingRepository, @Value("${file.upload-dir}") String fileUploadDir) {
        this.imageRepo = imageRepo;
        this.userRepo = userRepo;
        this.ratingRepository = ratingRepository;
        this.fileUploadDir = Paths.get(fileUploadDir).normalize().toAbsolutePath();
    }

    @Transactional
    public String addImage(Image image, MultipartFile file, UUID userUuid) {
        try {
            if (file.isEmpty()) {
                throw new IllegalArgumentException("File is empty");
            }

            User author = userRepo.findById(userUuid)
                    .orElseThrow(() -> new IllegalArgumentException("User not found with UUID: " + userUuid));

            image.setAuthor(author);

            String originalFileName = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));
            String extension = originalFileName.substring(originalFileName.lastIndexOf("."));

            if (!extension.equals(".jpg") && !extension.equals(".jpeg") && !extension.equals(".png") && !extension.equals(".gif")) {
                throw new IllegalArgumentException("Invalid file type. Only JPEG, PNG, and GIF are allowed.");
            }

            image.setUploadDate(LocalDateTime.now());
            image.setUpdateDate(LocalDateTime.now());
            image.setImage_ID(UUID.randomUUID());
            image.setFileName(image.getImage_ID() + extension);
            imageRepo.save(image);

            String fileName = image.getFileName();

            Path filePath = fileUploadDir.resolve(fileName);
            file.transferTo(filePath.toFile());

            return fileName;

        } catch (Exception e) {
            throw new RuntimeException("Unexpected error occurred", e);
        }
    }

    @Transactional
    public String editImage(UUID imageId, MultipartFile file, UUID userUuid, String newTitle, String newDescription) {
        try {
            Image image = imageRepo.findById(imageId)
                    .orElseThrow(() -> new IllegalArgumentException("Image not found with the given ID"));

            if (!userUuid.equals(image.getAuthor().getKeycloakID())) {
                throw new IllegalArgumentException("User is not authorized to edit this image.");
            }

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

    @Transactional(readOnly = true)
    public List<Image> searchImages(String query, String sortBy, boolean ascending, int page, int size) {
        List<String> allowedSortFields = Arrays.asList("title", "description", "uploadDate", "updateDate", "fileName", "author.username");

        if (!allowedSortFields.contains(sortBy)) {
            throw new IllegalArgumentException("Invalid sortBy parameter: " + sortBy);
        }

        Sort.Direction direction = ascending ? Sort.Direction.ASC : Sort.Direction.DESC;
        Sort sort = Sort.by(direction, sortBy);

        PageRequest pageRequest = PageRequest.of(page, size, sort);

        Page<Image> imagesPage;
        if (query != null && !query.isEmpty()) {
            imagesPage = imageRepo.findByQuery(query, pageRequest);
        } else {
            imagesPage = imageRepo.findAll(pageRequest);
        }

        return imagesPage.getContent();
    }

    @Transactional(readOnly = true)
    public List<ImageDTO> getRandomImages() {
        return imageRepo.findRandomImages()
                .stream()
                .map(image -> new ImageDTO(image.getImage_ID(), image.getTitle(), image.getDescription()))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Path getImageByFilePath(String fileName) {
        Path filePath = fileUploadDir.resolve(fileName);

        if (!Files.exists(filePath)) {
            throw new IllegalArgumentException("File not found");
        }

        return fileUploadDir.resolve(fileName);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getImageDetails(UUID imageId) {
        Image image = imageRepo.findById(imageId)
                .orElseThrow(() -> new IllegalArgumentException("Image not found with the given ID"));

        Double averageRating = ratingRepository.findAverageRatingByImageId(imageId);

        long favoriteCount = image.getFavorites().size();

        Map<String, Object> details = new HashMap<>();
        details.put("image", image);
        details.put("averageRating", averageRating);
        details.put("favoriteCount", favoriteCount);

        return details;
    }

    @Transactional(readOnly = true)
    public List<ImageDTO> getSuggestions(UUID imageId) {
        Image image = imageRepo.findById(imageId)
                .orElseThrow(() -> new IllegalArgumentException("Image not found with the given ID"));

        Set<Tag> imageTags = image.getTags();

        if (imageTags.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> tagNames = imageTags.stream()
                .map(Tag::getTagName)
                .collect(Collectors.toList());

        List<Image> similarImages = imageRepo.findSimilarImagesByTags(tagNames, imageId);

        Collections.shuffle(similarImages);
        return similarImages.stream()
                .limit(5)
                .map(img -> new ImageDTO(img.getImage_ID(), img.getTitle(), img.getDescription()))
                .collect(Collectors.toList());
    }

    @Transactional
    public Image deleteImage(UUID imageId, UUID userUuid) {
        try {
            Image image = imageRepo.findById(imageId)
                    .orElseThrow(() -> new IllegalArgumentException("Image not found with the given ID"));

            if (!userUuid.equals(image.getAuthor().getKeycloakID())) {
                throw new IllegalArgumentException("User is not authorized to delete this image.");
            }

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

}
