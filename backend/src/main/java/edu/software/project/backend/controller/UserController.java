package edu.software.project.backend.controller;

import edu.software.project.backend.dto.UserProfileResponse;
import edu.software.project.backend.security.AuthenticatedUser;
import edu.software.project.backend.security.CurrentUser;
import edu.software.project.backend.service.ResponseMapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final ResponseMapper responseMapper;

    public UserController(ResponseMapper responseMapper) {
        this.responseMapper = responseMapper;
    }

    @GetMapping("/me")
    public UserProfileResponse getCurrentUser(@CurrentUser AuthenticatedUser authenticatedUser) {
        return responseMapper.toUserProfile(authenticatedUser);
    }
}
