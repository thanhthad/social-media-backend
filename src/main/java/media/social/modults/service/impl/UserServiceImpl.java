package media.social.modults.service.impl;

import lombok.AllArgsConstructor;
import media.social.modults.dto.request.UserRequest;
import media.social.modults.dto.response.UserResponse;
import media.social.modults.entity.Post;
import media.social.modults.entity.User;
import media.social.modults.exception.user.UserAlreadyExistsException;
import media.social.modults.repository.PostRepository;
import media.social.modults.repository.UserRepository;
import media.social.modults.service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@AllArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PostRepository postRepository;

    @Override
    public UserResponse create(UserRequest request) {
        String email = request.getEmail();
        if(!userRepository.existsByEmail(email)){
            throw new UserAlreadyExistsException("User with email: "+ email + " is already exists");
        }
        return null;
    }

    @Override
    public UserResponse updateById(Long id, UserRequest request) {
        return null;
    }

    @Override
    public void deleteById(Long id) {

    }

    @Override
    public UserResponse getById(Long id) {
        return null;
    }

    @Override
    public Page<UserResponse> getAll(Pageable pageable) {
        return null;
    }

    // JOIN: lấy posts theo userId
    @Override
    public List<Post> getPostsByUserId(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return postRepository.findByUser(user);
    }

}