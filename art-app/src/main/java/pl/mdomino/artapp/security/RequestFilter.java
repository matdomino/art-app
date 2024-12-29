package pl.mdomino.artapp.security;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import pl.mdomino.artapp.model.User;
import pl.mdomino.artapp.repo.UserRepo;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.UUID;

@Component
public class RequestFilter extends OncePerRequestFilter {
    private final UserRepo userRepo;

    public RequestFilter(UserRepo userRepo) {
        this.userRepo = userRepo;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        var authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
            UUID keycloakID = UUID.fromString(jwt.getClaimAsString("sub"));
            String username = jwt.getClaimAsString("preferred_username");

            if (!userRepo.existsById(keycloakID)) {
                User newUser = new User();
                newUser.setKeycloakID(keycloakID);
                newUser.setUsername(username);
                newUser.setCreatedAt(LocalDateTime.now());

                userRepo.save(newUser);
            }
        }

        filterChain.doFilter(request, response);
    }
}
