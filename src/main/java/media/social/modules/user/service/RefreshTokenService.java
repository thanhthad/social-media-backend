package media.social.modules.user.service;

import media.social.modules.auth.entity.RefreshToken;

public interface RefreshTokenService {

    RefreshToken create(Long userId);

    RefreshToken verify(String token);

    RefreshToken findValidByUser(Long userId );

    void revoke(String refreshToken);

    String generateAccessToken(String refreshToken);
}