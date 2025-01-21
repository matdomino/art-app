package pl.mdomino.artapp.web.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import pl.mdomino.artapp.model.User;
import pl.mdomino.artapp.service.UserService;

import java.util.UUID;

@Controller
public class WebUserController {
    private final UserService userService;

    @Autowired
    public WebUserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/user/{id}")
    public String user(@PathVariable("id") UUID id, Model model, Authentication auth) {
        model.addAttribute("name",
                auth instanceof OAuth2AuthenticationToken oauth && oauth.getPrincipal() instanceof OidcUser oidc
                        ? oidc.getPreferredUsername()
                        : "");
        model.addAttribute("isAuthenticated", auth != null && auth.isAuthenticated());

        User userProfile = userService.getUserProfile(id);

        model.addAttribute("user", userProfile);

        return "user.html";
    }
}
