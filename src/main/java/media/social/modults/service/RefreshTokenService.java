package media.social.modults.service;

import media.social.modults.entity.RefreshToken;
import media.social.modults.entity.User;

public interface RefreshTokenService {

    RefreshToken create(Long userId);

    RefreshToken verify(String token);

    RefreshToken findValidByUser(Long userId );

    void revoke(String refreshToken);

    String generateAccessToken(String refreshToken);
}