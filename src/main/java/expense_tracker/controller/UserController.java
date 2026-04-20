package expense_tracker.controller;

import expense_tracker.model.dto.user.CreateUserRequestDto;
import expense_tracker.model.dto.LoginRequest;
import expense_tracker.model.dto.LoginResponse;
import expense_tracker.model.dto.user.UserResponseDto;
import expense_tracker.services.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;


    @PostMapping("/signup")
    public UserResponseDto signUp(
            @Valid @RequestBody CreateUserRequestDto request) {
        return userService.signUp(request);
    }

    @PostMapping("/login")
    public LoginResponse login(
            @Valid @RequestBody LoginRequest request
    ) {
        return userService.login(request);
    }

    @DeleteMapping("/users/me")
    public ResponseEntity<String> deleteUser(
            @AuthenticationPrincipal UserDetails user) {
        userService.deleteUser(user.getUsername());

        return ResponseEntity.ok("User deleted successfully");
    }

}
