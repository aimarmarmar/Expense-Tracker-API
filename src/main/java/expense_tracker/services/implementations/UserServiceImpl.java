package expense_tracker.services.implementations;

import expense_tracker.exception.BadRequestException;
import expense_tracker.exception.NotFoundException;
import expense_tracker.model.dto.user.CreateUserRequestDto;
import expense_tracker.model.dto.LoginRequest;
import expense_tracker.model.dto.LoginResponse;
import expense_tracker.model.dto.user.UserResponseDto;
import expense_tracker.model.entity.User;
import expense_tracker.repository.UserRepository;
import expense_tracker.services.UserService;
import expense_tracker.util.JwtUtil;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;


@Slf4j
@Service
@AllArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder encoder;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public UserResponseDto signUp(CreateUserRequestDto request) {

        log.info(">>> MASUK SERVICE SIGNUP");

        if (request.getUsername() == null || request.getUsername().isEmpty()) {
            throw new BadRequestException("Username must not be empty");
        }

        if (request.getPassword() == null || request.getPassword().isEmpty()) {

            throw new BadRequestException("Password must not be empty");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already exists");
        }

        if (request.getEmail() == null || request.getEmail().isEmpty()) {
            throw new IllegalArgumentException("Email must not be empty");
        }

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username already exists");
        }

        log.info("Checking duplicate user...");

        String HashedPassword = encoder.encode(request.getPassword());

        log.info("Password hashed successfully");

        User user = new User();
        LocalDateTime now = LocalDateTime.now();

        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(HashedPassword);
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        user.setDeleted(false);
        user.setDeletedAt(null);

        User savedUser = userRepository.save(user);

        log.info("User saved with id={}", savedUser.getId());

        return UserResponseDto.builder()
                .id(savedUser.getId())
                .username(savedUser.getUsername())
                .email(savedUser.getEmail())
                .createdAt(savedUser.getCreatedAt())
                .build();

    }

    public LoginResponse login(LoginRequest request) {
        log.info(">>> MASUK SERVICE LOGIN");

        User user = userRepository.findByUsernameAndDeletedFalse(request.getUsername())
                .orElseThrow(() -> new NotFoundException("User not found"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Invalid username or password");
        }

        log.info("Generating JWT token...");

        String token = jwtUtil.generateToken(user.getUsername());
        return LoginResponse.builder()
                .token(token)
                .build();
    }

    public void deleteUser(String username) {
        User user = userRepository.findByUsernameAndDeletedFalse(username)
                .orElseThrow(() -> new NotFoundException("User not found"));

        user.setDeleted(true);
        user.setUpdatedAt(LocalDateTime.now());
        user.setDeletedAt(LocalDateTime.now());

        userRepository.save(user);
    }

    public User getActiveUserByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("User not found"));

        if (Boolean.TRUE.equals(user.getDeleted())) {
            throw new NotFoundException("User is not active");
        }

        return user;
    }

}

