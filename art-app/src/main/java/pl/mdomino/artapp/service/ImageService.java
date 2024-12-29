package pl.mdomino.artapp.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import pl.mdomino.artapp.model.Image;
import pl.mdomino.artapp.repo.ImageRepo;

@Service
public class ImageService {
    @Autowired
    private final ImageRepo imageRepo;

    public ImageService(ImageRepo imageRepo) {
        this.imageRepo = imageRepo;
    }

    public void addImage(Image image, MultipartFile file) {
        try {
            imageRepo.save(image);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }


}
