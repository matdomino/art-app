package pl.mdomino.artapp.api.controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import pl.mdomino.artapp.model.User;
import pl.mdomino.artapp.service.UserService;

import java.util.UUID;

@RestController
@RequestMapping("api/users")
public class UserController {
    private final UserService userService;

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PutMapping("/edit")
    public ResponseEntity<ApiResponse> editUserProfile(@RequestBody @Valid User user) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            return ResponseEntity.status(401).body(new ApiResponse("Unauthorized: Unable to get user information."));
        }

        UUID userUuid = UUID.fromString(jwt.getClaimAsString("sub"));

        User updatedUser = userService.changeProfile(user, userUuid);

        return ResponseEntity.ok(new ApiResponse("Updated profile: " + updatedUser.getUsername()));
    }

    @GetMapping("/{userUuid}")
    public ResponseEntity<User> getUserProfile(@PathVariable UUID userUuid) {
        User userProfile = userService.getUserProfile(userUuid);

        return ResponseEntity.ok(userProfile);
    }
}
