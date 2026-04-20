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
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public UserResponseDto signUp(CreateUserRequestDto request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BadRequestException("Username already exists");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email already exists");
        }

        String hashedPassword = passwordEncoder.encode(request.getPassword());

        User user = new User();
        LocalDateTime now = LocalDateTime.now();

        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(hashedPassword);
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        user.setDeleted(false);
        user.setDeletedAt(null);

        User savedUser = userRepository.save(user);

        return UserResponseDto.builder()
                .id(savedUser.getId())
                .username(savedUser.getUsername())
                .email(savedUser.getEmail())
                .createdAt(savedUser.getCreatedAt())
                .build();

    }

    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByUsernameAndDeletedFalse(request.getUsername())
                .orElseThrow(() -> new NotFoundException("User not found"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BadRequestException("Invalid username or password");
        }

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
        User user = userRepository.findByUsernameAndDeletedFalse(username)
                .orElseThrow(() -> new NotFoundException("User not found"));

        return user;
    }

}

