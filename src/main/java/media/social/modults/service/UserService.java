package media.social.modults.service;


import media.social.modults.dto.request.LoginRequest;
import media.social.modults.dto.request.RegisterRequest;
import media.social.modults.dto.request.UserRequest;
import media.social.modults.dto.response.LoginResponse;
import media.social.modults.dto.response.UserResponse;
import media.social.modults.entity.Post;
import media.social.modults.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface UserService {
    Page<UserResponse> getAll(Pageable pageable);

    LoginResponse login(LoginRequest loginRequest);

    UserResponse register(RegisterRequest registerRequest);

    UserResponse getById(Long id);

    UserResponse getByEmail(String email);

    UserResponse create(UserRequest request);

    UserResponse updateById(Long id, UserRequest request);

    void deleteById(Long id);

    void deleteByEmail(String email);

    Page<UserResponse> getByUsername(String username,Pageable pageable);

    UserResponse uploadAvatar(Long userId, MultipartFile file);

}