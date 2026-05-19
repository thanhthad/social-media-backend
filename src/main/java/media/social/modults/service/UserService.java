package media.social.modults.service;


import media.social.modults.dto.request.UserRequest;
import media.social.modults.dto.response.UserResponse;
import media.social.modults.entity.Post;
import media.social.modults.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface UserService {

    UserResponse create(UserRequest request);

    UserResponse updateById(Long id, UserRequest request);

    void deleteById(Long id);

    UserResponse getById(Long id);

    Page<UserResponse> getAll(Pageable pageable);


    List<Post> getPostsByUserId(Long userId);

}