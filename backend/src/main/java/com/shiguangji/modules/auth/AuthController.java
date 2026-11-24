package com.shiguangji.modules.auth;

import com.shiguangji.common.api.Result;
import com.shiguangji.common.dto.Dtos.LoginResult;
import com.shiguangji.common.dto.Dtos.UserDto;
import com.shiguangji.common.security.CurrentUser;
import com.shiguangji.modules.user.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserService userService;

    public record LoginRequest(@NotBlank String username, @NotBlank String password) {
    }

    public record RegisterRequest(@NotBlank String username, @NotBlank String password,
                                  String nickname, String contact) {
    }

    @PostMapping("/login")
    public Result<LoginResult> login(@Valid @RequestBody LoginRequest request) {
        return Result.ok(authService.login(request.username().trim(), request.password()));
    }

    @PostMapping("/register")
    public Result<LoginResult> register(@Valid @RequestBody RegisterRequest request) {
        return Result.ok(authService.register(request.username(), request.password(),
                request.nickname(), request.contact()));
    }

    @GetMapping("/me")
    public Result<UserDto> me() {
        return Result.ok(userService.toDto(userService.current()));
    }

    @PostMapping("/logout")
    public Result<Void> logout() {
        return Result.ok();
    }
}
