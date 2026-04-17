package expense_tracker.services;

import expense_tracker.model.dto.user.CreateUserRequestDto;
import expense_tracker.model.dto.LoginRequest;
import expense_tracker.model.dto.LoginResponse;
import expense_tracker.model.dto.user.UserResponseDto;
import expense_tracker.model.entity.User;

public interface UserService {

    UserResponseDto signUp(CreateUserRequestDto request);
    LoginResponse login(LoginRequest request);
    void deleteUser(String username);
    User getActiveUserByUsername(String username);
}
