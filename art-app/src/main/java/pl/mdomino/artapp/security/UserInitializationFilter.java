package pl.mdomino.artapp.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import pl.mdomino.artapp.model.User;
import pl.mdomino.artapp.repo.UserRepo;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.UUID;

@Component
public class UserInitializationFilter extends OncePerRequestFilter {
    private final UserRepo userRepo;

    public UserInitializationFilter(UserRepo userRepo) {
        this.userRepo = userRepo;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.getPrincipal() instanceof OidcUser oidcUser) {
            UUID keycloakID = UUID.fromString(oidcUser.getSubject());
            String username = oidcUser.getPreferredUsername();

            if (!userRepo.existsById(keycloakID)) {
                User newUser = new User();
                newUser.setKeycloakID(keycloakID);
                newUser.setUsername(username);
                newUser.setCreatedAt(LocalDateTime.now());

                userRepo.save(newUser);            }
        }

        filterChain.doFilter(request, response);
    }
}
