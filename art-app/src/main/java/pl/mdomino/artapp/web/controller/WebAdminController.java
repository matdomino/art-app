package pl.mdomino.artapp.web.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import pl.mdomino.artapp.service.AdminService;

@Controller
public class WebAdminController {
    private final AdminService adminService;

    public WebAdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    private void addAuthAttributes(Model model, Authentication auth) {
        String username = "";
        String userId = "";
        boolean isAdmin = false;

        if (auth instanceof OAuth2AuthenticationToken oauth && oauth.getPrincipal() instanceof OidcUser oidc) {
            username = oidc.getPreferredUsername();
            userId = oidc.getSubject();
            isAdmin = auth.getAuthorities().stream()
                    .anyMatch(authority -> "Admin".equals(authority.getAuthority()));
        }

        model.addAttribute("name", username);
        model.addAttribute("userId", userId);
        model.addAttribute("isAuthenticated", auth != null && auth.isAuthenticated());
        model.addAttribute("isAdmin", isAdmin);
    }

    @GetMapping("/admin")
    public String adminPage(Model model, Authentication auth) {
        addAuthAttributes(model, auth);

        return "admin.html";
    }

    @GetMapping("/admin/export/csv")
    public ResponseEntity<byte[]> exportDataToCSV() {
        byte[] csvData = adminService.exportDataToCSV();
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"exported_data.csv\"")
                .header("Content-Type", "text/csv")
                .body(csvData);
    }
}
