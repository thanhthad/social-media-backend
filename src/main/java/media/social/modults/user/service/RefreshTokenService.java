package media.social.modults.user.service;

import media.social.modults.user.entity.RefreshToken;

public interface RefreshTokenService {

    RefreshToken create(Long userId);

    RefreshToken verify(String token);

    RefreshToken findValidByUser(Long userId );

    void revoke(String refreshToken);

    String generateAccessToken(String refreshToken);
}