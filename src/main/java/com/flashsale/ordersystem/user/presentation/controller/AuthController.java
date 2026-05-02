package com.flashsale.ordersystem.user.presentation.controller;

import com.flashsale.ordersystem.user.presentation.dto.RegisterRequest;
import com.flashsale.ordersystem.user.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public void register(@RequestBody RegisterRequest request) {
        log.info("User registration request. email={}", request.email());
        authService.register(request);
    }
}