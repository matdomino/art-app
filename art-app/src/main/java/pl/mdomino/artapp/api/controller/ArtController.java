package pl.mdomino.artapp.api.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/art")
public class ArtController {
    @GetMapping("/hello-1")
    public String hello() {
        return "Hello World - not logged in!";
    }

    @GetMapping("/hello-2")
    public String hello2() {
        return "Hello World - logged in!";
    }

    @GetMapping("/hello-3")
    @PreAuthorize("hasRole('client_admin')")
    public String hello3() {
        return "Hello World - logged in admin!";
    }
}
