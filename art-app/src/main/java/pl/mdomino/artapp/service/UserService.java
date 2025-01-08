package pl.mdomino.artapp.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.mdomino.artapp.model.User;
import pl.mdomino.artapp.repo.UserRepo;

import java.util.*;

@Service
public class UserService {
    private final UserRepo userRepo;

    @Autowired
    public UserService(UserRepo userRepo) {
        this.userRepo = userRepo;
    }

    @Transactional
    public User changeProfile(User user, UUID userUuid) {
        User existingUser = userRepo.findById(userUuid)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (!userUuid.equals(existingUser.getKeycloakID())) {
            throw new IllegalArgumentException("User is not authorized to edit this profile.");
        }

        if (user.getUsername() != null) {
            existingUser.setUsername(user.getUsername());
        }
        if (user.getProfileSummary() != null) {
            existingUser.setProfileSummary(user.getProfileSummary());
        }

        return userRepo.save(existingUser);
    }

    @Transactional(readOnly = true)
    public User getUserProfile(UUID userUuid) {
        return userRepo.findById(userUuid)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }
}