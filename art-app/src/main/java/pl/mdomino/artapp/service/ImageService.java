package pl.mdomino.artapp.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import pl.mdomino.artapp.model.Image;
import pl.mdomino.artapp.repo.ImageRepo;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

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
            image.setFileExtension(extension);
            imageRepo.save(image);

            String fileName = image.getImage_ID().toString() + extension;

            Path filePath = fileUploadDir.resolve(fileName);
            file.transferTo(filePath.toFile());

            return fileName;

        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Unexpected error occurred", e);
        }
    }

    public Path getImageFileById(UUID imageId) {
        Image image = imageRepo.findById(imageId)
                .orElseThrow(() -> new IllegalArgumentException("Image not found with ID: " + imageId));

        String extension = image.getFileExtension();

        String fileName = imageId + extension;

        return fileUploadDir.resolve(fileName);
    }
}
