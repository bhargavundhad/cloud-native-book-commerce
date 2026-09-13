package com.bookecommerce.user_service.controller;

import com.bookecommerce.user_service.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/internal")
@RequiredArgsConstructor
public class InternalUserController {

    private final UserService userService;

    @GetMapping("/users/{id}/exists")
    public ResponseEntity<Map<String, Boolean>> userExists(@PathVariable UUID id) {
        boolean exists = userService.userExists(id);
        return ResponseEntity.ok(Map.of("exists", exists));
    }
}
