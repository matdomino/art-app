package pl.mdomino.artapp.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import pl.mdomino.artapp.model.Image;
import pl.mdomino.artapp.model.dto.ImageDTO;
import pl.mdomino.artapp.repo.ImageRepo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ImageService {
    private final Path fileUploadDir;

    @Autowired
    private final ImageRepo imageRepo;

    public ImageService(ImageRepo imageRepo, @Value("${file.upload-dir}") String fileUploadDir) {
        this.imageRepo = imageRepo;
        this.fileUploadDir = Paths.get(fileUploadDir).normalize().toAbsolutePath();
    }

    public String addImage(Image image, MultipartFile file) {
        try {
            if (file.isEmpty()) {
                throw new IllegalArgumentException("File is empty");
            }

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

        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Unexpected error occurred", e);
        }
    }

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

        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Unexpected error occurred", e);
        }
    }

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

    public List<ImageDTO> getRandomImages() {
        return imageRepo.findRandomImages()
                .stream()
                .map(image -> new ImageDTO(image.getImage_ID(), image.getTitle(), image.getDescription()))
                .collect(Collectors.toList());
    }

    public Path getImageByFilePath(String fileName) {
        Path filePath = fileUploadDir.resolve(fileName);

        if (!Files.exists(filePath)) {
            throw new IllegalArgumentException("File not found");
        }

        return fileUploadDir.resolve(fileName);
    }
}
